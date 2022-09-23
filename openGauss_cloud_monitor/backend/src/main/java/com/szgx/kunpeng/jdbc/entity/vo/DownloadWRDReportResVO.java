package com.szgx.kunpeng.jdbc.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author ly
 * @create 2022-08-13-14:21
 */
@Data
public class DownloadWRDReportResVO {

    @ApiModelProperty(example = "asiatrip", value = "minio标签")
    String bucketId;

    @ApiModelProperty(example = "", value = "文件下载地址")
    String url;

    @ApiModelProperty(example = "", value = "文件名")
    String fileName;
}
