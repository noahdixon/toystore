package com.dixon.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Handles reading from and writing to a csv file for objects of type T
 * @param <T> Object type to by read and written
 */
    public class CSVFileHandler<T> {
        private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_CHAR = "\n";

    /**
     * File Path to read from
     */
    private String filePath;

    /**
     * Default Constructor
      */
    public CSVFileHandler(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Generates a list of objects read from a csv file stored at filePath
     * @param factory Functional interface to create object instances
     * @return List of objects read from csv file
     * @throws IOException
     */
    public List<T> readObjectValuesFromCSV(ObjectFactory<T> factory) throws IOException {
        // Create a new dataRecords list to store the read values
        List<T> dataRecords = new ArrayList<>();

        // Read the file data using Buffered Reader
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // split each line read with comma delimiter
                String[] readValues = line.split(COMMA_DELIMITER);
                T object = createObject(readValues, factory);
                if (object != null) {
                    dataRecords.add(object);
                }
            }
        }
        return dataRecords;
    }

    /**
     * Writes a list of objects to a csv file
     * @param objects List of objects to write
     * @param factory Functional interface to create object instances
     * @param append Indicates whether to append to or overwrite file
     * @throws IOException
     */
    public void writeObjectValuesToCSV(List<T> objects, ObjectFactory<T> factory, boolean append) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath, append)) {
            for (T object : objects) {
                String csvRow = convertObjectToCSVRow(object, factory);
                fileWriter.append(csvRow);
                fileWriter.append(NEW_LINE_CHAR);
            }
        } catch (IOException e) {
            System.out.println("Unable to write to inventory file.");
        }
    }

    /**
     * Converts an object to a comma separated value string
     * @param object The object
     * @param factory Functional interface to create object instances
     * @return A comma separated value string representing the object fields
     */
    private String convertObjectToCSVRow(T object, ObjectFactory<T> factory) {
        StringBuilder rowRecord = new StringBuilder();
        // Get string values array from object properties using factory
        String[] csvRow = factory.getObjectValues(object);

        // Append all the values with comma until the last value
        for(int i=0; i<csvRow.length-1; i++) {
            rowRecord.append(csvRow[i]);
            rowRecord.append(COMMA_DELIMITER);
        }
        // Append the last value of the row
        rowRecord.append(csvRow[csvRow.length-1]);

        return rowRecord.toString();
    }

    /**
     * Creates an object based on mapping and values
     * @param readValues String array representing object field values
     * @param factory Functional interface to create object instances
     * @return New instance of object
     * @throws IOException
     */
    private T createObject(String[] readValues, ObjectFactory<T> factory) throws IOException {
        // Instantiate an object using the factory
        T object = factory.createInstance();
        Map<Integer, String> mapping = factory.getIndexParamMapping();

        // Assign values to object properties based on the mapping
        for (Map.Entry<Integer, String> entry : mapping.entrySet()) {
            int index = entry.getKey();
            String paramName = entry.getValue();

            // Check if the index is within the bounds of values array
            if (index >= 0 && index < readValues.length) {
                // Call setter method of the object using reflection
                try {
                    factory.setObjectProperty(object, paramName, readValues[index]);
                } catch (Exception e) {
                    e.printStackTrace(); // Handle reflection errors here
                }
            } else {
                System.out.println("Index out of bounds error while assining values from CSV to object");
                throw new IOException("Issue in the mapping of CSV values");
            }
        }

        return object;
    }

    /**
     * Functional Interface to create object instances
     * @param <T> Object type
     */
    public interface ObjectFactory<T> {
        T createInstance();
        void setObjectProperty(T object, String propertyName, String value) throws Exception;
        String[] getObjectValues(T object);
        Map<Integer, String> getIndexParamMapping();
    }
}
