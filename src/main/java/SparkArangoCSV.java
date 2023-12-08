import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.StructField;
import java.util.HashMap;
import scala.reflect.ClassTag$;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

public class SparkArangoCSV {
    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        // Initialize Spark
        SparkSession spark = SparkSession.builder()
                .appName("csv_to_arangodb")
                .master("local[*]")
                .getOrCreate();
        //local[*]

        // // Define the path to your large CSV file
        // String inputFile = "../SparkDataIngestion/input_file.csv"; // Replace with your file path

        // The path to the directory to monitor
        String inputDir = "../SparkDataIngestion"; // Replace with your directory path
        
        // Infer schema from existing data
        StructType csvSchema = spark.read()
                .format("csv")
                .option("header", "true")
                .option("inferSchema", "true")
                .load(inputDir)
                .schema();

        Dataset<Row> df = spark.readStream()
                .format("csv")
                .schema(csvSchema)
                .option("header", "true")
                .load(inputDir);

        Map<String, String> arangoOpts = new HashMap<String, String>();
        arangoOpts.put("password", "rootpassword");
        arangoOpts.put("table", "test");
        arangoOpts.put("ssl.enabled", "False");
        arangoOpts.put("endpoints", "localhost:8529");

        // Write data to ArangoDB as it's read
        StreamingQuery query = df.writeStream()
                .format("com.arangodb.spark")
                .mode(SaveMode.Append)
                .options(arangoOpts)
                .option("checkpointLocation", "checkpoints/")
                .start();

        query.awaitTermination();

        // Stop Spark context
        spark.stop();
    }
}