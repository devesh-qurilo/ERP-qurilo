FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy only pom first to leverage layer caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline

# Now copy sources and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests clean package

# ====== Runtime stage ======
FROM eclipse-temurin:21-jre
ENV TZ=Etc/UTC \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=25 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
WORKDIR /opt/app

# create non-root user
RUN addgroup --system app && adduser --system --ingroup app app
USER app:app

# copy fat jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]