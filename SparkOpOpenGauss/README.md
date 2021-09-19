# SparkOpOpenGauss

这是将 openGuass 作为数据源的spark示例代码。

**环境和软件版本要求（前置要求）：**

1. Java 1.8
2. Scala 2.12
3. postgresql.jar 或 opengauss-jdbc--${version}.jar(自己打包或官方提供的jar包)

**说明：**

1. 请确保服务器上的数据库正常运行，且你的机器可正常连接数据库
2. 以sparkuser用户身份执行./resources/school.sql文件
3. 请修改代码中连接数据库的ip及端口，即修改 x.x.x.x:port 。主要代码内容在 ./src/main/scala/
4. 可以在idea运行本示例。
   + 可运行./src/test/scala/org/opengauss/spark/OpenGaussExample。
   + 另一个直接使用Spark JDBC的例子是 ./src/main/scala/SQLDataSourceExample。