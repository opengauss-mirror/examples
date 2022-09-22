package com.szgx.kunpeng.shell.service.impl;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import com.szgx.kunpeng.jdbc.entity.vo.DownloadWRDReportResVO;
import com.szgx.kunpeng.shell.service.ShellService;
import io.minio.MinioClient;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.rmi.ServerException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * shell操作相关
 *
 * @author York_Luo
 * @create 2022-08-02-10:05
 */
@Service
public class ShellServiceImpl implements ShellService {

    @Value("${shell.hostname}")
    String hostname;
    @Value("${shell.username}")
    String username;
    @Value("${shell.password}")
    String password;

    @Value("${minio.url}")
    String url;
    @Value("${minio.accessKey}")
    String accessKey;
    @Value("${minio.secretKey}")
    String secretKey;


    @Override
    public DownloadWRDReportResVO downloadWRDReport(String reportName) throws IOException, InvalidPortException, InvalidEndpointException, NoSuchAlgorithmException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, InvalidBucketNameException, XmlParserException, ErrorResponseException, RegionConflictException, ServerException, InvalidExpiresRangeException {

        Connection conn = getConnection(hostname, username, password);
        SCPClient scpClient = conn.createSCPClient();
        String projectPath = System.getProperty("user.dir");
        String filePath = projectPath + "/WRDReport/";
        scpClient.get("/home/omm/" + reportName + ".html", filePath);
        MinioClient minioClient = new MinioClient(url, accessKey, secretKey, "share");
        String id = "cloud-monitor";
        // 检查存储桶是否已经存在
        boolean isExist = minioClient.bucketExists(id);
        if (isExist) {
            System.out.println("Bucket already exists.");
        } else {
            // 创建一个名为asiatrip的存储桶。
            minioClient.makeBucket(id);
        }
        minioClient.putObject(id, reportName + ".html", filePath + reportName + ".html", null);
        DownloadWRDReportResVO reportResVO = new DownloadWRDReportResVO();
        String url = minioClient.presignedGetObject(id, reportName + ".html");
        reportResVO.setBucketId(id);
        reportResVO.setFileName(reportName + ".html");
        reportResVO.setUrl(url);

        return reportResVO;

    }

    @Override
    public void executeShell(String beginId, String endId, String reportType, String reportScope, String nodeName, String reportName) throws IOException {

        Connection conn = getConnection(hostname, username, password);
        Session session = conn.openSession();
//        session.execCommand("/home/omm/format.sh '\\t \\a \\o test2.html \nselect generate_wdr_report("+beginId+","+endId+", \'all\', \'cluster\', pgxc_node_str()::cstring);'");
//        session.execCommand("/home/omm/generate.sh '\\t \\a \\o test2.html; \nselect generate_wdr_report(,214, \'all\', \'cluster\', pgxc_node_str()::cstring);'");
        if ("node".equals(reportScope)) {
            nodeName = "pgxc_node_str()::cstring";
        }
        if ("cluster".equals(reportScope)) {
            nodeName = null;
        }
        session.execCommand("/home/omm/generate.sh '" + beginId + "' '" + endId + "' '" + reportType + "' '" + reportScope + "' '" + nodeName + "' '" + reportName + "'");

    }


    /**
     * 通过用户名密码获取shell链接
     *
     * @param hostname
     * @param username
     * @param password
     * @return
     */
    private Connection getConnection(String hostname, String username, String password) throws IOException {

        Connection conn = new Connection(hostname);
        conn.connect();
        boolean isAuthenticated = conn.authenticateWithPassword(username, password);
        ///是否登录成功
        if (isAuthenticated == false) {
            throw new IOException("登录失败！！！");
        }

        return conn;

    }
}
