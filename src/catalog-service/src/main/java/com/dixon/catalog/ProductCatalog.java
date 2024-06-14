package com.dixon.catalog;

import com.dixon.CatalogChangeRequest;
import com.dixon.CatalogChangeResponse;
import com.dixon.CatalogQueryRequest;
import com.dixon.CatalogQueryResponse;
import com.dixon.common.Address;
import com.dixon.common.CSVFileHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Product catalog to hold item inventory and cost
 */
public class ProductCatalog {

    /**
     * Inventory of items
     */
    private final HashMap<String, ProductCatalogRecord> products = new HashMap<>();

    /**
     * File Handler to read and write the catalog records to the database file
     */
    private final CSVFileHandler<ProductCatalogRecord> productCatalogFileHandler;

    /**
     * CatalogRecord object factory to help in converting the object to and from the CSV row format
     */
    private final CSVFileHandler.ObjectFactory<ProductCatalogRecord> catalogRecordObjectFactory;

    /**
     * Read write lock for synchronization
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Read lock for query synchronization
     */
    private final Lock readLock = readWriteLock.readLock();

    /**
     * Write lock for buy synchronization
     */
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Http client object for frontend server
     */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * URI for invalidating cache entries at the frontend server
     */
    private static String invalidate_uri;

    /**
     * Boolean to indicate whether cache has been enabled at front end
     */
    private static boolean isCacheEnabled;

    /**
     * Instantiates a new ProductCatalog
     * @param filePath file path of the inventory of the Products
     * @param dbWriteFreq Frequency to update the inventory into the DB/file
     * @param restockFreq Frequency of item restocking
     * @param frontendAddress Address of the frontend service for invalidating cache lines
     * @param isCacheEnabled Indicates whether caching is enabled on the frontend service,
     *                       which determines whether invalidation requests are sent
     * @throws IOException
     */
    public ProductCatalog(String filePath, int dbWriteFreq, int restockFreq, Address frontendAddress, boolean isCacheEnabled) throws IOException {
        productCatalogFileHandler = new CSVFileHandler<>(filePath);
        catalogRecordObjectFactory = new ProductCatalogRecord.ProductCatalogRecordFactory();
        productCatalogFileHandler.readObjectValuesFromCSV(catalogRecordObjectFactory).forEach(
                eachProductRecord -> products.put(eachProductRecord.getName(), eachProductRecord)
        );

        // Define invalidation uri
        invalidate_uri = "http://" + frontendAddress.toString() + "/invalidate/";

        this.isCacheEnabled = isCacheEnabled;

        // Write scheduler to update the inventory if a product went out of stock
        ScheduledExecutorService reStockScheduler = Executors.newScheduledThreadPool(1);

        // Schedule a new thread that restocks the inventory every 10 seconds if a product quantity
        // is less than threshold quantity of 5
        reStockScheduler.scheduleAtFixedRate(() -> {
//            System.out.println("RESTOCKING");
            products.keySet().forEach(productName -> {
                try{
                    CatalogQueryResponse response = query(CatalogQueryRequest.newBuilder().setName(productName).build());
                    // If quantity is less than 5, restock 1000 items
//                    System.out.println(response.getQuantity());
                    if(response.getSuccess() && response.getQuantity() < 5) {
                        changeItem(CatalogChangeRequest.newBuilder()
                                        .setIsIncrement(true)
                                        .setName(productName)
                                        .setQuantity(1000)
                                        .build()
                        );
                    }
                } catch (Exception e) {
                    System.out.println("Exception during restocking the quantity for product " + productName);
                    System.out.println(e);
                }
            });
        }, 0, restockFreq, TimeUnit.SECONDS);

        // Write scheduler to update the inventory into the file
        ScheduledExecutorService dbWriteScheduler = Executors.newScheduledThreadPool(1);

        // Schedule a new thread that writes the inventory every dbUpdateFreq seconds
        dbWriteScheduler.scheduleAtFixedRate(() -> {
            try {
                persistToFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, dbWriteFreq, dbWriteFreq, TimeUnit.SECONDS);
    }

    /**
     * Gets stock and cost of item
     * @param req The query request object containing the product name
     * @return A query response object detailing the stock and cost of the item
     * or an error message if failure
     */
    public CatalogQueryResponse query(CatalogQueryRequest req) {
        String toyName = req.getName();
        // Check if product exists
        if (!products.containsKey(toyName)) {
            return CatalogQueryResponse.newBuilder()
                                .setSuccess(false)
                                .setErrorMessage(toyName + " does not exist in catalog.")
                                .build();
        }

        // Acquire read lock since reading inventory
        readLock.lock();
        ProductCatalogRecord productRecord = products.get(toyName);
        double price = productRecord.getPrice();
        int stock = productRecord.getStock();
        readLock.unlock();

        return CatalogQueryResponse.newBuilder()
                .setSuccess(true)
                .setName(toyName)
                .setPrice(price)
                .setQuantity(stock)
                .build();
    }

    /**
     * Buys/Adds item and reduces/adds inventory by specified quantity if inventory is greater than desired quantity
     * based on the increment flag(increases/decreases the stock of the product)
     * @param req The change request object containing the product name and desired quantity, and an increment flag
     * @return A change response object detailing the success of the change request or an error message if failure
     */
    public CatalogChangeResponse changeItem(CatalogChangeRequest req) {
        String toyName = req.getName();
        // Check if product exists
        if (!products.containsKey(toyName)) {
            return CatalogChangeResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(toyName + " does not exist in catalog.")
                    .build();
        }

        int quantity = req.getQuantity();

        // Acquire write lock since writing to inventory
        writeLock.lock();
        ProductCatalogRecord productRecord = products.get(toyName);
        // If restocking, add quantity and invalidate cache line
        if (req.getIsIncrement()) {
            productRecord.setStock(productRecord.getStock()+quantity);
            writeLock.unlock();
            invalidateCacheLine(toyName);
            return CatalogChangeResponse.newBuilder()
                    .setSuccess(true)
                    .build();
        }
        // If stock is less than quantity, do nothing and return error object
        if (productRecord.getStock() < quantity) {
            writeLock.unlock();
            return CatalogChangeResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(toyName + "'s stock is less than desired quantity.")
                    .build();
        }
        // If stock is greater than or equal to quantity, decrement stock and invalidate cache
        productRecord.setStock(productRecord.getStock()-quantity);
        writeLock.unlock();
        invalidateCacheLine(toyName);
        return CatalogChangeResponse.newBuilder()
                .setSuccess(true)
                .build();
    }

    /**
     * Persists the existing inventory in memory to the DB/File
     * @throws IOException in case of any IO issues
     */
    public void persistToFile() throws IOException {
        List<ProductCatalogRecord> list = new ArrayList<>(products.values());
        productCatalogFileHandler.writeObjectValuesToCSV(list, catalogRecordObjectFactory, false);
    }

    private void invalidateCacheLine(String toyName) {
        if(!isCacheEnabled) {
            return;
        }
        // Send invalidation request to frontend
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(invalidate_uri + toyName))
                .DELETE()
                .build();
        // Send request
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
