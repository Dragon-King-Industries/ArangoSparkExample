import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import java.util.HashMap;
import java.util.Map;

public class SparkArangoJSON {
    public static void main(String[] args) {
        // Initialize Spark
        SparkSession spark = SparkSession.builder()
                .appName("csv_to_arangodb")
                .master("local[*]")
                .getOrCreate();
        //local[*]
        // Define the path to your large CSV file
        String inputFile = "../SparkDataIngestion/input_json.json"; // Replace with your file path

        Dataset<Row> df = spark.read()
                .format("json")
                .option("inferSchema", "true")
                .load(inputFile);

        Map<String, String> arangoOpts = new HashMap<String, String>();
        arangoOpts.put("password", "rootpassword");
        arangoOpts.put("table", "test2");
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