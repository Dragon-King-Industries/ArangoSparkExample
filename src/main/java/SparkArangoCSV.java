import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.StructField;
import org.arangodb.spark.ArangoSpark;
import org.arangodb.spark.rdd.api.java.ArangoJavaRDD;
import org.arangodb.spark.rdd.api.java.ArangoWriteOptions;
import scala.reflect.ClassTag$;

import java.util.Properties;

public class LargeCSVToArangoDB {
    public static void main(String[] args) {
        // Initialize Spark
        SparkConf sparkConf = new SparkConf().setAppName("LargeCSVToArangoDB");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        SparkSession spark = SparkSession.builder().config(sparkConf).getOrCreate();

        // Define the path to your large CSV file
        String inputFile = "../input_file.csv"; // Replace with your file path

        // Read data from the CSV file in chunks
        StructType schema = new StructType(new StructField[]{
                new StructField("Random String1", DataTypes.StringType, false, null),
                new StructField("Random String2", DataTypes.StringType, false, null),
                // Define the schema for your CSV file
                // Adjust the field names and data types as needed
        });

        Dataset<Row> fileDF = spark.read()
                .option("header", "true") // If your file has a header
                .schema(schema)
                .csv(inputFile);

        // Perform any necessary data transformations
        Dataset<Row> transformedDF = fileDF
                .withColumn("newColumn", functions.lit("someValue")); // Add or modify columns as needed

        // Define ArangoDB connection options
        String database = "mydb"; // Replace with your ArangoDB database name
        String collection = "mycollection"; // Replace with your ArangoDB collection name

        // Use the ArangoDB Datasource for Apache Spark to write data to ArangoDB with bulk insert
        Properties writeProps = new Properties();
        writeProps.setProperty("overwriteMode", "overwrite");
        writeProps.setProperty("parallelism", "4"); // Adjust the parallelism according to your cluster size

        ArangoJavaRDD<Document> arangoRDD = ArangoSpark
                .write(transformedDF, "arangodb://localhost:8529", database, collection, ClassTag$.MODULE$.apply(Document.class), new ArangoWriteOptions(writeProps));

        // Stop Spark context
        spark.stop();
    }
}