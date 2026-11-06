# syntax=docker/dockerfile:1
# The line above must stay first: it tells BuildKit which Dockerfile syntax to use.
# Version 1 enables COPY --chmod, ADD --checksum and RUN --mount, used below.

# ============================================================
# CIVICGRANT CONTAINER IMAGE
# ============================================================
#
# Two stages:
#   1. build:   a JDK and Maven compile the application into a JAR.
#   2. runtime: only a JRE and the compiled application. This is the final image.
#
# Only the last stage ends up in the image. The JDK, Maven, the source code
# and the Maven cache from stage 1 are thrown away.
#
# Build and run locally (see compose.yaml for the database):
#   docker build --tag civicgrant .
#   docker run --rm -p 8080:8080 -e SPRING_DATASOURCE_URL=... civicgrant
#
# The image contains no configuration that differs per environment and no
# secrets. The database address and the rest come from environment variables
# when the container starts: one image runs everywhere.


# ------------------------------------------------------------
# STAGE 1: BUILD
# ------------------------------------------------------------

# Eclipse Temurin JDK 25 on Ubuntu 24.04 (noble): compiler and tools included.
# "AS build" names the stage, so stage 2 can copy files out of it.
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

# 1. Copy what Maven needs to build: the wrapper, the root pom and the modules.
#    --chmod=755 makes mvnw executable, whatever its permissions were on Windows.
COPY --chmod=755 mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY domain domain
COPY api api
COPY persistence persistence
COPY starter starter

# 2. Build the application JAR and split it into layers.
#
#    --mount=type=cache keeps the Maven repository (/root/.m2) between local builds,
#    so dependencies are downloaded once. It's a build cache, not part of the image.
#    On GitHub runners it starts empty on every run.
#
#    -pl starter -am     build the starter module and the modules it depends on
#                        ("also make"): domain, api, persistence.
#    -DskipTests         the tests already ran in the Verify job.
#
#    jarmode=tools extract --layers splits the Spring Boot JAR into folders, from
#    "rarely changes" to "changes every commit": dependencies, spring-boot-loader,
#    snapshot-dependencies, application. Stage 2 copies them as separate layers.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests -pl starter -am \
    && cp starter/target/civicgrant-starter-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted


# ------------------------------------------------------------
# STAGE 2: RUNTIME (the final image)
# ------------------------------------------------------------

# The Java runtime only, no compiler: a smaller image with fewer tools an
# attacker could use. Same Java version and Ubuntu release as stage 1.
FROM eclipse-temurin:25-jre-noble
WORKDIR /application

# 1. Create an unprivileged user. The application runs as this user (step 4),
#    so a compromised application isn't root inside the container.
#    --system: a service account, no password, no login.
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --no-create-home spring

# 2. Download the Application Insights Java agent, which sends logs, traces and
#    metrics to Azure. --checksum makes the build fail if the file isn't exactly
#    the expected one. --chmod=444 makes it read-only.
#    It's only active when the container sets
#    JAVA_TOOL_OPTIONS=-javaagent:/application/applicationinsights-agent.jar.
ADD --chmod=444 \
    --checksum=sha256:93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec \
    https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.10/applicationinsights-agent-3.7.10.jar \
    applicationinsights-agent.jar

# 3. Copy the application from stage 1, one image layer per folder.
#    The order goes from "rarely changes" to "changes every commit". A registry
#    stores identical layers once, and a rebuild reuses every layer before the
#    first one that changed, so a code change only adds the last layer.
#      dependencies           third-party libraries: Spring, the PostgreSQL driver…
#      spring-boot-loader     the launcher that starts a Spring Boot JAR
#      snapshot-dependencies  SNAPSHOT libraries, including this project's own modules
#      application            the starter's own classes and resources, and application.jar
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# 4. Run everything after this line as the unprivileged user from step 1.
USER spring

# 5. Documents the port the application listens on. It doesn't open anything:
#    docker run -p 8080:8080, or the Container App's ingress, does.
EXPOSE 8080

# 6. The command the container runs when it starts.
#    The JSON form makes java the main process, so it receives the stop signal
#    directly and Spring Boot can shut down gracefully.
#    MaxRAMPercentage=75: the JVM may use 75% of the container's memory limit,
#    whatever size the container is given. The rest is left for the agent and the OS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
