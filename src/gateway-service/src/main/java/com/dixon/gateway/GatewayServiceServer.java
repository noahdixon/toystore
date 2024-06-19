package com.dixon.gateway;

import com.dixon.common.Address;
import com.dixon.common.OrderServiceNodesReader;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Gateway server that handles query and buy requests from a client
 */
public class GatewayServiceServer {

    /**
     * Launches the server from the command line.
     */
    public static void main(String[] args) {

        // Define command line arguments
        Options options = new Options();
        options.addOption("h", "host", true, "server address");
        options.addOption("p", "port", true, "server port");
        options.addOption("m", "maxThreads", true, "maximum number of threads in dynamic pool");
        options.addOption("b", "backlogRequests", true, "maximum number of backlog requests in dynamic pool");
        options.addOption("ch", "catalogHost", true, "catalog service server address");
        options.addOption("cp", "catalogPort", true, "catalog service server port");
        options.addOption("te", "test", false, "testing mode activated");
        options.addOption("cs", "cacheSize", true, "cache size");
        options.addOption("ts", "timeoutSeconds", true, "timeout limit for order server health check");

        // Read in command line arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Parse command line arguments
        String host = cmd.getOptionValue("host", "0.0.0.0");
        int port = Integer.parseInt(cmd.getOptionValue("port", "1764"));
        int maxThreads = Integer.parseInt(cmd.getOptionValue("maxThreads", "50"));
        int backlogRequests = Integer.parseInt(cmd.getOptionValue("backlogRequests", "50"));
        String catalogHost = cmd.getOptionValue("catalogHost", "0.0.0.0");
        int catalogPort = Integer.parseInt(cmd.getOptionValue("catalogPort", "1765"));
        boolean testMode = cmd.hasOption("te");
        int cacheSize = Integer.parseInt(cmd.getOptionValue("cacheSize", "0"));
        long timeoutSeconds = Long.parseLong(cmd.getOptionValue("timeoutSeconds", "5"));

        // Read the dependent service environment variables
        String catalogHostFromEnv = System.getenv("CATALOG_HOST");
        if(catalogHostFromEnv!= null) {
            catalogHost = catalogHostFromEnv;
        }

        // Check if this is being run in docker by reading the DOCKER_RUN env variable
        boolean dockerRun = false;
        String dockerEnv = System.getenv("DOCKER_RUN");
        if(dockerEnv!= null) {
            dockerRun = Boolean.parseBoolean(dockerEnv);
        }

        // Initialize server
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), backlogRequests);
        } catch (IOException e) {
            System.out.println("Failed to initialize the server");
            e.printStackTrace();
        }

        assert server != null;

        // Read the order service hosts from the conf files
        HashMap<Integer, Address> orderAddresses = OrderServiceNodesReader.readOrderNodes(dockerRun);

        // Initialize orders handler and add mapping
        OrdersHandler ordersHandler = new OrdersHandler(timeoutSeconds, orderAddresses, testMode);
        server.createContext("/orders/", ordersHandler);

        // Initialize products handler with or without cache mode and add mapping
        // Optionally initialize invalidate handler and add mapping
        ProductsHandler productsHandler;
        if (cacheSize > 0) {
            LRUCache cache = new LRUCache(cacheSize);
            productsHandler = new CachingProductsHandler(catalogHost, catalogPort, cache, testMode);
            InvalidateHandler invalidateHandler = new InvalidateHandler(cache);
            server.createContext("/invalidate/", invalidateHandler);
        } else  {
            productsHandler = new ProductsHandler(catalogHost, catalogPort, testMode);
        }
        server.createContext("/products/", productsHandler);

        // Set dynamic thread pool executor
        server.setExecutor(new ThreadPoolExecutor(0, maxThreads,
                60L, TimeUnit.SECONDS, new SynchronousQueue<>()));

        // Start server
        server.start();
        System.out.println("Gateway Service started, listening on port " + port);
    }
}
