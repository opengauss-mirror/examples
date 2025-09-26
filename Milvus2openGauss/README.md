# Milvus2OpenGauss

**环境准备：**

- 部署2.3 及以上版本的Milvus实例

- 部署7.0.0-RC1 及以上版本的openGauss实例

- 安装3.8 及以上版本的Python环境

- 安装涉及的Python库

- Python依赖库：

  ```bash
  pip3 install psycopg2
  pip3 install pymilvus==2.4.9
  pip3 install numpy
  ```

**数据生成**

- 如需对迁移功能进行测试，可使用脚本生成数据

  ```bash
  python generate_all.py -n <生成数据条数>
  ```

  默认情况下会在Milvus中生成collection: "test<生成数据条数>"。

**迁移步骤**：

- 各自启动Milvus和OpenGauss服务。

- 配置config.ini，根据本地部署情况修改

  ```bash
  vim config.ini
  ```

  ```ini
  [Milvus]
  host = localhost
  port = 19530
  
  [openGauss]
  user = postgres
  password = xxxxxx
  port = 5432
  database = postgres
  create_index = true
  
  [Table]
  milvus_collection_name = test
  opengauss_table_name = test
  
  [SparseVector]
  # openGauss only support 1000 dimensions for sparsevec
  default_dimension = 1000
  
  [Output]
  folder = output
  
  [Migration]
  cleanup_temp_files = true
  
  ```

- 执行：

  ```bash
  python milvus2opengauss.py
  ```

  如果出现索引配置时间过长或配置失败的情况请在迁移结束后进行手动配置。

- 在迁移结束后可以对迁移正确性和效果等进行测试，执行：

  ```bash
  python test.py
  ```

更多迁移问题欢迎联系3242640173@qq.com