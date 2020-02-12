FROM openjdk:11-jdk
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw package -Dmaven.test.skip=true

ENV APP_UID=9999
ENV APP_GID=9999
RUN groupadd -r -g $APP_GID appUser
RUN useradd -r -u $APP_UID -g $APP_GID appUser 
RUN mkdir -p /usr/bin/app/
RUN chown -R appUser /usr/bin/app/
USER appUser

FROM openjdk:11-jre-slim
COPY --from=0 /usr/src/app/target/program-service-*-SNAPSHOT.jar /usr/bin/app/program-service.jar
CMD ["java", "-ea", "-jar", "/usr/bin/app/program-service.jar"]
EXPOSE 50051/tcp
