FROM openjdk:11-jdk
WORKDIR /usr/src/app
ADD . .
RUN ./mvnw package


# ENV APP_UID=9999
# ENV APP_GID=9999
# RUN groupadd -r -g $APP_GID appUser
# RUN useradd -r -u $APP_UID -g $APP_GID appUser 
# # RUN mkdir -p /usr/bin/program-service.jar
# RUN chown -R appUser /usr/bin/program-service.jar
# USER appUser

FROM openjdk:11-jre-slim
COPY --from=0 /usr/src/app/target/program-service-*-SNAPSHOT.jar /usr/bin/program-service.jar
CMD ["java", "-ea", "-jar", "/usr/bin/program-service.jar"]
EXPOSE 50051/tcp
