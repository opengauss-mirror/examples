package com.szgx.kunpeng.basic.tool;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author zhangrb
 * @date 2021/7/8
 */
@Data
public class ExecuteResult {

    @ApiModelProperty(value = "执行语句")
    private String code;
    @ApiModelProperty(value = "类型")
    private String type;
    @ApiModelProperty(value = "select类型返回集合")
    private List<Map<String,Object>> table;
    @ApiModelProperty(value = "成功标志")
    private  boolean success = true;
    @ApiModelProperty(value = "执行结果")
    private String result;
    @ApiModelProperty("执行耗时")
    private String time;

    public ExecuteResult(){
    }

    public ExecuteResult(String code, String result){
        this.type = "UNKNOWN";
        this.result = result;
        this.code = code;
        this.time = "0秒";
    }
}
