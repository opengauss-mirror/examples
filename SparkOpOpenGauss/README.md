# SparkOpOpenGauss

这是将 openGuass 作为数据源的spark示例代码，./src/main/scala/org.openguass.spark.sources.datasourcev2

环境和软件版本要求（前置要求）
1. Java 1.8
2. Scala 2.12
3. postgresql.jar 或 opengauss-jdbc--${version}.jar(自己打包或官方提供的jar包)

运行说明：
1. 请确保服务器上的数据库正常运行，且你的机器可正常连接数据库。
2. 以sparkuser用户身份执行./resources/school.sql文件。
3. 请修改代码中sparkuser用户对应的密码、连接数据库的ip及端口，即修改 x.x.x.x:port 。主要代码内容在 `./src/main/scala/`。
4. 关于代码的构建
    + 首先需要获取 openGauss JDBC 的 jar 包，放入本项目的根目录下的libs目录下,在IDEA中添加jar包为依赖。
    + 其次本项目为 maven 项目，IDEA 会通过 maven自动下载其余依赖。
    + 如要使用从中央仓库中获取 openGauss JDBC  的 jar 包，可取消掉pom中 openGuass JDBC 的依赖。并将JDBC url 由 `jdbc:postgresql://x.x.x.x:port/dbname` 改为 `jdbc:opengauss://x.x.x.x:port/dbname` ，其中x.x.x.x是IP，port是端口号，dbname是数据库的名字。
5. 可以在IDEA中进入以下示例文件中的方法，单击右键可直接运行本示例。
   + 可运行`./src/test/scala/org/opengauss/spark/OpenGaussExample`。
   + 另一个直接使用Spark JDBC的例子是 `./src/main/scala/SQLDataSourceExample`。

代码结构说明： 
1. `./src/main/scala/org.opengauss.spark.sources.datasourcev2`包下分别包含了实现Spark Datasource V2 接口的简单数据源的实现示例、简单多数据源的实现示例、流的实现与测试示例与流和批的实现与测试示例。
2. `./src/main/scala/org.opengauss.spark.sources.opengauss`包下包含了将openGauss作为数据源实现Spark Datasouce V2的实现示例。
3. `./src/test/scala/org/opengauss/spark/OpenGaussExample`包含了三个测试方法，分别是 简单数据源的使用与测试示例，读和写 openGauss的使用与测试示例。

PS: Windows 系统在IDEA中调试 `DataSourceV2StreamAndBatchExample` 与 `DataSourceV2StreamingExample`可能会报错，请从[该链接](https://github.com/steveloughran/winutils/tree/master/hadoop-3.0.0/bin) 中下载 hadoop.dll 和 winutils.exe 文件，并将文件路径添加到环境变量中。