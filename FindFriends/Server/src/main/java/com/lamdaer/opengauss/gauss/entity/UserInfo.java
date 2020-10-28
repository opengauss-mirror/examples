package com.lamdaer.opengauss.gauss.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户信息
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "UserInfo对象", description = "用户信息")
public class UserInfo implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "用户id")
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;
    
    @ApiModelProperty(value = "岗位id")
    private Long jobId;
    
    @ApiModelProperty(value = "爱好id")
    private String hobbyIdList;
    
    @ApiModelProperty(value = "性别 1 男性 2女性")
    private Integer sex;
    
    @ApiModelProperty(value = "姓名")
    private String name;
    
    @ApiModelProperty(value = "年龄")
    private Integer age;
    
    @ApiModelProperty(value = "手机号")
    private String phoneNumber;
    
    @ApiModelProperty(value = "昵称")
    private String nickName;
    
    @ApiModelProperty(value = "头像url")
    private String avatarUrl;
    
    
}
