# syntax=docker.io/docker/dockerfile:1.7-labs

#############################
# Cache gradle dependencies #
#############################

FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home/
ENV GRADLE_USER_HOME=/home/gradle/cache_home/
COPY *.gradle.kts gradle.properties /home/gradle/app/
COPY --parents */build.gradle.kts /home/gradle/app/
COPY gradle /home/gradle/app/gradle/
WORKDIR /home/gradle/app/
RUN gradle build -i --stacktrace

########################
# Building the project #
########################

FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle/
COPY --chown=gradle:gradle . /home/gradle/src/
WORKDIR /home/gradle/src/
RUN gradle :hub:buildFatJar

############################
# Create the runtime image #
############################

FROM --platform=linux/arm64 arm64v8/amazoncorretto:23-alpine-full AS runtime
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/hub/build/libs/*.jar /app/smarthome-hub.jar
ENTRYPOINT ["java", "-jar", "/app/smarthome-hub.jar"]
