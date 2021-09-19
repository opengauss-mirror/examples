package org.opengauss.spark.sources.datasourcev2.streaming

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.OutputMode

object DataSourceV2StreamingExample {
  def main(args: Array[String]): Unit = {
    val sparkSession = SparkSession.builder.
          master("local[2]")
          .appName("streaming example")
          .getOrCreate()

    val streamingDf = sparkSession.
      readStream.
      format("cn.ecnu.spark.sources.datasourcev2.streaming.simple")
      .load()



    val query = streamingDf.writeStream
      .format("console")
      .queryName("simple_source")
    .outputMode(OutputMode.Append())

    query.start().awaitTermination()

  }

}
