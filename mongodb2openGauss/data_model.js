// 将 OpenGauss 数据库模型存储在此处
/* 
    每个表列表中的第一项是Postgres表的名称，并且应与MongoDB中的集合名称匹配
    每个表中的第二项应为主键项
    第三项是要从 MongoDB 迁移到 OpenGauss 的列。名称应与 MongoDB 名称匹配，除非您要在迁移脚本中创建自定义规则
*/

module.exports.all_models = {
    mongo_dummy_data_books_model: [
        'mongo_dummy_data_books_model', // 第一项始终为表名称
        '_id VARCHAR(50) PRIMARY KEY NOT NULL', // 第二项是主键字段
        'title TEXT',
        'isbn VARCHAR(100)',
        'pageCount INT4',
        'created_at TIMESTAMPTZ',
        'thumbnailUrl VARCHAR(200)',
        'shortDescription TEXT',
        'longDescription TEXT',
        'status VARCHAR(50)',
        'authors _TEXT', 
        'categories _TEXT',
    ],
    mongo_dummy_data_countries_model: [
        'mongo_dummy_data_countries',
        '_id VARCHAR(50) PRIMARY KEY NOT NULL',
        'altSpellings _TEXT',
        'area NUMERIC',
        'borders _TEXT',
        'callingCode _TEXT',
        'capital VARCHAR(100)',
        'languages JSONB', // JSONB
        'common_name VARCHAR(100)', //自定义字段 - 有关详细信息，请参阅migrate_data.js
    ],
    mongo_dummy_data_profiles_model: [
        'mongo_dummy_data_profiles',
        '_id VARCHAR(50) PRIMARY KEY NOT NULL',
        'client TEXT',
        'updated_at TIMESTAMPTZ', //updated_at和created_at字段可用于动态自动更新数据
    ],    
    mongo_test_data: [
        'mongo_test_data',
        '_id  VARCHAR(50) PRIMARY KEY NOT NULL',
        'doucment TEXT',
    ],
}