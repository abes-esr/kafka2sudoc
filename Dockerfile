###
# Image pour la compilation
FROM maven:3-eclipse-temurin-21 AS build-image
WORKDIR /build/
# On lance la compilation Java
# On débute par une mise en cache docker des dépendances Java
# cf https://www.baeldung.com/ops/docker-cache-maven-dependencies
COPY ./pom.xml /build/kafka2sudoc/pom.xml
RUN mvn -f /build/kafka2sudoc/pom.xml verify --fail-never
# et la compilation du code Java
COPY ./   /build/

RUN mvn --batch-mode \
        -Dmaven.test.skip=true \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        package -Passembly


FROM ossyupiik/java-jdk:21.0.8 AS kafka2sudoc-image
WORKDIR /
COPY --from=build-image /build/target/kafka2sudoc-distribution.tar.gz /
RUN tar xvfz kafka2sudoc-distribution.tar.gz
RUN rm -f /kafka2sudoc-distribution.tar.gz

ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD ["java", "-cp", "/kafka2sudoc/lib/*", "fr.abes.kafkatosudoc.KafkaToSudocApplication"]
