# Um container só: build do front (Vite) -> copiado para dentro da API (Spring Boot) -> imagem final.
# Contexto de build = raiz do repositório.

# 1) Front
FROM node:22-alpine AS front
WORKDIR /front
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# sem VITE_API_URL: o front chama /api no mesmo domínio
RUN npm run build

# 2) API (com o front embutido em /static)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=front /front/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# 3) Execução
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
