# Build: Gradle-Wrapper mit Java 21 (Tests laufen in CI; hier nur das ausführbare JAR).
FROM eclipse-temurin:21.0.12_8-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY config config
COPY engine engine
RUN chmod +x gradlew && ./gradlew --no-daemon verifyEngine
COPY src src
RUN ./gradlew --no-daemon bootJar

# Laufzeit: JDK-Image, da Calvin das Modul jdk.incubator.vector benötigt.
FROM eclipse-temurin:21.0.12_8-jdk
RUN useradd --system --uid 10001 chesscoach
WORKDIR /app
COPY --from=build /src/build/libs/chess-coach-backend.jar /app/chess-coach-backend.jar
COPY --from=build /src/engine/calvin-chess-engine-6.1.1.jar /app/engine/calvin-chess-engine-6.1.1.jar
COPY --from=build /src/engine/LICENSE-calvin.txt /app/engine/LICENSE-calvin.txt
USER chesscoach
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40" \
    SPRING_PROFILES_ACTIVE=prod \
    CHESSCOACH_ENGINE_CALVIN_JAR_PATH=/app/engine/calvin-chess-engine-6.1.1.jar
ENTRYPOINT ["java", "-jar", "/app/chess-coach-backend.jar"]
