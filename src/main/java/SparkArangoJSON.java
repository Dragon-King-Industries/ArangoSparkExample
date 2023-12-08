import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class SparkArangoJSON {
    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        // Initialize Spark
        SparkSession spark = SparkSession.builder()
                .appName("json_to_arangodb")
                .master("local[*]")
                .getOrCreate();
        //local[*]

        // // Define the path to your large CSV file
        // String inputFile = "../SparkDataIngestion/input_json.json"; // Replace with your file path

        // The path to the directory to monitor
        String inputDir = "../SparkDataIngestion"; // Replace with your directory path
        
        Dataset<Row> df = spark.readStream()
                .format("json")
                .option("multiline", "true") 
                .load(inputDir);

        Map<String, String> arangoOpts = new HashMap<String, String>();
        arangoOpts.put("password", "rootpassword");
        arangoOpts.put("table", "test2");
        arangoOpts.put("ssl.enabled", "False");
        arangoOpts.put("endpoints", "localhost:8529");

        // Write data to ArangoDB as it's read
        StreamingQuery query = df.write()
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