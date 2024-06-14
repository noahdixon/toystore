package com.dixon.order;

import com.dixon.OrderRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Creates and Manages the Order DB connections and SQL queries
 */
public class OrderLogDb {

    /**
     * Connection to the SQLite DB
     */
    private Connection conn = null;

    /**
     * Read write lock for synchronization
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Read lock for query synchronization
     */
    private final Lock readLock = readWriteLock.readLock();

    /**
     * Write lock for write synchronization
     */
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * Constructor for the Order DB
     * @param dbFilePath path to the sqlite db file
     */
    public OrderLogDb(String dbFilePath) {
        // Set up the SQLite database connection
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println(e.getMessage());
        }
        if(conn == null) {
            throw new RuntimeException("Couldn't Create a SQLite DB connection");
        }

        // Creating the order_log table
        createOrderLogTable();
    }

    /**
     * Returns the OrderRecord of the given orderNumber
     * @param orderNumber input param to search the DB
     * @return OrderRecord of the associated order number
     */
    public OrderRecord getOrderByNumber(int orderNumber) {
        Statement stmt = null;
        ResultSet rs = null;

        // Query data from the table by order number
        try {
            stmt = conn.createStatement();
            String sql = "SELECT * FROM order_log WHERE order_num=" + orderNumber;

            OrderRecord.Builder orderRecordBuilder = OrderRecord.newBuilder();
            orderRecordBuilder.setOrderNumber(orderNumber);

            readLock.lock();
            rs = stmt.executeQuery(sql);

            if (!rs.isBeforeFirst()) {
                System.out.println("No data found for order number " + orderNumber);
                return null;
            }
            if (rs.next()) {
                // Process the single record
                orderRecordBuilder.setName(rs.getString("product_name"));
                orderRecordBuilder.setQuantity(rs.getInt("quantity"));

                // If there are more than one records retrieved, print an error
                if (rs.next()) {
                    throw new RuntimeException("More than one order found for " + orderNumber);
                }
            }
            return orderRecordBuilder.build();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            readLock.unlock();
            try {
                if (stmt!=null) {
                    stmt.close();
                }
                if (rs!=null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return null;
    }


    /**
     * Returns the list of OrderRecord object after the given orderNumber
     * @param orderNumber input param to search the DB records after this number
     * @return List<OrderRecord> after the given order number
     */
    public List<OrderRecord> getOrdersAfterOrderNumber(int orderNumber) {
        Statement stmt = null;
        ResultSet rs = null;
        List<OrderRecord> orderRecords = new ArrayList<>();

        // Query data from the table by order number
        try {
            stmt = conn.createStatement();
            String sql = "SELECT * FROM order_log WHERE order_num > " + orderNumber + " ORDER BY order_num";

            readLock.lock();
            rs = stmt.executeQuery(sql);

            if (!rs.isBeforeFirst()) {
                // System.out.println("No data found after order number " + orderNumber);
                return orderRecords;
            }
            while (rs.next()) {
                // Process the single record
                OrderRecord.Builder orderRecordBuilder = OrderRecord.newBuilder();
                orderRecordBuilder.setOrderNumber(rs.getInt("order_num"));
                orderRecordBuilder.setName(rs.getString("product_name"));
                orderRecordBuilder.setQuantity(rs.getInt("quantity"));
                orderRecords.add(orderRecordBuilder.build());
            }
            return orderRecords;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            readLock.unlock();
            try {
                if (stmt!=null) {
                    stmt.close();
                }
                if (rs!=null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return orderRecords;
    }

    /**
     * Returns the max / last order number from the DB
     * @return int of the last order number
     */
    public int getMaxOrderNumber() {
        Statement stmt = null;
        ResultSet rs = null;
        int maxOrderNumber = 0;

        try {
            stmt = conn.createStatement();
            String sql = "SELECT MAX(order_num) AS max_order_num FROM order_log";

            readLock.lock();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                maxOrderNumber = rs.getInt("max_order_num");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            readLock.unlock();
            try {
                if (stmt!=null) {
                    stmt.close();
                }
                if (rs!=null) {
                    rs.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
        return maxOrderNumber;
    }

    /**
     * Inserts the given order record into the DB
     * @param orderRecord orderRecord object containing name, quantity
     * @return int indicating the number of records that were inserted
     */
    public int insertOrderRecord(OrderRecord orderRecord) {

        PreparedStatement stmt = null;

        // Insert into the order log table
        try {
            // Prepare the SQL statement
            String sql = "INSERT INTO order_log (order_num, product_name, quantity) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(sql);

            // Set the parameter values
            stmt.setInt(1, orderRecord.getOrderNumber());
            stmt.setString(2, orderRecord.getName());
            stmt.setInt(3, orderRecord.getQuantity());

            writeLock.lock();

            // Execute the SQL statement
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            writeLock.unlock();
            try {
                if (stmt!=null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return -1;
    }

    /**
     * Inserts the given order records into the DB
     * @param orderRecords list of orderRecord objects containing name, quantity
     * @return int indicating the number of records that were inserted
     */
    public int insertOrderRecords(List<OrderRecord> orderRecords) {
        PreparedStatement stmt = null;
        int totalRowsInserted = 0;

        try {
            // Prepare the SQL statement
            String sql = "INSERT INTO order_log (order_num, product_name, quantity) VALUES (?, ?, ?)";
            stmt = conn.prepareStatement(sql);

            for (OrderRecord orderRecord : orderRecords) {
                stmt.setInt(1, orderRecord.getOrderNumber());
                stmt.setString(2, orderRecord.getName());
                stmt.setInt(3, orderRecord.getQuantity());
                stmt.addBatch(); // Add the statement to the batch
            }

            writeLock.lock();
            int[] rowsInserted = stmt.executeBatch(); // Execute the batch

            for (int rows : rowsInserted) {
                if (rows > 0) {
                    totalRowsInserted += rows;
                } else {
                    System.out.println("Failed to insert an order record");
                }
            }

            return totalRowsInserted;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            writeLock.unlock();
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }

        return totalRowsInserted;
    }

    /**
     * Creates an order log SQL table if the table does not exist
     */
    private void createOrderLogTable() {
        Statement stmt = null;
        // Create the order table
        try {
            stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS order_log " +
                    "(order_num INTEGER PRIMARY KEY, " +
                    "product_name TEXT NOT NULL, " +
                    "quantity INTEGER)";

            writeLock.lock();
            stmt.executeUpdate(sql);
            // System.out.println("Table created successfully");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            writeLock.unlock();
            try {
                if (stmt!=null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
