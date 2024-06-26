# Microservice-Based Toy Store

#### A multi-tiered, microservice-based backend application for an online toy store.

## Table of Contents
- [Overview](#overview)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Run Using Docker Compose](#run-using-docker-compose)
  - [Run Without Docker](#run-without-docker)
- [Acknowledgements](#acknowledgements)

## Overview
This project implements a web application backend for a fictional online
toy store using a multi-tier architecture
with three containerized microservices that communicate via gRPC. The store maintains 10 different
toy products and services clients by responding to stock query requests and fulfilling orders. 
The project includes a simulated client program that issues sequential requests to the
store via an HTTP-based REST API. A bash script is also included that is used to start multiple client processes
concurrently to examine latency impacts under increasing client load. A highly detailed description of the application
can be found in the [Design Document](docs/DesignDoc.md). This includes the high level objectives, 
textual and graphical solution overviews, and detailed specifics of the implementation. The test cases used to verify
the application's functional correctness and examine its performance under increasing load are detailed in the
[Testing and Evaluation Document](TestDoc.md). Instructions for running the application are outlined below.

## Requirements

The following technologies are required to be able to run the application:
- **Java Development Kit (JDK) 17 or higher**: Installation instructions can be found
   [here](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html).


- **Apache Maven**: Installation instructions can be found
   [here](https://maven.apache.org/install.html).


- **Docker** (Optional): Installation instructions can be found
   [here](https://docs.docker.com/get-docker/).

## Getting Started

1. Clone the repository and switch into the project source directory:
   ```sh
   git clone https://github.com/noahdixon/toystore.git
   cd toystore/src
   ```

2. Compile and package the project into JAR files:
   ```sh
   mvn clean install
   ```

The application can now be run by either starting the microservices via Docker Compose,
or starting each service manually from the command line. In both cases the application will be 
run with a default configuration. This configuration specifies 3 Order Service replicas,
enables caching with a cache size of 7, and sets several other parameters to default values. 
In order to learn about the parameters for each service and the Client and how they can be changed,
please reference the [Command Line Arguments and Configuration Document](docs/ConfigurationDoc.md).

### Run Using Docker Compose
1. Ensure that docker is running on your machine.


2. Build docker images for all
   three microservices and run them in the daemon mode.
   ```sh
   docker compose up -d
   ```

3. The Gateway Service is now exposed on port `15623`. To run the client from the same machine, run:
   ```sh
   java -cp "client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -p 15623
   ```
   Or from a different machine, run:
   ```sh
   java -cp "client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -s <server address> -p 15623
   ```
   where "host address" specifies the address of the machine where the Docker services are running.


4. To stop all microservices and close down the application, run:
   ```shell
   docker compose down
   ```

### Run Without Docker

1. Start the Catalog Service:
   ```sh
   java -cp "catalog-service/target/catalogservice-1.0-SNAPSHOT.jar" com.dixon.catalog.CatalogServiceServer -ec
   ```


2. In separate terminals, start the Order Service replicas. 
The default configuration is 3 replicas with ID's 1, 2, and 3. 
Again, this configuration can be changed via the instructions at the bottom of the [Command Line Arguments and Configuration Document](docs/ConfigurationDoc.md).

   ```sh
   SELF_ID=1 java -cp "order-service/target/orderservice-1.0-SNAPSHOT.jar" com.dixon.order.OrderServiceServer -p 1766 
   ```
   
   ```sh
   SELF_ID=2 java -cp "order-service/target/orderservice-1.0-SNAPSHOT.jar" com.dixon.order.OrderServiceServer -p 1767
   ```
   
   ```sh
   SELF_ID=3 java -cp "order-service/target/orderservice-1.0-SNAPSHOT.jar" com.dixon.order.OrderServiceServer -p 1768
   ```


3. In a separate terminal, start the Gateway Service.

   ```sh
   java -cp "gateway-service/target/gatewayservice-1.0-SNAPSHOT.jar" com.dixon.gateway.GatewayServiceServer -cs 7
   ```


4. The Gateway Service is now exposed on port `1764`. To run the client from the same machine, in a separate terminal run:
   ```sh
   java -cp "client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -p 1764
   ```
   Or from a different machine, run:
   ```sh
   java -cp "client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -s <server address> -p 1764
   ```
   where "host address" specifies the address of the machine where the Docker services are running.


5. To stop each microservice and close down the application, simply enter control-C in the
terminal for each service:
   ```shell
   ^C
   ```
   
## Acknowledgements
1. Followed https://github.com/grpc/grpc-java for pom.xml changes to enable gRPC to be built using maven.


2. Followed https://github.com/grpc/grpc-java/issues/10826 to fix the issues of dependent classes not being available.
   Also used the same resource to decide on the packaging plugin of maven shade to construct single jar with all dependencies.


3. Followed https://www.tutorialspoint.com/commons_cli/commons_cli_quick_guide.htm to read the command line arguments from CLI.


4. Followed https://www.baeldung.com/java-csv-file-array#buff-reader to read the csv file as buffer reader.


5. Read this https://stackoverflow.com/questions/27767264/how-to-dockerize-maven-project-and-how-many-ways-to-accomplish-it
   and https://stackoverflow.com/questions/56438864/dockerize-a-multi-maven-project-not-multi-module for containerization help.


6. Followed https://www.baeldung.com/java-lru-cache for inspiration on LRUCache design.