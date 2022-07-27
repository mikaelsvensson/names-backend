#
# BUILD
#

FROM maven:3.8-openjdk-16 AS builder

WORKDIR /app

COPY pom.xml .
COPY names-backend-core/src ./names-backend-core/src
COPY names-backend-core/pom.xml ./names-backend-core/pom.xml
COPY names-backend-importer/src ./names-backend-importer/src
COPY names-backend-importer/pom.xml ./names-backend-importer/pom.xml
COPY names-backend-repository-firestore/src ./names-backend-repository-firestore/src
COPY names-backend-repository-firestore/pom.xml ./names-backend-repository-firestore/pom.xml
COPY names-backend-repository-rdms/src ./names-backend-repository-rdms/src
COPY names-backend-repository-rdms/pom.xml ./names-backend-repository-rdms/pom.xml
COPY names-backend-service/src ./names-backend-service/src
COPY names-backend-service/pom.xml ./names-backend-service/pom.xml

RUN mvn package -Pserverless-with-firebase -DskipTests


#
# RUN
#

# Note: 17-jre-alpine is not enough for the Firestore SDK to start
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/names-backend-service/target/names-backend-service-*.jar service.jar
COPY names-backend-service/application*.yaml .

CMD ["java",                                           \
    "-Djava.security.egd=file:/dev/./urandom",         \
    "-jar",                                            \
    "service.jar",                                     \
    "--spring.liquibase.enabled=false" ]
