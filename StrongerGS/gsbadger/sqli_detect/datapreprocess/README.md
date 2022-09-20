# SQLIDetection - Dataprepocess
##### gsbadger工具中SQL注入检测功能的数据预处理部分的实现，基于LibInjection-java开源项目二次开发。



##### SQL语句预处理流程大致如下：

首先如果sqli经过unicode编码过，则进行unicode解码，再将原始sql语句进行tokenize分词。tokenize分词过程大概分为大小写统一、泛化处理、分词（按空格、操作符和括号等特殊符号拆分）这几个步骤。



以下为是使用范例：

```Java
public class Main {

    public static void main(String[] args) {
        /* test a sqli string */
        Libinjection a = new Libinjection();
        a.libinjection_sqli("admin' OR 1=1--");
        System.out.println(a.getOutput()); 

        /* test a file and output its tokenizing results to another file, with options to urldecode and time (in milliseconds) */  
        Test t = new Test();
        t.testfile("data/sqli.txt", "data/sqli-tokenize.txt", true, false);
    }
}
```
