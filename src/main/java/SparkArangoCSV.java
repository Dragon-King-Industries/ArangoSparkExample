import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.entity.BaseDocument;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class SparkArangoCSV {
    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        final String ARANGO_HOST = "localhost";
        final int ARANGO_PORT = 8529;
        final String ARANGO_USER = "root";
        final String ARANGO_PASS = "123456";
        final String ARANGO_DB_NAME = "testdb";
        final String ARANGO_COLLECTION_NAME = "test";

        // Initialize Spark
        SparkSession spark = SparkSession.builder()
                .appName("csv_to_arangodb")
                .master("local[*]")
                .getOrCreate();
        //local[*]

        // The path to the directory to monitor
        String inputDir = "./SparkDataIngestion";

        // Create the directory if it does not exist
        File directory = new File(inputDir);
        if (!directory.exists()) {
            boolean mkdirs = directory.mkdirs();
            if (!mkdirs) {
                System.err.println("Failed to create directory: " + inputDir);
                return;
            }
        }

        // Check if the directory contains any CSV files
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {
            System.err.println("No CSV files found in directory: " + inputDir);
            return;
        }
        
        // Infer schema from existing data
        StructType csvSchema = spark.read()
                .format("csv")
                .option("header", "true")
                .option("inferSchema", "true")
                .load(inputDir + "/*.csv")
                .schema();

        Dataset<Row> df = spark.readStream()
                .format("csv")
                .schema(csvSchema)
                .option("inferSchema", "true")
                .option("header", "true")
                .load(inputDir);

        // Custom ForeachWriter to send the data to ArangoDB
        ForeachWriter<Row> writer = new ForeachWriter<Row>() {
            private transient ArangoDB arangoDB;

            @Override
            public boolean open(long partitionId, long epochId) {
                try {
                    arangoDB = new ArangoDB.Builder()
                            .host(ARANGO_HOST, ARANGO_PORT)
                            .user(ARANGO_USER)
                            .password(ARANGO_PASS)
                            .build();
                    return true;
                } catch (Exception e) {
                    System.err.println("Failed to establish ArangoDB connection: " + e.getMessage());
                    return false;
                }
            }

            @Override
            public void process(Row row) {
                BaseDocument doc = new BaseDocument();
                Arrays.stream(row.schema().fields()).forEach(field -> {
                    String fieldName = field.name();
                    Object fieldValue = row.getAs(fieldName);
                    doc.addAttribute(fieldName, fieldValue);
                });

                try {
                    arangoDB.db(ARANGO_DB_NAME).collection(ARANGO_COLLECTION_NAME).insertDocument(doc);
                } catch (ArangoDBException e) {
                    System.err.println("Failed to insert document: " + e.getMessage());
                }
            }

            @Override
            public void close(Throwable errorOrNull) {
                if (arangoDB != null) {
                    try {
                        arangoDB.shutdown();
                    } catch (Exception e) {
                        System.err.println("Failed to close ArangoDB connection: " + e.getMessage());
                    }
                }
            }
        };

        // Write data to ArangoDB as it's read
        StreamingQuery query = df.writeStream()
                .foreach(writer)
                .outputMode("append")
                .start();

        query.awaitTermination();

        // Stop Spark context
        spark.stop();
    }
}