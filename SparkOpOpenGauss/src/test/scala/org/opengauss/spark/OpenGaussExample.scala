package org.opengauss.spark

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.scalatest.FlatSpec
import java.sql.DriverManager
import java.util.Properties

import org.scalatest.Matchers.convertToAnyShouldWrapper

class OpenGaussExample extends FlatSpec {

  val testTableName = "course"
  val dburl = "jdbc:postgresql://x.x.x.x:port/school"

  "Simple data source" should "read" in{
    val sparkSession = SparkSession.builder
      .master("local[2]")
      .appName("example")
      .getOrCreate()

    val simpleDf = sparkSession.read
      .format("org.opengauss.spark.sources.datasourcev2.simple")
      .load()

    simpleDf.show()
    println(
      "number of partitions in simple source is " + simpleDf.rdd.getNumPartitions)
  }


  "openGauss data source" should "read table" in  {
    val spark = SparkSession
      .builder()
      .master("local[*]")
      .appName("OpenGaussReaderJob")
      .getOrCreate()

    val simpleRead = spark
      .read
      .format("org.opengauss.spark.sources.opengauss")
      .option("url", dburl)
      .option("user", "sparkuser")
      .option("password", "pwdofsparkuser")
      .option("tableName", testTableName)
      .option("partitionSize", 10)
//      .option("partitionColumn", "name")
      .load()
      .show()

    spark.stop()
  }

  "openGauss data source" should "write table" in {
    val spark = SparkSession
      .builder()
      .master("local[*]")
      .appName("OpenGaussWriterJob")
      .getOrCreate()

    import spark.implicits._

    val df = (60 to 70).map(_.toLong).toDF("product_no")

    df
      .write
      .format("org.opengauss.spark.sources.opengauss")
      .option("url", dburl)
      .option("user", "sparkuser")
      .option("password", "pwdofsparkuser")
      .option("tableName", testTableName)
      .option("partitionSize", 10)
      .option("partitionColumn", "product_no")
      .mode(SaveMode.Append)
      .save()

    spark.stop()
  }



  object Queries {
    lazy val createTableQuery = s"CREATE TABLE $testTableName (user_id BIGINT PRIMARY KEY);"

    lazy val testValues: String = (1 to 50).map(i => s"($i)").mkString(", ")

    lazy val insertDataQuery = s"INSERT INTO $testTableName VALUES $testValues;"
  }

}

