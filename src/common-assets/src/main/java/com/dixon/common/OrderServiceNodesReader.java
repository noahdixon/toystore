package com.dixon.common;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * Class to read the order service host addresses and port numbers
 */
public class OrderServiceNodesReader {

    /**
     * Config header for Total Quantity
     */
    private static final String ORDER_SERVICES_STRING = "TOTAL_ORDER_SERVICES";
    /**
     * Config header for Order Service
     */
    private static final String ORDER_SERVICES_HEADER = "ORDER_SERVICE_";
    /**
     * Key for the HOST name
     */
    private static final String HOST_HEADER = "HOST_";
    /**
     * Key for the PORT number
     */
    private static final String PORT_HEADER = "PORT_";
    /**
     * Key for the ID of the order services
     */
    private static final String ID_HEADER = "ID_";

    /**
     * During startup, this function reads the order service nodes
     * @param dockerMode a boolean to indicate where the service is running
     * @return Hashmap of the order services with ID numbers as key and address objects as values
     */
    public static HashMap<Integer, Address> readOrderNodes(boolean dockerMode) {
        Properties properties = readProperties(dockerMode);

        String host;
        int port;
        int id;

        // Parse the total number of orders service replicas
        int total_order_services = Integer.parseInt(properties.getProperty(ORDER_SERVICES_STRING));
        HashMap<Integer, Address> orderAddresses = new HashMap<>(total_order_services);

        for(int i=0; i<total_order_services; i++) {
            host = properties.getProperty(ORDER_SERVICES_HEADER + HOST_HEADER + (i+1));
            port = Integer.parseInt(properties.getProperty(ORDER_SERVICES_HEADER + PORT_HEADER + (i+1)));
            id = Integer.parseInt(properties.getProperty(ORDER_SERVICES_HEADER + ID_HEADER + (i+1)));

            orderAddresses.put(id, Address.builder().host(host).port(port).build());
        }

        return orderAddresses;
    }

    /**
     * Reads the properties from the Config and returns the Properties Object
     * @param dockerMode a boolean to indicate where the service is running
     * @return Properties object after reading the config file
     */
    private static Properties readProperties(boolean dockerMode) {
        Properties properties = new Properties();

        if(dockerMode) {
            // if using docker read from ordernodes.conf
            try (InputStream inputStream = OrderServiceNodesReader.class.getClassLoader().getResourceAsStream("ordernodes.conf")) {
                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    System.err.println("Unable to find the properties file");
                }
            } catch (IOException e) {
                System.err.println("Unable to find the properties file");
                e.printStackTrace();
            }
        } else {
            // if not using docker read from ordernodes_local.conf
            String confFilePath = "src/frontend-service/src/main/resources/ordernodes_local.conf";

            try (FileInputStream fis = new FileInputStream(confFilePath)) {
                properties.load(fis);
            } catch (FileNotFoundException ex) {
                System.err.println("Unable to find the properties file");
                System.out.println(ex);
            } catch (IOException ex) {
                System.err.println("Unable to find the properties file");
                System.out.println(ex);
            }
        }
        return properties;
    }

}
