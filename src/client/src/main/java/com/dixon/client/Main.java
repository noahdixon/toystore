package com.dixon.client;

import com.dixon.common.Data;
import com.dixon.common.Order;
import com.dixon.common.ResponseWrapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Client that makes query and buy requests to a frontend server
 */
public class Main {
    /**
     * Enum to define client mode as sending only queries, only buys, or both
     */
    private enum Mode {
        QUERY,
        BUY,
        BOTH
    }

    /**
     * Client mode (query or buy)
     */
    private static Mode mode;

    /**
     * Object mapper used for marshaling and unmarshalling
     */
    private static ObjectMapper objectMapper;

    /**
     * Http client object
     */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * Toys for client requests
     */
    private static String[] products = new String[]{"Tux", "Whale", "Elephant", "Dolphin", "Fox", "Python", "Shark", "Panda", "Duck", "Turtle"};

    /**
     * Runs client
     * @param args Command line arguments for server host, port, number of requests,
     *             probability of sending orders after successful query, client mode, latency mode
     * @throws IOException
     */
    public static void main( String[] args ) throws IOException {

        // Create object mapper
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        objectMapper = new ObjectMapper(jsonFactory);

        // Define command line arguments
        Options options = new Options();
        options.addOption("s", "server", true, "server address");
        options.addOption("p", "port", true, "port of the server");
        options.addOption("r", "req", true, "number of requests to be sent to the server");
        options.addOption("pr", "probability", true, "probability that order request is sent after query returns in stock");
        options.addOption("m", "mode", true, "client request mode");
        options.addOption("l", "latency", false, "flag to enable latency testing or not");
        options.addOption("f", "fakeproducts", false, "flag to enable querying for products that do not exist");

        // Read in command line arguments
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Get command line arguments
        InetAddress serverName = InetAddress.getByName(cmd.getOptionValue("server", "localhost"));
        int port = Integer.parseInt(cmd.getOptionValue("port", "1764"));
        int numOfReq = Integer.parseInt(cmd.getOptionValue("req", "500"));
        double probOrder = Double.parseDouble(cmd.getOptionValue("probability", "1.0"));
        mode = getMode(cmd.getOptionValue("mode", "both"));
        boolean printLatencies = cmd.hasOption("l");

        // Set products to fake if f argument passed
        if (cmd.hasOption("f")) {
            products = new String[]{"Fake", "Fake", "Fake", "Fake", "Fake", "Fake"};
        }

        // Create custom printer for ignoring printing during latency tests
        CustomPrinter customPrinter = new CustomPrinter(printLatencies);

        // Define uri's
        String uri = "http://" + serverName.getHostName() + ":" + port + "/";
        String products_uri = uri + "products/";
        String orders_uri = uri + "orders/";

        // double to store sum of query latencies
        double queryLatencySum = 0;
        int numQueries = 0;

        // double to store sum of buy latencies
        double buyLatencySum = 0;
        int numBuys = 0;

        // Create list to store sent orders and compare after using get order requests
        List<SentOrder> sentOrders = new LinkedList<>();

        // Sequentially issue requests
        Data data = null;
        int remainingReq = numOfReq;
        Random random = new Random();
        HttpResponse<String> response;
        while (remainingReq > 0) {
            try {
                // Select random toy
                String toy = products[(int) Math.round(Math.random()*9)];

                // Send query depending on mode
                if (mode == Mode.QUERY || mode == Mode.BOTH) {
                    // Create get request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(products_uri + toy))
                            .GET()
                            .build();

                    customPrinter.println("Sending query for " + toy + " to server.");

                    // Start latency time
                    long startTime = System.nanoTime();

                    // Send request and block for response
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // End latency time and append to the sum
                    long endTime = System.nanoTime();
                    queryLatencySum += (endTime - startTime) / 1e6;
                    numQueries += 1;

                    // decrement remaining request count
                    remainingReq--;

                    // Deserialize response
                    ResponseWrapper responseWrapper = objectMapper.readValue(response.body(), ResponseWrapper.class);

                    // Check if response is error
                    if (responseIsError(responseWrapper, customPrinter)) {
                        continue;
                    }

                    // Parse response into Data object and show data
                    data = responseWrapper.getData();
                    customPrinter.println(data.getName() + " costs $" + data.getPrice() + " and there are " + data.getQuantity() + " in stock.");
                }

                // Send buy depending on mode, product stock, and probOrder
                if (mode == Mode.BUY || (mode == Mode.BOTH && remainingReq > 0 && data.getQuantity() > 0 && Math.random() < probOrder)) {
                    // Get order quantity
                    int maxOrderQuantity = Integer.min(5, data.getQuantity());
                    int orderQuantity = random.nextInt(maxOrderQuantity) + 1;

                    // Create order
                    Order order = Order.builder().name(toy).quantity(orderQuantity).build();
                    String orderJSON = objectMapper.writeValueAsString(order);

                    // Create post request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(orders_uri + toy))
                            .POST(HttpRequest.BodyPublishers.ofString(orderJSON))
                            .build();

                    customPrinter.println("Sending order for " + orderQuantity + " " + toy + " to server.");

                    // Start latency time
                    long startTime = System.nanoTime();

                    // Send request and block for response
                    response =  client.send(request, HttpResponse.BodyHandlers.ofString());

                    // End latency time and append to the sum
                    long endTime = System.nanoTime();
                    buyLatencySum += (endTime - startTime) / 1e6;
                    numBuys += 1;

                    // decrement remaining request count
                    remainingReq--;

                    // Deserialize response
                    ResponseWrapper responseWrapper = objectMapper.readValue(response.body(), ResponseWrapper.class);

                    // Check if response is error
                    if (responseIsError(responseWrapper, customPrinter)) { continue; }

                    // Show Order Number
                    customPrinter.println("Order successful. Order number: " + responseWrapper.getData().getOrderNumber());

                    // Add order to sent orders
                    sentOrders.add(SentOrder.builder().name(toy).quantity(orderQuantity).number(responseWrapper.getData().getOrderNumber()).build());
                }

            } catch (ConnectException e) {
                System.out.println("No response from frontend server. Exiting Gracefully");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // Check that sent order data matches database data using get orders
        for(SentOrder sentOrder : sentOrders) {

            // Send request and block for response
            try {
                // Create get order request for order number
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(orders_uri + sentOrder.getNumber()))
                        .GET()
                        .build();

                // Send request and block for response
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Deserialize response
                ResponseWrapper responseWrapper = objectMapper.readValue(response.body(), ResponseWrapper.class);

                // Check if response is error
                if (responseIsError(responseWrapper, customPrinter)) {
                    customPrinter.println("Data Mismatch!");
                    customPrinter.println("Sent order number: " + sentOrder.getNumber() + ", product: " + sentOrder.getName() + ", quantity: " + sentOrder.getQuantity());
                    customPrinter.println("Get order number " + sentOrder.getNumber() + " received error.");
                    return;
                }

                // Show Order Number
                data = responseWrapper.getData();
                if(data.getNumber() != sentOrder.getNumber() ||
                   !data.getName().equals(sentOrder.getName() ) ||
                   data.getQuantity() != sentOrder.getQuantity()) {
                    customPrinter.println("Data Mismatch!");
                    customPrinter.println("Sent order number: " + sentOrder.getNumber() + ", product: " + sentOrder.getName() + ", quantity: " + sentOrder.getQuantity());
                    customPrinter.println("Retrieved order number: " + data.getNumber() + ", product: " + data.getName() + ", quantity: " + data.getQuantity());
                    return;
                }
            } catch (ConnectException e) {
                System.out.println("No response from frontend server. Exiting Gracefully");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        customPrinter.println("All sent order data matches get order data from database.");

        // Print average latency values
        if (printLatencies) {
            if (numQueries > 0) {
                System.out.print(queryLatencySum/numQueries);
            } else {
                System.out.print("0");
            }
            System.out.print(",");
            if (numBuys > 0) {
                System.out.println(buyLatencySum/numBuys);
            } else {
                System.out.println("0");
            }
        }
    }

    /**
     * Prints error code and message and returns true if a response wrapper contains an error,
     * else returns false
     * @param responseWrapper The reponse wrapper
     * @param customPrinter A printer object for printing
     * @return true if a response wrapper contains an error, else false
     */
    private static boolean responseIsError(ResponseWrapper responseWrapper, CustomPrinter customPrinter) {
        if (responseWrapper.getError() != null) {
            customPrinter.println("Error code " + responseWrapper.getError().getCode() + ": " + responseWrapper.getError().getMessage());
            return true;
        }
        return false;
    }

    /**
     * Gets the client Mode based on an input string
     * @param input The input string
     * @return The Mode associated with the string, either QUERY, BUY, ORDER, or BOTH
     */
    private static Mode getMode(String input) {
        if (input.equals("q") || input.equals("query")) {
            return Mode.QUERY;
        }
        if (input.equals("b") || input.equals("buy")) {
            return Mode.BUY;
        }
        return Mode.BOTH;
    }
}
