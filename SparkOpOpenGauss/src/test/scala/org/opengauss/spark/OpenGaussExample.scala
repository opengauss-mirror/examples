package org.opengauss.spark

import org.apache.spark.sql.{SaveMode, SparkSession}
import org.scalatest.FlatSpec
import java.sql.DriverManager
import java.util.Properties

import org.scalatest.Matchers.convertToAnyShouldWrapper

class OpenGaussExample extends FlatSpec {

  val testTableName = "course"
  val dburl = "jdbc:postgresql://x.x.x.x:port/school" //注意将此处修改成你的机器对应的的ip与端口

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

    val df = (60 to 70).map(_.toLong).toDF("cla_id")

    df
      .write
      .format("org.opengauss.spark.sources.opengauss")
      .option("url", dburl)
      .option("user", "sparkuser")
      .option("password", "pwdofsparkuser")
      .option("tableName", testTableName)
      .option("partitionSize", 10)
      .option("partitionColumn", "cla_id")
      .mode(SaveMode.Append)
      .save()

    spark.stop()
  }

}

