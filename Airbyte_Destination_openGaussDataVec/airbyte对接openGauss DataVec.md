# Airbyte对接openGauss DataVec

本教程讲述如何将openGauss DataVec作为airbyte的destination连接器。

## 环境准备

- 安装较高版本的Docker 或 Docker Desktop
- 配置代理

## 本地部署airbyte

参考官方教程：[Quickstart | Airbyte Docs](https://docs.airbyte.com/platform/using-airbyte/getting-started/oss-quickstart)

本教程以Linux系统为例进行部署。

### 1.安装abctl

abctl是airbyte部署和管理的命令行工具。

```bash
curl -LsfS https://get.airbyte.com | bash -
```

成功后可以看见`abctl install succeeded.`

### 2.运行airbyte

运行命令

```bash
abctl local install --insecure-cookies --low-resource-mode
```

- `--insecure-cookies` 表示通过http进行连接，针对不想配置ssl证书使用安全https连接时适用，可以不用
- `--low-resource-mode`表示在低资源环境下(少于4个CPUs)运行，不能使用Connector Builder工具。可以不用

可通过`abctl local install --help`查看更多参数详情。上述2个参数可根据自身情况选择是否使用。



整个过程大概需要花费30min。安装完成后，可在浏览器中输入 [http://localhost:8000](http://localhost:8000/) 进入登录界面。第一次登录会要求填写邮箱，公司等信息。

> 温馨提示：若安装过程中遇见报错，优先考虑重启机器和更换稳定代理

### 3.查看凭证

查看默认密码命令

```bash
abctl local credentials
```

输出大致为

```bash
Credentials:
Email: user@example.com
Password: a-random-password
Client-Id: 03ef466c-5558-4ca5-856b-4960ba7c161b
Client-Secret: m2UjnDO4iyBQ3IsRiy5GG3LaZWP6xs9I
```

若要修改密码，采用下面命令。

```bash
abctl local credentials --password YourStrongPasswordExample
```



## 加载openGauss DataVec connector镜像

访问 [http://localhost:8000](http://localhost:8000/) (部署airbyte所在机器的ip)登录后，点击Setting -> Destinations -> +New connector

![image-20250918222315397](C:\Users\胡力俨\AppData\Roaming\Typora\typora-user-images\image-20250918222315397.png)

填写表单如下

- `Connector display name`：`openGauss DataVec`
- `Docker repository name`:  `lucashu123/airbyte-connector-opengauss-datavec`
- `Docker image tag`：`latest`

![image-20250918222423289](C:\Users\胡力俨\AppData\Roaming\Typora\typora-user-images\image-20250918222423289.png)

> Connector documentation URL选项有点问题，填充URL后也不会显示文档

点击`Add`，等待2-3min后成功加载。若超过5min，大概率是因为代理网络问题。



之后便可在Destination -> Custom中选择openGauss DataVec作为destination。

![image-20250918223149758](C:\Users\胡力俨\AppData\Roaming\Typora\typora-user-images\image-20250918223149758.png)



