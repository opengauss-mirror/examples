# ER-Model 插件

ER-Model 提供一个基于 Web 的 ER 模型设计器，支持 DBML 双向转换、实例元数据导入、可视化编辑与 SQL 执行。

## 功能特性

- **DBML 编辑 / 可视化联动**：左侧编辑器实时解析 DBML，右侧图形视图同步渲染表与关系。  
- **实例连接与 Schema 管理**：支持平台内已托管实例的选择、Schema 查询与创建、连接测试。  
- **导入导出**：可从 SQL 文件或数据库中加载结构；支持导出 SQL、PlantUML、PNG / PDF 等格式。  
- **自动布局与可视化优化**：内置多种布局方式、列 / 关系高亮、提示信息等交互体验。  
- **SQL 执行**：可将当前 DBML 转换的 DDL 直接执行到选定实例 / Schema。  

---

## 目录结构

```
plugins/er-model
├── src/main/java/org/opengauss/admin/plugin      # 插件后端：控制器、服务、SQL 生成、翻译等
│   ├── controller                                # REST 接口：执行、导入导出、SPA 前置等
│   ├── dto                                       # 数据传输对象（表/列/索引/实例请求等）
│   ├── service                                   # 实例管理、SQL 执行、导出等业务服务
│   ├── sql                                       # 方言适配的 SQL 生成器
│   └── translate                                 # DBML → DTO / SQL 转换逻辑
├── src/main/resources                            # 插件资源文件
│   └── resources                                 # 构建产物：前端静态资源、国际化等
└── web-ui                                        # 前端工程（Quasar + Vue3）
    ├── src/components                            # 组件：图形视图、表格节点、工具提示等
    ├── src/pages                                 # 页面：编辑器、只读展示、错误页等
    ├── src/store                                 # Pinia 数据仓库：编辑器状态、图形状态等
    ├── src/utils                                 # 工具：导出、布局、下载、认证等
    └── src/css                                   # 样式：ER 图主题、全局样式
```

---

# er-model 安装与运行指南

## 一、环境配置

### 1. 安装 openGauss 数据库

请先安装并启动 openGauss 数据库。  
> **参考文档：** [openGauss 参数配置说明](https://gitcode.com/opengauss/openGauss-workbench#补充opengauss参数配置)  
> （特别注意数据库远程用户访问权限的设置）

### 2. 安装依赖环境

确保已安装以下组件：

- Java 17+
- Maven 3.9.0+
- Node.js v18+（含 npm）

并配置好：

- Maven 镜像源
- Node 镜像源

### 3. 安装 SpringBrick 组件

在本地编译并安装：  
[springboot-plugin-framework-parent](https://gitcode.com/wang4721/springboot-plugin-framework-parent.git)

---

## 二、开发环境运行（建议）

### 构建步骤

1. 修改 `openGauss-datakit/visualtool-api` 目录下的 `application-dev.yml`，配置数据库连接、用户名与密码（需为远程用户）。


2. 按照`https://gitcode.com/opengauss/openGauss-workbench 安装步骤，将第五步生成 SSL 密钥的keystore.p12文件拷贝到`openGauss-datakit/config/application-temp.yml` 的`server:ssl:key-store`。保持生成keystore.p12文件的密码和接下来第三步和第七步的密码一致。


3. 在 `openGauss-datakit/config/application-temp.yml` 中配置`server:ssl:key-store-password`。需要与第七步 IDE 启动时环境变量的 `DATA_KIT_AES_KEY` 相同。（大于6位，包含数字、大小写字母、特殊字符）


3. 在项目根目录创建文件夹：

   ```bash
   mkdir visualtool-plugin
   ```

   用于存放构建后的插件 jar 包。（如果 ER-Modeling 项目没有位于根目录，则需要在根目录创建 `visualtool-plugin` 文件夹用于存放 jar 包。）


4. 执行构建脚本：

   ```bash
   sh idea-debug-plugins-api-build.sh
   ```

5. 将 `base-ops` 与 `er-model` 模块中 `target` 目录下的 jar 包拷贝至 `visualtool-plugin` 文件夹。


6. 点击根目录的`pom.xml`，选择将该项目添加为Maven项目。


7. 启动主应用类：

   ```
   openGauss-datakit/visualtool-api/src/main/java/org/opengauss/admin/AdminApplication.java
   ```

​	启动时配置环境变量`DATA_KIT_AES_KEY=YourKey@123`（示例，与第二步你设置的密码`key-store-password`相同）

### 启动与访问

- 默认账号密码：

  ```
  用户名：admin
  密码：admin123
  ```

  首次登录需修改密码。


---

## 三、服务器环境运行

1. **构建安装包**
   ```bash
   sh build.sh
   ```
   成功后会生成：
   ```
   openGauss-Datakit-All-7.0.0-RC3.tar.gz
   ```
   将该文件上传至服务器。

2. **解压安装包**
   ```bash
   tar -xzf openGauss-Datakit-All-7.0.0-RC3.tar.gz
   cd openGauss-datakit
   ```

3. **创建目录**
   ```bash
   mkdir config files ssl logs
   ```

4. **修改配置文件 - 工作目录**
   编辑 `application-temp.yml`：
   ```yaml
   system.defaultStoragePath: /ops/files
   server.ssl.key-store: /ops/ssl/keystore.p12
   logging.file.path: /ops/logs
   ```
   将 `/ops` 替换为实际安装目录（如 `/path/datakit_server`），
   然后将文件移动至 `config` 目录：
   ```bash
   mv application-temp.yml config/
   ```

5. **修改配置文件 - 数据库连接**
   如使用 openGauss 数据库：
   ```yaml
   driver-class-name: org.opengauss.Driver
   url: jdbc:opengauss://ip:port/database?currentSchema=public&batchMode=off
   username: dbuser
   password: ******
   ```

6. **生成 SSL 密钥**
   ```bash
   keytool -genkey -noprompt      -dname "CN=opengauss, OU=opengauss, O=opengauss, L=Beijing, S=Beijing, C=CN"      -alias opengauss -storetype PKCS12 -keyalg RSA -keysize 4096      -keystore /path/datakit_server/ssl/keystore.p12      -validity 365 -storepass ******      -ext "SAN=IP:x.x.x.x"
   ```
   - `-storepass` 值需与配置文件中 `server.ssl.key-store-password` 保持一致  
   - `x.x.x.x` 替换为服务器实际 IP

7. **启动与运维**
   ```bash
   sh ./run.sh start --aes-key xxxxxx    # 启动
   sh ./run.sh stop                      # 停止
   sh ./run.sh restart --aes-key xxxxxx  # 重启
   sh ./run.sh status                    # 查看状态
   ```

8. **访问服务**
   启动成功后，通过浏览器访问：
   ```
   https://ip:9494/
   ```
   默认账号密码：
   ```
   用户名：admin
   密码：admin123
   ```

---

## 四、补充说明

- 若使用 openGauss 作为后台数据库，请确保已正确配置远程连接参数。  
- 详细参数说明可参考：[openGauss 参数配置文档](https://gitcode.com/opengauss/openGauss-workbench#补充opengauss参数配置)。

---

## License

本插件遵循 [Mulan PSL v2](http://license.coscl.org.cn/MulanPSL2) 协议。
