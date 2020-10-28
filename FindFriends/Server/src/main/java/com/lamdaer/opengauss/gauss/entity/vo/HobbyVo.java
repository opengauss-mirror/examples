package com.lamdaer.opengauss.gauss.entity.vo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

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
public class HobbyVo {
    @NotNull(message = "patentId can not be empty.")
    @ApiModelProperty(value = "一级分类id")
    private Long parentId;
    
    @NotBlank(message = "name can not be empty.")
    @ApiModelProperty(value = "爱好名称")
    private String name;
    
}
