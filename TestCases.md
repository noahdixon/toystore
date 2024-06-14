The following test cases are used to verify that the application works as expected.
The client configuration of the test cases details the command line arguments used to
perform the test. 
In all tests, the frontend server, catalog service server, and three
order service server instances 
were run with the -te argument so that they would print the objects they were sending via HTTP or gRPC
to the standard output before sending them. The three order service instances were given id's
1, 2, and 3.
For each test case, the standard outputs for the
applicable services and/or client are all
captured in a screenshot, placed in the outputs directory, and shown below.

1. Test initial leader election functionality
- The initial leader election functionality can be tested just by starting the three order services,
the catalog service, and the frontend service.
- Output: the outputs below show that order service 3 is elected as the leader since
it has the highest id, and that all three order services are aware of this.
- order service id 3: ![](test_snips/1_order_3.png)
- order service id 2: ![](test_snips/1_order_2.png)
- order service id 1: ![](test_snips/1_order_1.png)
- frontend service: ![](test_snips/1_frontend.png)

2. Test query, buy, and get order functionality
- Client configuration: pr = 0.5 to test that buys are sent with probability 0.5. r = 10 for 10 requests
- Output: the outputs below show that orders are sometimes placed
  after queries when pr is set to 0.5, which is expected. Also, 
we can see from the order service outputs that the leader (service 3)
is servicing buy requests and propagating the data to the replica services. Also,
the output from the frontend server and the catalog service shows that the 
LRU cache is being updated and used to respond quickly to query requests.

- client: ![](test_snips/2_client.png)
- frontend service: ![](test_snips/2_frontend(1).png)
    ![](test_snips/2_frontend(2).png)
    ![](test_snips/2_frontend(3).png)
- catalog service: ![](test_snips/2_catalog(1).png)
![](test_snips/2_catalog(2).png)
- order service id 3: ![](test_snips/2_order_3(1).png)
![](test_snips/2_order_3(2).png)
- order service id 2: ![](test_snips/2_order_2.png)
- order service id 1: ![](test_snips/2_order_1.png)

3. Test querying products that do not exist
- Client configuration: query mode activated with m = q, fake product
  mode activated with f, r = 3 for 3 requests
- Output: the outputs below show that errors are being returned
  because the product "Fake" does not exist, which is expected.
  - client : ![](test_snips/3_client.png)
  - catalog : ![](test_snips/3_catalog.png)
  - frontend : ![](test_snips/3_frontend.png)

4. Test buying products that do not exist
- Client configuration: buy mode activated with m = b, fake product 
mode activated with f, r = 3 for 3 requests
- Output: the outputs below show that errors are being returned
  because the product "Fake" does not exist, which is expected.
  - client : ![](test_snips/4_client.png)
  - catalog : ![](test_snips/4_catalog.png)
  - frontend : ![](test_snips/4_frontend.png)
  - order : ![](test_snips/4_order.png)

5. Test killing an order service replica and bringing it back online
- Client configuration: r = 300 for 300 requests, p = 0.5
- During the client executing, we killed order service replica 2 (non leader) and 
brought it back online
- Output: the output below shows several things. First, all the client requests
are successfully met without issue, and the database order numbers match what the 
client sent. The client does not notice the failures 
(both during order requests and the final order checking phase)
meaning they are transparent to the client. Second, we can see that when service 2
comes back online, it syncs its database with services 1 and 3, by asking them to 
send orders that it missed out on. We can see the services 1 and 3 sent 179 records
to service 2 for updating its database. Finally, looking at the actual entries in the database files for each service, we can see that all three databases match in terms of number of entries and
the values of their entries.

- client : ![](test_snips/5_client.png)
- order service id 3: ![](test_snips/5_order_3.png)
- order service id 2: ![](test_snips/5_order_2.png)
- order service id 1: ![](test_snips/5_order_1.png)
- order service id 3 database: ![](test_snips/5_db_3.png)
- order service id 2 database: ![](test_snips/5_db_2.png)
- order service id 1 database: ![](test_snips/5_db_1.png)

6. Test killing the leader order service replica and bringing it back online
- Client configuration: r = 300 for 300 requests, p = 0.5
- During the client executing, we killed order service 3 (the leader) and
  brought it back online
- Output: the output below shows several things. First, all the client requests
  are successfully met without issue, and the database order numbers match what the
  client sent. The client does not notice the failures
  (both during order requests and the final order checking phase)
  meaning they are transparent to the client. Second, we can see that
from the frontend service output that when the leader dies, it chooses
to elect a new leader and selects service 2 (as it has the next highest
id). Finally, looking at the actual entries in the
  database files for each service, we can see that all 
three databases match in terms of number of entries and
  the values of their entries.
- client : ![](test_snips/6_client.png)
- frontend: ![](test_snips/6_frontend(1).png)
![](test_snips/6_frontend(2).png)
- order service id 3 database: ![](test_snips/6_db_3.png)
- order service id 2 database: ![](test_snips/6_db_2.png)
- order service id 1 database: ![](test_snips/6_db_1.png)

Aside from the above test case, the LRUCache was also tested separately to ensure proper
function. In the /src/frontend-service/src/test/java/com/dixon/frontend directory, the LRUCacheTest class 
tests putting items into the cache, getting items from the cache, invalidating items in the
cache, putting items back into the cache after invalidation, and finally updating an
item that is already in the cache. The output of running the main method in the class
shows the following: 
- Items are able to be successfully added to the cache and the cache 
size will not grow larger than it's specified size.


- If the cache is already at maximum capacity, when an item is added, the least recently accessed
or added item is removed (item at the back of the LRUQueue).


- Getting items from the cache moves whatever item get is called on to the front of the LRU queue.
- Invalidating items removes them from the cache.


- The cache can shrink back down to zero if all items are invalidated.


- Items can be added back in to the cache after it shrinks down to zero.


- Items already in the cache can be updated successfully and are successfully moved
to the front of the LRU queue.