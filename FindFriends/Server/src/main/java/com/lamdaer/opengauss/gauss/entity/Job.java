package com.lamdaer.opengauss.gauss.entity;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 岗位
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Job对象", description = "岗位")
public class Job implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "岗位id")
    private Long id;
    
    @ApiModelProperty(value = "岗位名称")
    private String name;
    
    
}
