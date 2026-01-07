###
# Image pour la compilation
FROM maven:3-eclipse-temurin-21 as build-image
WORKDIR /build/
# On lance la compilation Java
# On débute par une mise en cache docker des dépendances Java
# cf https://www.baeldung.com/ops/docker-cache-maven-dependencies
COPY ./pom.xml /build/kafka2sudoc/pom.xml
RUN mvn -f /build/kafka2sudoc/pom.xml verify --fail-never
# et la compilation du code Java
COPY ./   /build/

RUN mvn --batch-mode \
        -Dmaven.test.skip=false \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        package spring-boot:repackage


FROM ossyupiik/java:21.0.8 as kafka2sudoc-image
WORKDIR /app/
COPY --from=build-image /build/target/*.jar /app/kafka2sudoc.jar
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
CMD ["java","-jar","/app/kafka2sudoc.jar"]
