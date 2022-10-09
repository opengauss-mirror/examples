# mongodb to opengauss

在 Node.js 中将数据从 MongoDB 迁移和复制到 opengauss的工具。

该工具的工作原理是创建一个对象数据模型，其中包括您要迁移到 SQL 的所有数据字段并执行迁移脚本。该工具可以选择作为一次性脚本运行，也可以设置为使复制的 openguass 数据库与 MongoDB 保持同步



此项目中node.js连接opengauss 基于 开源的https://gitee.com/opengauss/openGauss-connector-nodejs.git openGauss-connector-nodejs项目

### 运行环境

1. Node.js（在 V16.16.0上测试）

2. npm（在 V8.18.0 上测试）

3. opengauss服务器

4. MongoDB 服务器（在 4.2.2 上测试）

   ### 运行步骤

1. 配置本地环境
2. 配置数据库连接设置
3. 配置数据模型
4. 配置自定义字段（如果需要）
5. 运行脚本

#### 环境设置

将此存储库克隆到本地文件夹后，导航到您选择的控制台/终端中的文件夹，然后运行：

```
npm install
```

安装后，您可以在命令提示符中键入“npm list”，应该会看到类似的内容（省略下拉列表）：

```js
├── @typescript-eslint/eslint-plugin@4.33.0
├── @typescript-eslint/parser@4.33.0
├── async-waterfall@0.1.5
├── async@3.2.4
├── eslint-config-prettier@6.15.0
├── eslint-plugin-node@11.1.0
├── eslint-plugin-prettier@3.4.1
├── eslint@7.32.0
├── lerna@3.22.1
├── moment@2.29.4
├── mongodb@3.7.3
├── pg-connection-string@2.5.0 -> .\packages\pg-connection-string
├── pg-cursor@2.6.0 -> .\packages\pg-cursor
├── pg-pool@3.3.0 -> .\packages\pg-pool
├── pg-protocol@1.5.0 -> .\packages\pg-protocol
├── pg-query-stream@4.1.0 -> .\packages\pg-query-stream
├── pg@8.6.0 -> .\packages\pg
├── prettier@2.1.2
├── typescript@4.7.4
└── yarn@1.22.19
```

#### 数据库连接设置

在 db_details.json 文件中输入 MongoDB 和 openguass 数据库连接设置：	

```js
Example:
{
    "mongo_connection": {
        "url": "mongodb://你的ip地址:27017",
        "database": "你的mongodb数据库"
    },
    "opengauss_connection": {
        "user": "你的用户",
        "host": "你的数据库ip地址",
        "database": "你的opengaus数据库名",
        "password": "xxxxx",
        "port": "你的数据库端口"
    }
}
```

​	根据自身的数据库配置，注意opengauss连接用户 最好用sha256 否则可能会报错

#### 配置数据

已经基于“dummy_data”文件夹中的 JSON 数据创建了一个示例数据库模型设置。有关运行示例数据的说明，请参阅下面的部分。

对象数据模型是在 data_model.js 文件中创建的对象。这包含一个对象列表（每个表一个），在每个对象中都有一个包含表、字段名称和字段类型信息的数组。字段名和表名需要相互匹配才能选择正确的集合名和字段

```
//Example:

module.exports.all_models = {
    table_1_model: [
        'table_1', // 第一个字段是数据表名
        '_id VARCHAR(50) PRIMARY KEY NOT NULL', // 第二个字段总是唯一主键
        'created_at TIMESTAMPTZ', // 对于自动同步是必须的 (可选)
        'updated_at TIMESTAMPTZ', // 对于自动同步是必须的 (optional)
        'title TEXT',
        'authors _TEXT', // 这是一个数组字段
        'languages JSONB' // 这是一个 json 字段
    ],
    table_2_model: [
        'table_2',
        '_id VARCHAR(50) PRIMARY KEY NOT NULL'
    ]
}
```

#### 配置自定义字段（如果需要）

您可能想要访问需要更复杂逻辑的自定义字段。一些示例用例是：

1. MongoDB 字段的名称与您在 opengauss中所需的字段名称不同
2. MongoDB 字段嵌套在一个对象中（有关此内容的更多详细信息，请参见下面的示例过程）
3. 您想在加载到 Postgres 之前执行数据转换

这些自定义字段规则在 migrate_data.js 文件中创建，从第 359 行开始。代码块如下所示：

```
for (let j = 0; j < columns.length; j++) {
    // 如果需要，在此语句中应用自定义规则，否则将使用默认值
    // -------------------------------------------------------
    switch (columns[j]) {
        //用于自定义规则
        case 'common_name':
            insert_row.push(
                json_key(data_row.name, 'common', j)
            )
            break
        default:
            insert_row.push(
                json_key(data_row, columns[j], j)
            )
    }
    // -------------------------------------------------------
}
```

在此处显示的示例中，数据模型中定义为 common_name 的列将应用自定义规则。在这种情况下，MongoDB 文档中 'name' 对象中的属性 'common' 是要迁移的。

#### 运行数据库创建和迁移脚本

此工具有两个基本脚本。要运行它们，请导航到此存储库的根文件夹并运行：

```
node start.js createdb
```

这会将 data_model.js 文件中定义的数据库模型创建到您选择的 openguass实例中。对于基本更改，您可以更改数据模型并重新运行脚本以添加或删除列。

```
node start.js migratedata
```

这将根据数据模型和自定义规则设置执行从 MongoDB 到 openguass的数据迁移。这可以运行一次以一次性迁移数据。或者，如果在 MongoDB 中使用 'created_at' 和 'updated_at' 时间戳日期并且这些日期已包含在 data_model.js 设置中，则可以定期运行脚本以将新的或更新的文档传输到 openguass

## 运行示例

运行示例的步骤：

1. 完成上述设置说明的第 1 步和第 2 步
2. 将dummy_data里面的示例数据上传到 MongoDB 数据
3. 配置数据模型
4. 配置自定义字段
5. 运行脚本

## 其他信息

### 后续可优化和迭代点

- 展示数据逐步执行
- 时间戳字段的用例信息
- 使时间戳字段名称可自定义
- 目前，没有时间戳字段的迁移脚本只能运行一次，并且在执行新的迁移之前需要删除数据。如果不使用时间戳字段，可能会创建选项以在整个脚本中自动删除和重新迁移数据
- 目前支持大部分基础数据类型的转换，对于mongodb的嵌套类型支持不是很好，可以暂时根据自定义规则转换成字符串格式。