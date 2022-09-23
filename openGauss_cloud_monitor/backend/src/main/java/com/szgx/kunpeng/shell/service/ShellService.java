package com.szgx.kunpeng.shell.service;

import com.szgx.kunpeng.jdbc.entity.vo.DownloadWRDReportResVO;
import io.minio.errors.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.rmi.ServerException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * shell操作相关
 *
 * @author York_Luo
 * @create 2022-08-02-10:04
 */
@Service
public interface ShellService {

    DownloadWRDReportResVO downloadWRDReport(String reportName) throws IOException, InvalidPortException, InvalidEndpointException, NoSuchAlgorithmException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, InvalidBucketNameException, XmlParserException, ErrorResponseException, RegionConflictException, ServerException, InvalidExpiresRangeException;

    void executeShell(String beginId,String endId,String reportType,String reportScope,String nodeName,String reportName) throws IOException;

}
