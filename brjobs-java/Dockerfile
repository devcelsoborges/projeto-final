FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system brjobs && useradd --system --gid brjobs --home-dir /app brjobs

COPY --from=build /app/target/brjobs-*.jar /app/brjobs-api.jar

USER brjobs

EXPOSE 8080

# --enable-native-access: o encoder WebP (webp-imageio) carrega lib nativa via System.load
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/brjobs-api.jar"]
