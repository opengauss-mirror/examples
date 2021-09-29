package org.opengauss.spark.sources.opengauss

import java.sql.DriverManager
import java.util

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.catalog._
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.connector.read._
import org.apache.spark.sql.connector.write._
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import org.apache.spark.unsafe.types.UTF8String

import scala.collection.JavaConverters._

class DefaultSource extends TableProvider {
  override def inferSchema(options: CaseInsensitiveStringMap): StructType = OpenGaussTable.schema

  override def getTable(
                         schema: StructType,
                         partitioning: Array[Transform],
                         properties: util.Map[String, String]
                       ): Table = new OpenGaussTable(properties.get("tableName")) // TODO: Error handling
}

class OpenGaussTable(val name: String) extends SupportsRead with SupportsWrite {
  override def schema(): StructType = OpenGaussTable.schema

  override def capabilities(): util.Set[TableCapability] = Set(
    TableCapability.BATCH_READ,
    TableCapability.BATCH_WRITE
  ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder = new OpenGaussScanBuilder(options)

  override def newWriteBuilder(info: LogicalWriteInfo): WriteBuilder = new OpenGaussWriteBuilder(info.options)
}

object OpenGaussTable {
  /*Table products*/
  /*Database school, table course*/
  val schema: StructType = new StructType().add("cor_id", IntegerType).add("cor_name", StringType).add("cor_type", StringType).add("credit", DoubleType)
}

case class ConnectionProperties(url: String, user: String, password: String, tableName: String, partitionColumn: String, partitionSize: Int)



/** Read */

class OpenGaussScanBuilder(options: CaseInsensitiveStringMap) extends ScanBuilder {
  override def build(): Scan = new OpenGaussScan(ConnectionProperties(
    options.get("url"), options.get("user"), options.get("password"), options.get("tableName"), options.get("partitionColumn"), options.get("partitionSize").toInt
  ))
}

class OpenGaussPartition extends InputPartition

class OpenGaussScan(connectionProperties: ConnectionProperties) extends Scan with Batch {
  override def readSchema(): StructType = OpenGaussTable.schema

  override def toBatch: Batch = this

  override def planInputPartitions(): Array[InputPartition] = Array(new OpenGaussPartition)

  override def createReaderFactory(): PartitionReaderFactory = new OpenGaussPartitionReaderFactory(connectionProperties)
}

class OpenGaussPartitionReaderFactory(connectionProperties: ConnectionProperties)
  extends PartitionReaderFactory {
  override def createReader(partition: InputPartition): PartitionReader[InternalRow] = new OpenGaussPartitionReader(connectionProperties)
}



class OpenGaussPartitionReader(connectionProperties: ConnectionProperties) extends PartitionReader[InternalRow] {
  private val connection = DriverManager.getConnection(
    connectionProperties.url, connectionProperties.user, connectionProperties.password
  )
  private val statement = connection.createStatement()
  private val resultSet = statement.executeQuery(s"select * from ${connectionProperties.tableName}")

  override def next(): Boolean = resultSet.next()

  override def get(): InternalRow = InternalRow(
    resultSet.getInt(1),
    UTF8String.fromString(resultSet.getString(2)),
    UTF8String.fromString(resultSet.getString(3)),
    resultSet.getDouble(4))

  override def close(): Unit = connection.close()

}



/** Write */

class OpenGaussWriteBuilder(options: CaseInsensitiveStringMap) extends WriteBuilder {
  override def buildForBatch(): BatchWrite = new OpenGaussBatchWrite(ConnectionProperties(
    options.get("url"), options.get("user"), options.get("password"), options.get("tableName"), options.get("partitionColumn"), options.get("partitionSize").toInt
  ))
}

class OpenGaussBatchWrite(connectionProperties: ConnectionProperties) extends BatchWrite {
  override def createBatchWriterFactory(physicalWriteInfo: PhysicalWriteInfo): DataWriterFactory =
    new OpenGaussDataWriterFactory(connectionProperties)

  override def commit(writerCommitMessages: Array[WriterCommitMessage]): Unit = {}

  override def abort(writerCommitMessages: Array[WriterCommitMessage]): Unit = {}
}

class OpenGaussDataWriterFactory(connectionProperties: ConnectionProperties) extends DataWriterFactory {
  override def createWriter(partitionId: Int, taskId:Long): DataWriter[InternalRow] =
    new OpenGaussWriter(connectionProperties)
}

object WriteSucceeded extends WriterCommitMessage

class OpenGaussWriter(connectionProperties: ConnectionProperties) extends DataWriter[InternalRow] {

  val connection = DriverManager.getConnection(
    connectionProperties.url,
    connectionProperties.user,
    connectionProperties.password
  )

  val statement = "insert into ${connectionProperties.tableName} (cla_id, cla_name, cla_teacher) values (?,?,?)"
  val preparedStatement = connection.prepareStatement(statement)

  override def write(record: InternalRow): Unit = {
    val cla_id = record.getInt(0)
    val cla_name = record.getString(1)
    val cla_teacher = record.getInt(2)

    preparedStatement.setInt(0, cla_id)
    preparedStatement.setString(1, cla_name)
    preparedStatement.setInt(2, cla_teacher)
    preparedStatement.executeUpdate()
  }

  override def commit(): WriterCommitMessage = WriteSucceeded

  override def abort(): Unit = {}

  override def close(): Unit = connection.close()
}


