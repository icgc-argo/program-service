###################################
#   Maven Builder
###################################
FROM adoptopenjdk/openjdk11:jdk-11.0.6_10-alpine-slim as builder

WORKDIR /srv

ADD . .

RUN ./mvnw package -Dmaven.test.skip=true

###################################
#   Server
###################################
FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine

ENV APP_USER app
ENV APP_UID 9999
ENV APP_GID 9999
ENV APP_HOME /home/$APP_USER

COPY --from=builder /srv/target/program-service-*.jar /program-service.jar

RUN addgroup -S -g $APP_GID $APP_USER  \
   && adduser -S -u $APP_UID -G $APP_USER $APP_USER \
   && mkdir -p $APP_HOME \
   && chown -R $APP_UID:$APP_GID $APP_HOME \
   && chown $APP_UID:$APP_GID /program-service.jar \
   && mv /program-service.jar $APP_HOME

USER $APP_UID

WORKDIR $APP_HOME

CMD ["java", "-ea", "-jar", "program-service.jar"]

EXPOSE 50051/tcp
