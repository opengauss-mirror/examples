package com.lamdaer.opengauss.gauss.entity.vo;

import java.util.List;

import com.lamdaer.opengauss.gauss.entity.Hobby;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lamdaer
 * @createTime 2020/10/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HobbyParentVo {
    @ApiModelProperty(value = "爱好一级分类id")
    private Long id;
    
    @ApiModelProperty(value = "爱好名称")
    private String text;
    
    @ApiModelProperty(value = "爱好二级分类列表")
    private List<HobbyListVo> children;
}
