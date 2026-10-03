# Appointment API

This project was built using java 25 with SpringBoot 4 and maven >=3.8. It starts an embedded server on port 9090.

### Requirements
- Java 25
- Maven 3.8 or higher
- Docker (for running Postgres and Kafka locally)

### Building the project
```shell
mvn clean package
```

### Running tests
```shell
mvn test
```

### Running the application locally

First, start the containerized Postgres and Kafka instances using Docker Compose. "bootstrap.sql" will create the database, tables and populate them with sample data
```shell
docker-compose up -d
```

Then, run the Spring Boot application using local profile to use the local Postgres instance and populate the database with sample data located on 'snapshot' folder:
```shell
java -jar target/app.jar
```

Swagger is available at http://localhost:9090/swagger-ui/index.html