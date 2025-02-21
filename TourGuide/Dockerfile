#Version of Java
FROM openjdk:17-jdk-slim

WORKDIR /tourguide

COPY target/*.jar tourguide.jar

#Port for running the app
EXPOSE 8080

#Command to run the app
CMD ["java", "-jar", "tourguide.jar"]