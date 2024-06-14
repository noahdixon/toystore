package com.dixon.catalog;

import com.dixon.common.CSVFileHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * ProductCatalogRecord is the products inventory to be stored/read from the DB
 */
@Getter
@Setter
public class ProductCatalogRecord {

    /**
     * Name of the product ordered
     */
    private String name;

    /**
     * Quantity of the product in the inventory
     */
    private Integer stock;

    /**
     * Price of the product
     */
    private Double price;

    /**
     * Functional interface implementation of the ProductCatalogRecord object factory
     */
    public static class ProductCatalogRecordFactory implements CSVFileHandler.ObjectFactory<ProductCatalogRecord> {

        /**
         * Mapping of the fields to the row order in the CSV file
         */
        public final Map<Integer, String> mapping = new HashMap<>();
        {
            mapping.put(0, "name");
            mapping.put(1, "stock");
            mapping.put(2, "price");
        }

        /**
         * Creates an empty object
         * @return ProductCatalogRecord empty order log record object
         */
        @Override
        public ProductCatalogRecord createInstance() {
            return new ProductCatalogRecord();
        }

        /**
         * Sets the given propertyName field in the object to the value provided
         * @param object ProductCatalogRecord object not having all fields set
         * @param propertyName fieldName in the ProductCatalogRecord object to be set
         * @param value value to be set for the propertyName field
         */
        @Override
        public void setObjectProperty(ProductCatalogRecord object, String propertyName, String value) throws Exception {
            switch (propertyName) {
                case "name":
                    object.setName(value);
                    break;
                case "stock":
                    object.setStock(Integer.valueOf(value));
                    break;
                case "price":
                    object.setPrice(Double.valueOf(value));
                    break;
                default:
                    break;
            }
        }

        /**
         * Gets the field values in a string array in order to be written into the file
         * @param object ProductCatalogRecord object
         * @return String array of all the fields of object in order
         */
        @Override
        public String[] getObjectValues(ProductCatalogRecord object) {
            return new String[]{
                    object.getName(),
                    String.valueOf(object.getStock()),
                    String.valueOf(object.getPrice())
            };
        }

        /**
         * Returns the mapping containing the ordering and fieldnames of the object
         * @return Map of the indices and field names
         */
        @Override
        public Map<Integer, String> getIndexParamMapping() {
            return mapping;
        }
    }
}
