# Command Line Arguments and Configuration Files

Below is a comprehensive list of the command line arguments accepted by each microservice and by the simulated client. 
An explanation for the Order Service replicas' configuration files is also given at the very bottom of the document.

### Client

- `-s <server address>` or `-server <server address>` specifies the hostname of the gateway Service and defaults to 
localhost if not specified.
- `-p <port number>` or `-port <port number>` specifies the port of the gateway Service and defaults to 1764 if not specified.
- `-r <number>` or `-req <number>` specifies the number of requests to be sent by the Client and defaults to 500 if not specified. 
This includes product query and product buy requests. For each buy request sent, an additional query order request will be sent
during the check orders phase.
- `-pr <probability>` or `-probability <probability>` specifies the probability of sending a product buy request
if a successful product query request reveals that an item is in stock. 
The probability must be between 0.0 and 1.0, and defaults to 1.0 if not specified.
- `-m <mode>` or `-mode <mode>` specifies the Client "mode" indicating if it should send only query or buy requests,
bypassing the `-pr` argument. `<mode>` must be either `q` or `query` for sending only queries, or `b` or `buy` for sending only buys.
If not passed, the Client will send both queries and buys as defined by the `-pr` argument. 
- `-l` or `-latency` flag instructs the Client to output only the average query and buy request latency in 
the form "<query latency>,<buy latency>". This flag is used during testing to estimate the latency seen by the Client
under various conditions.
- `-f` or `-fakeproducts` flag instructs the Client to only query for products that do not exist. This flag is 
used during testing to check that the application responds properly to query requests for fake products.

### gateway Service

- `-h <host address>` or `-host <host address>` specifies the network interface for the HttpServer, and defaults to
0.0.0.0 if not specified.
- `-p <port number>` or `-port <port number>` specifies the port the service will listen on, and defaults to
1764 if not specified.
- `-m <number>` or `-maxThreads <number>` specifies the max number of worker threads that will exist
concurrently in the thread pool, and defaults to 50 if not specified.
- `-b <number>` or `-backlogRequests <number>` specifies the maximum number of incoming connections that can be queued 
to be accepted by the server. If the queue is full, additional incoming connections may be refused. 
This number defaults to 50 if not specified.
- `-ch <catalog address>` or `-catalogHost <catalog address>` specifies the hostname of the Catalog Service and defaults to
  0.0.0.0 if not specified.
- `-cp <catalog port>` or `-catalogPort <catalog port>` specifies the port of the Catalog Service and defaults to 1765 if not specified.
- `-te` or `-test` flag instructs the service to run in testing mode, meaning it will output information about the data it 
is sending.
- `-cs <size>` or `-cacheSize <size>` specifies the size of the LRU cache. If set to 0 or not specified, caching will be
disabled.
- `-ts <seconds>` or `-timeoutSeconds <seconds>` specifies the number of seconds the service will wait to hear back from 
an Order Service replica during leader election before determining that the replicas is offline. This defaults to 5
seconds if not specified.

### Catalog Service

- `-p <port number>` or `-port <port number>` specifies the port the service will listen on, and defaults to
  1765 if not specified.
- `-m <number>` or `-maxThreads <number>` specifies the max number of worker threads that will exist
  concurrently in the thread pool, and defaults to 50 if not specified.
- `-f <path>` or `-filePath <path>` specifies the file path for the inventory.csv file. This defaults to
src/catalog-service/src/main/resources/inventory.csv if not specified or /data/inventory.csv if
not specified and running via Docker Compose.
- `-ut <seconds>` or `-updateTime <seconds>` specifies the frequency in seconds of writes to the inventory.csv file,
and defaults to 600 if not specified. 
- `-rt <seconds>` or `-restockTime <seconds>` specifies the frequency in seconds of restock checks,
  and defaults to 10 if not specified.
- `-te` or `-test` flag instructs the service to run in testing mode, meaning it will output information about the data it
  is sending.
- `-fs <gateway address>` or `-gatewayServer <gateway address>` specifies the hostname of the gateway Service and defaults to
  localhost if not specified.
- `-fp <gateway port>` or `-gatewayPort <gateway port>` specifies the port of the gateway Service and defaults to 1764 if not specified.
- `-ec` or `-enableCache` flag enables caching, which instructs the service to send cache invalidation requests 
to the gateway Service when stock is changed. This should always be passed if caching is enabled at the gateway Service
(if the cache size at the gateway Service is set larger than 0).

### Order Service

- `-p <port number>` or `-port <port number>` specifies the port the service will listen on, and defaults to
  1766 if not specified.
- `-m <number>` or `-maxThreads <number>` specifies the max number of worker threads that will exist
  concurrently in the thread pool, and defaults to 50 if not specified.
- `-ch <catalog address>` or `-catalogHost <catalog address>` specifies the hostname of the Catalog Service and defaults to
  0.0.0.0 if not specified.
- `-cp <catalog port>` or `-catalogPort <catalog port>` specifies the port of the Catalog Service and defaults to 1765 if not specified.
- `-f <path>` or `-filePath <path>` specifies the file path for the order log db file. This defaults to
src/order-service/src/main/resources/orderlog_x.db if not specified or /data/orderlog_x.db if not specified and running 
via Docker Compose, where x is the instance ID.

### Order Service Configuration Files

Each Order Service instance is made aware of other order service nodes via a configuration file. This file is called 
ordernodes_local.conf or ordernodes.conf if running via Docker Compose. The default configuration is 3 Order Service nodes,
and the default ordernodes_local.conf is shown below:
```lombok.config
TOTAL_ORDER_SERVICES = 3
ORDER_SERVICE_HOST_1 = localhost
ORDER_SERVICE_PORT_1 = 1766
ORDER_SERVICE_ID_1 = 1

ORDER_SERVICE_HOST_2 = localhost
ORDER_SERVICE_PORT_2 = 1767
ORDER_SERVICE_ID_2 = 2

ORDER_SERVICE_HOST_3 = localhost
ORDER_SERVICE_PORT_3 = 1768
ORDER_SERVICE_ID_3 = 3
```
This file can be modified to add more nodes so long as the `TOTAL_ORDER_SERVICES` value is changed and the
new nodes are added following the format above. For example, in order to add one more node that runs locally
and listens on port 1769, the file should be changed to:
```lombok.config
TOTAL_ORDER_SERVICES = 4
ORDER_SERVICE_HOST_1 = localhost
ORDER_SERVICE_PORT_1 = 1766
ORDER_SERVICE_ID_1 = 1

ORDER_SERVICE_HOST_2 = localhost
ORDER_SERVICE_PORT_2 = 1767
ORDER_SERVICE_ID_2 = 2

ORDER_SERVICE_HOST_3 = localhost
ORDER_SERVICE_PORT_3 = 1768
ORDER_SERVICE_ID_3 = 3

ORDER_SERVICE_HOST_4 = localhost
ORDER_SERVICE_PORT_4 = 1769
ORDER_SERVICE_ID_4 = 4
```

If running using Docker Compose, the default ordernodes.conf file looks like:

```lombok.config
TOTAL_ORDER_SERVICES = 3
ORDER_SERVICE_HOST_1 = order-service-1
ORDER_SERVICE_PORT_1 = 1766
ORDER_SERVICE_ID_1 = 1

ORDER_SERVICE_HOST_2 = order-service-2
ORDER_SERVICE_PORT_2 = 1766
ORDER_SERVICE_ID_2 = 2

ORDER_SERVICE_HOST_3 = order-service-3
ORDER_SERVICE_PORT_3 = 1766
ORDER_SERVICE_ID_3 = 3
```

Here, each instance is given an ID and a hostname (ex. "order-service-1").
In the docker-compose.yml file, the ID is set as an environment variable in the Docker 
service along with the Catalog Service hostname and the DOCKER_RUN flag. The hostname is set as an alias 
for the service within the default Docker network. In order to add a fourth Order Service node, the ordernodes.conf
file could be changed to: 

```lombok.config
TOTAL_ORDER_SERVICES = 4
ORDER_SERVICE_HOST_1 = order-service-1
ORDER_SERVICE_PORT_1 = 1766
ORDER_SERVICE_ID_1 = 1

ORDER_SERVICE_HOST_2 = order-service-2
ORDER_SERVICE_PORT_2 = 1766
ORDER_SERVICE_ID_2 = 2

ORDER_SERVICE_HOST_3 = order-service-3
ORDER_SERVICE_PORT_3 = 1766
ORDER_SERVICE_ID_3 = 3

ORDER_SERVICE_HOST_4 = order-service-4
ORDER_SERVICE_PORT_4 = 1766
ORDER_SERVICE_ID_4 = 4

```

and the following can be added as a new service in the docker-compose.yml file:

```
order_4:
    build:
      context: .
      dockerfile: orderservice.Dockerfile
    environment:
      - CATALOG_HOST=catalog
      - DOCKER_RUN=true
      - SELF_ID=4
    depends_on:
      - catalog
    volumes:
      - ./data:/data
    networks:
      default:
        aliases:
          - order-service-4
```
