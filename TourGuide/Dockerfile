#Version of Java
FROM openjdk:17-jdk-slim

WORKDIR /tourguide

COPY target /tourguide.jar tourguide.jar

#Port for running the app
Expose 8080

#Command to run the app
CMD ["java", "-jar", "tourguide.jar"]