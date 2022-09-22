package com.szgx.kunpeng.jdbc.entity.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author York_Luo
 * @create 2022-08-02-17:27
 */
@Data
@ApiModel
public class DownloadWRDReportReqVO {

    @ApiModelProperty(example = "1", value = "查询时间段开始的snapshot的id")
    @NotBlank
    String beginSnapId;
    @ApiModelProperty(example = "2", value = "查询时间段结束snapshot的id。默认end_snap_id大于begin_snap_id")
    @NotBlank
    String endSnapId;
    @ApiModelProperty(example = "all",value = "指定生成report的类型。例如：summary：汇总数据。detail：明细数据。all：包含summary和detail")
    @NotBlank
    String reportType;
    @ApiModelProperty(example = "cluster", value = "指定生成report的范围。cluster：数据库级别的信息。node：节点级别的信息。")
    @NotBlank
    String reportScope;
    @ApiModelProperty(example = "",value = "在report_scope指定为node时，需要把该参数指定为对应节点的名称。在report_scope为cluster时，该值可以省略或者指定为空或NULL")
    String nodeName;
//    @ApiModelProperty(example = "C:\\Users\\Desktop",value = "本地下载路径")
//    @NotBlank
//    String localPath;
    @ApiModelProperty(example = "report",value = "报告名字")
    @NotBlank
    String reportName;
}
