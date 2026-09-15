# Image de production du portail : front Angular servi par Spring Boot.

FROM node:24.21.0-alpine AS front
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

FROM eclipse-temurin:21-jdk-alpine AS back
WORKDIR /build/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN sh mvnw -B -q dependency:go-offline
COPY backend/src src
# Le build Angular remplace la coquille temporaire de src/main/resources/static.
RUN rm -rf src/main/resources/static
COPY --from=front /build/frontend/dist/frontend/browser src/main/resources/static
RUN sh mvnw -B -q -DskipTests package && cp target/portal-*.jar /build/portal.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S portal && adduser -S -G portal portal
WORKDIR /app
COPY --from=back /build/portal.jar portal.jar
USER portal
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/portal.jar"]
