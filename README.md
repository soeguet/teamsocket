# TeamSocket

Welcome to the TeamSocket project! This repository is the backend to a Java Swing chat client that could run on local servers like a Raspberry Pi. It's a fun chat project akin to a walkie talkie. You can find the chat client at this link: [TeamChat](https://github.com/soeguet/teamchat).

## Overview

The TeamSocket project is built on the Java-WebSocket from TooTallNate. You can find the project at this link: [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket). This project employs Maven 4 for dependency management and project building.

**Disclaimer:** This project has no advanced security measures implemented yet. It is recommended to use it in a safe, local network environment for the time being.

## Technologies

Here is a list of main technologies and dependencies used in this project:

- [Java 17](https://openjdk.java.net/projects/jdk/17/)
- [Maven 4](https://maven.apache.org/)
- [Java-WebSocket 1.5.3](https://github.com/TooTallNate/Java-WebSocket)
- [PostgreSQL JDBC Driver 42.6.0](https://jdbc.postgresql.org/)
- [Jackson Core, Databind, Annotations 2.15.2](https://github.com/FasterXML/jackson)

## Building and Running the Project

You need Java 17 installed on your machine to build and run the project. After cloning the repository, navigate to the project root directory and execute the following command:

```bash
./mvnw clean install
```

This will compile the code, run the tests, and package the application. Once the build process is completed, you can run the application with the following command:

```bash
java -jar target/teamsocket-1.0-SNAPSHOT.jar
```

(Please replace `teamsocket-1.0-SNAPSHOT.jar` with your actual jar file name if it's different.)

Since all messages are persisted in a PostgreSQL Database, you will need one as well. Easiest way would be to use Docker. You can run the following command to start a PostgreSQL container:

```bash
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres
```

## License

The TeamSocket project is licensed under the [MIT License](https://choosealicense.com/licenses/mit/).
