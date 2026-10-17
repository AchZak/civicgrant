# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

COPY --chmod=755 mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY domain domain
COPY api api
COPY persistence persistence
COPY starter starter

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests -pl starter -am \
    && cp starter/target/civicgrant-starter-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted


FROM eclipse-temurin:25-jre-noble
WORKDIR /application

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --no-create-home spring

ADD --chmod=444 \
    --checksum=sha256:93a70c8f5d364c7e777f6c4d1b235dba91aef8448bd3fa94359f1d7f3e2eb0ec \
    https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.10/applicationinsights-agent-3.7.10.jar \
    applicationinsights-agent.jar

COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
