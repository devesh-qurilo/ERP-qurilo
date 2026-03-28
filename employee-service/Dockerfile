FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy only POMs first (monorepo root + module POMs)
COPY pom.xml .
COPY auth-service/pom.xml auth-service/pom.xml
COPY employee-service/pom.xml employee-service/pom.xml

# Pre-fetch deps for both
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests -pl auth-service,employee-service -am dependency:go-offline

# Copy sources
COPY auth-service/ auth-service/
COPY employee-service/ employee-service/

# Install auth-service to local repo, then build employee-service
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests -pl auth-service -am install
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests -pl employee-service -am package

# ===== Runtime =====
FROM eclipse-temurin:21-jre
ENV TZ=Etc/UTC \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
WORKDIR /opt/app
RUN addgroup --system app && adduser --system --ingroup app app
USER app:app
COPY --from=build /app/employee-service/target/*.jar app.jar
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=3s --retries=5 CMD wget -qO- http://EMPLOYEE-SERVICE:8083/actuator/health || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
