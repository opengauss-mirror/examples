package com.lamdaer.opengauss.gauss.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lamdaer.opengauss.gauss.entity.UserInfo;
import com.lamdaer.opengauss.gauss.entity.vo.UserInfoVo;

/**
 * <p>
 * 用户信息 服务类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
public interface UserInfoService extends IService<UserInfo> {
    /**
     * 添加用户信息
     * @param userInfoVo
     * @return
     */
    Long add(UserInfoVo userInfoVo);
}
