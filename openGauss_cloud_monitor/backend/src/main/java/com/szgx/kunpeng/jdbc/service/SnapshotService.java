
package com.szgx.kunpeng.jdbc.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 快照相关
 *
 * @author York_Luo
 * @create 2022-08-01-11:33
 */
@Service
public interface SnapshotService {

    String getSnapshotList() throws SQLException;

    void generateSnapshot() throws SQLException;

    void outputSnapshot(String beginSnapId,String endSnapId,String reportType,String reportScope,String nodeName,String reportName) throws SQLException, IOException;

}
