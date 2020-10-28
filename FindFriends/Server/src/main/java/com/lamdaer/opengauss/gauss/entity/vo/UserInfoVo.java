package com.lamdaer.opengauss.gauss.entity.vo;

import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {
    @ApiModelProperty(value = "岗位id")
    @NotNull(message = "jobId can not be empty.")
    private Long jobId;
    
    @ApiModelProperty(value = "爱好id")
    @NotEmpty(message = "hobbyIdList can not be empty.")
    private List<Long> hobbyIdList;
    
    @ApiModelProperty(value = "性别 1 男性 2女性")
    @NotNull(message = "sex can not be empty.")
    @Min(value = 1, message = "Illegal parameter.")
    @Max(value = 2, message = "Illegal parameter.")
    private Integer sex;
    
    @ApiModelProperty(value = "年龄")
    @NotNull(message = "age can not be empty.")
    private Integer age;
    
    @ApiModelProperty(value = "姓名")
    @NotBlank(message = "name can not be empty.")
    private String name;
    
    @ApiModelProperty(value = "手机号")
    @NotBlank(message = "phoneNumber can not be empty.")
    private String phoneNumber;
    
    @ApiModelProperty(value = "昵称")
    @NotBlank(message = "nickName can not be empty.")
    private String nickName;
    
    @ApiModelProperty(value = "头像url")
    @NotBlank(message = "avatarUrl can not be empty.")
    private String avatarUrl;
}
