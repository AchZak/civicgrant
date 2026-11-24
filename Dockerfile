# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

COPY --chmod=755 mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY core core
COPY api api
COPY persistence persistence
COPY blobstorage blobstorage
COPY app app

# The tests already ran in CI's Verify job.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests -pl app -am \
    && cp app/target/civicgrant-app-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM eclipse-temurin:25-jre-noble
WORKDIR /application

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --no-create-home spring

# Loaded only when JAVA_TOOL_OPTIONS names it, which infra/environment.bicep does.
ADD --chmod=444 \
    --checksum=sha256:93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec \
    https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.10/applicationinsights-agent-3.7.10.jar \
    applicationinsights-agent.jar

# From rarely to often changed, so a code change only replaces the last layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring
EXPOSE 8080

# A quarter of the container's memory stays free for the agent and the OS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
