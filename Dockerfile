# syntax=docker/dockerfile:1

# ---------- Build: Maven + JDK 25 ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Dependências numa camada própria: só baixa de novo quando o pom.xml muda.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam fora do build (o contextLoads precisa de banco): ./mvnw test antes de publicar.
RUN mvn -B -q package -DskipTests

# ---------- Runtime: só a JRE ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/task-api-*.jar /app/app.jar
USER app

# Porta e perfil de produção; limites de memória da JVM vêm do compose (x-java-app).
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
