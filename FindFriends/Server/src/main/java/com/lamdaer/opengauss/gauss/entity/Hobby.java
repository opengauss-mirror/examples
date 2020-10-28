package com.lamdaer.opengauss.gauss.entity;

import java.io.Serializable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 爱好二级分类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "Hobby对象", description = "爱好二级分类")
public class Hobby implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "爱好id")
    private Long id;
    
    @ApiModelProperty(value = "一级分类id")
    private Long parentId;
    
    @ApiModelProperty(value = "爱好名称")
    private String name;
    
    
}
