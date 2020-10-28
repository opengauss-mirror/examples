package com.lamdaer.opengauss.gauss.entity.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lamdaer
 * @createTime 2020/10/24
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HobbyListVo {
    @ApiModelProperty(value = "爱好id")
    private Long id;
    
    @ApiModelProperty(value = "一级分类id")
    private Long parentId;
    
    @ApiModelProperty(value = "爱好名称")
    private String text;
    
}
