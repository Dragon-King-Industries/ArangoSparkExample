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
        // Define the path to your large CSV file
        String inputFile = "../SparkDataIngestion/input_file.csv"; // Replace with your file path

        Dataset<Row> df = spark.read()
                .format("csv")
                .option("inferSchema", "true")
                .option("header", "true")
                .load(inputFile);

        Map<String, String> arangoOpts = new HashMap<String, String>();
        arangoOpts.put("password", "rootpassword");
        arangoOpts.put("table", "test");
        arangoOpts.put("ssl.enabled", "False");
        arangoOpts.put("endpoints", "localhost:8529");

        df.write()
                .format("com.arangodb.spark")
                .mode(SaveMode.Append)
                .options(arangoOpts)
                .save();


        // Stop Spark context
        spark.stop();
    }
}