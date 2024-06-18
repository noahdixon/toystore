# Microservice-Based Toy Store

#### A multi-tiered, microservice-based backend application for an online toy store.

## Overview
This project implements a web application backend for a fictional online
toy store using a multi-tier architecture
with three containerized microservices that communicate via gRPC. The store maintains 10 different
toy products and services clients by responding to stock query requests and fulfilling orders. 
The project includes a simulated client program that issues sequential requests to the
store via an HTTP-based REST API. A bash script is also included that is used to start multiple client processes
concurrently to examine latency impacts under increasing client load. A highly detailed description of the application
can be found in the [Design Document](docs/DesignDoc.md). This includes the high level objectives, 
textual and graphical solution overviews, and detailed specifics of the implementation.

## Requirements

The following technologies are required to be able to run the application:
1. **Java Development Kit (JDK) 17 or higher**: Installation instructions can be found
[here](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html).


2. **Apache Maven**: Installation instructions can be found
[here](https://maven.apache.org/install.html).


3. **Docker** (Optional): Installation instructions can be found
   [here](https://docs.docker.com/get-docker/).

## Getting Started

1. Clone the repository and switch to the source directory:
   ```sh
   git clone https://github.com/noahdixon/toystore.git
   cd toystore/src
   ```

2. Compile and package the project into JAR files:
   ```sh
   mvn clean install
   ```

### Run Using Docker Compose
1. Ensure that docker is running on your machine.


2. Build docker images for all
   three microservices and run them in the daemon mode.
   ```sh
   docker compose up -d
   ```
   A few notes on this:
   - The default docker-compose.yml
and ordernodes.conf files specify 3 Order Service replicas, and these files can be
modified to add more replicas per the instructions at the bottom of the 
[Command Line Arguments and Configuration Document](docs/CmdLineArgs.md).

   - The default LRU cache size is 7 and is defined on 
[line 16](https://github.com/noahdixon/toystore/blob/812391cbb53ecbf9850ddfea2eef49dbd6c29882/src/frontend.Dockerfile#L16)
of the [`frontend.Dockerfile`](src/frontend.Dockerfile). This value can be changed
to change the size of the cache. If it is set to 0, caching should also be disabled in the Catalog Service by
removing the -ec argument on [line 17](https://github.com/noahdixon/toystore/blob/812391cbb53ecbf9850ddfea2eef49dbd6c29882/src/catalogservice.Dockerfile#L17)
of the [`catalogservice.Dockerfile`](src/catalogservice.Dockerfile), or commenting out
[line 17](https://github.com/noahdixon/toystore/blob/812391cbb53ecbf9850ddfea2eef49dbd6c29882/src/catalogservice.Dockerfile#L17)
and uncommenting
[line 20]().


3. The Frontend Service is now exposed on port `15623`. To run the client from the same machine, run:
   ```sh
   java -cp "./client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -p 15623
   ```
   Or from a different machine, run:
   ```sh
   java -cp "./client/target/client-1.0-SNAPSHOT.jar" com.dixon.client.Main -s <server address> -p 15623
   ```
   where "host address" specifies the address of the machine where the Docker services are running. These commands
   will runt the client in a default configuration which can be changed using several command line arguments specified
   in the [Command Line Arguments and Configuration Document](docs/CmdLineArgs.md).


4. To stop all microservices and close down the application, run:
   ```shell
   docker compose down
   ```

### Run Without Docker

1. The commands listed below will start each microservice from the command line with a default configuration. In
order to change the configuration of any microservice, please reference the [Command Line Arguments and Configuration Document](docs/CmdLineArgs.md).

2. Start the Catalog Service:
```sh
java -cp "./catalog-service/target/catalogservice-1.0-SNAPSHOT.jar" com.dixon.catalog.CatalogServiceServer

```
