FROM --platform=linux/arm64 arm64v8/amazoncorretto:23-alpine-full
EXPOSE 8080
RUN mkdir /app
COPY hub/build/libs/*.jar /app/smarthome-hub.jar
ENTRYPOINT ["java", "-jar", "/app/smarthome-hub.jar"]
