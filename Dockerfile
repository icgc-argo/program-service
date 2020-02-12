FROM openjdk:11-jdk
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw package -Dmaven.test.skip=true

FROM openjdk:11-jre-slim
COPY --from=0 /usr/src/app/target/program-service-*-SNAPSHOT.jar /usr/bin/program-service.jar
CMD ["java", "-ea", "-jar", "/usr/bin/program-service.jar"]
EXPOSE 50051/tcp
