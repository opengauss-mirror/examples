# PHP Connector with OpenGauss

# 说明

```jsx
@author woov(吴未)<wooovi@qq.com>
@version 2.0
@since 2022.08.15
```

# 特性

- 全功能ORM
- 每个特性都经过了测试的重重考验
- 开发者友好

# CRUD接口

## 连接openGauss数据库

```php
$conn = new gauss_class($host,$user,$pass,$dbname,$port);
```

连接是通过创建 gauss 基类的实例而建立的。连接成功返回其他gauss函数需要的资源**conn**，连接失败提示错误并返回false。

**参数说明**

[Untitled](https://www.notion.so/3eec90eb2c19413ebdea798814a2812e)

## 关闭连接

```php
$conn = new gauss_class($host,$user,$pass,$dbname,$port);
//在此使用连接

//运行完成关闭连接
$conn = null;
```

连接数据成功后，返回一个 gauss类的实例给脚本，此连接在 gauss 对象的生存周期中保持活动。要想关闭连接，需要销毁对象以确保所有剩余到它的引用都被删除，可以赋一个 **`null`** 值给对象变量。如果不明确地这么做，PHP 在脚本结束时会自动关闭连接。关闭由所给资源$conn指到的Opengauss数据库的非持久连接。

## 创建

```php
$arr = array('username'=>'test1','pass'=>123456,'real_name'=>'test1');
$conn->Create('test',$arr);
```

## 查询

```php

$conn->Select('id','user');
$conn->Where("username = ?",'test1');
$data = $conn->Find('test');
```

```php
$conn->Select('*');
$conn->Where("username = 'test1'");
$data = $conn->First('test');
```

## 更新

```php
$conn->Where("username = 'test'");
$arr = array('pass'=>'123');
$data = $conn->Update('test',$arr);
```

## 删除

```php
$conn->Where("username = 'test1'");
$data = $conn->Delete('test');
```

## 原生SQL

## query -执行查询

```php
$result = $conn -> query($sql);
```

## fetch_all - 从结果中提取所有行作为一个数组

```php
$data = $conn -> fetch_all($result);
```

**fetch_all()** 从结果资源中返回一个包含有所有的行（元组/记录）的数组。如果没有更多行可供提取，则返回 **`false`**。

## fetch_assoc -提取一行作为关联数组

```php
$data = $conn -> fetch_assoc($result);
```

**fetch_assoc()** 它只返回一个关联数组。

## fetch_array -提取一行作为数组

```php
$data = $conn -> fetch_array($result)
```

**fetch_array()**返回一个与所提取的行（元组/记录）相一致的数组。如果没有更多行可供提取，则返回 **`false`**。

## 其他函数

## numRows -获取行的数目

```php
$count = $conn -> numRow($result);
```

**numRow()返回result**中的行的数目。其中result参数是由query()函数返回的查询结果资源号。出错则返回-1。

## affectedRows -获取受影响的记录数目

```php
$count = $conn -> affectedRows($result)
```

**affectesRows()**返回**query()**中执行的INSERT,UPDATE和DELECT查询后受到影响的记录数目（包括实例/记录/行）。如果本函数没有影响到任何记录，则返回0。其中result参数是由query()函数返回的查询结果资源号。

### connReset -重置连接

```php
$conn->connRest();
```

**connReset()成功返回`ture`**,否则返回`**false()**`(注意：返回类型为bool)。

## dbname -获取表名

```php
$res = $conn -> dbname();
```

## isbusy -获知连接是否为忙

```php
$isbusy = $conn->isbusy()
if ($isbusy){
    echo 'busy';
}
else{
    echo 'free';
}
```

当连接忙时，函数返回值为ture(bool)，否则为false