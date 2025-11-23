FROM eclipse-temurin:21-jdk-noble
WORKDIR /workspace
COPY target/pizza-store-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
