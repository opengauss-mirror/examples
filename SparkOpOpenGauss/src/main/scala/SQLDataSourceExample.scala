package org.apache.spark.examples.sql

import java.util.Properties

import org.apache.spark.sql.SparkSession

object SQLDataSourceExample {

  case class Person(name: String, age: Long)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("Spark SQL data sources example")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()

    runJdbcDatasetExample(spark)

    spark.stop()
  }


  private def runJdbcDatasetExample(spark: SparkSession): Unit = {
    // $example on:jdbc_dataset$
    // Note: JDBC loading and saving can be achieved via either the load/save or jdbc methods
    // Loading data from a JDBC source
    val dburl = "jdbc:postgresql://x.x.x.x:port/school" //注意修改此处的ip与端口地址
    val jdbcDF = spark.read
      .format("jdbc")
      .option("url", dburl)
      .option("dbtable", "class")
      .option("user", "sparkuser")
      .option("password", "pwdofsparkuser")
      .load()

    val connectionProperties = new Properties()
    connectionProperties.put("user", "sparkuser")
    connectionProperties.put("password", "pwdofsparkuser")
    val jdbcDF2 = spark.read
      .option("customSchema","cla_id INT, cla_teacher STRING")
      .jdbc(dburl, "class", connectionProperties)
    // Specifying the custom data types of the read schema


    connectionProperties.put("customSchema", "cla_id INT, cla_name STRING")
    val jdbcDF3 = spark.read
      .jdbc(dburl, "class", connectionProperties)

    // Saving data to a JDBC source. Create table "customtable1", and write data
    jdbcDF.write
      .format("jdbc")
      .option("url", dburl)
      .option("dbtable", "customtable1")
      .option("user", "sparkuser")
      .option("password", "pwdofsparkuser")
      .save()


    jdbcDF2.write
      .jdbc(dburl, "customtable2", connectionProperties)

    // Specifying create table column data types on write
    jdbcDF3.write
      .option("createTableColumnTypes", "cla_id INT, cla_name VARCHAR(20)")
      .jdbc(dburl, "customtable3", connectionProperties)
  }
}