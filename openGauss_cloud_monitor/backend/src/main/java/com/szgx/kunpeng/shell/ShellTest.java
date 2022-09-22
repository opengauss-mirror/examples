package com.szgx.kunpeng.shell;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ShellTest {
    public static void main(String[] args) {
        String hostname = "139.159.187.78";//远程机器IP
        String username = "root";//登录用户名
        String password = "keMd@!vD24dSf2r";//登录密码

        try {
            Connection conn = new Connection(hostname);
            conn.connect();
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            ///是否登录成功
            if (isAuthenticated == false) {
                throw new IOException("登录失败！！！");
            }
            SCPClient scpClient = conn.createSCPClient();
            scpClient.get("/home/omm/test.html","C:\\Users\\luoy\\Desktop");
//            Session sess = conn.openSession();
//            //执行命令
//            sess.execCommand("uname -a && date && uptime && who");
//            InputStream stdout = new StreamGobbler(sess.getStdout());
//            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
//            while (true) {
//                String line = br.readLine();
//                if (line == null) {
//                    break;
//                }
//                System.out.println("linux返回结果："+line);
//            }
//            System.out.println("ExitCode: " + sess.getExitStatus());
//            //关闭连接
//            sess.close();
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    /**
     * @param pathOrCommand 脚本路径或者命令
     * @return
     */
    public static List<String> exceShell(String pathOrCommand) {
        List<String> result = new ArrayList<>();

        try {
            // 执行脚本
            Process ps = Runtime.getRuntime().exec(pathOrCommand);
            int exitValue = ps.waitFor();
            if (0 != exitValue) {
                System.out.println("call shell failed. error code is :" + exitValue);
            }

            // 只能接收脚本echo打印的数据，并且是echo打印的最后一次数据
            BufferedInputStream in = new BufferedInputStream(ps.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("脚本返回的数据如下： " + line);
                result.add(line);
            }
            in.close();
            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}
