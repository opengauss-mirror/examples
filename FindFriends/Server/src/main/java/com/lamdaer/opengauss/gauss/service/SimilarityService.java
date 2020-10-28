package com.lamdaer.opengauss.gauss.service;

import java.util.List;

import com.lamdaer.opengauss.gauss.entity.UserInfo;

/**
 * @author lamdaer
 * @createTime 2020/10/24
 */
public interface SimilarityService {
    /**
     * 相似度计算
     * @param userId
     * @return
     */
    List<UserInfo> similarity(Long userId);
}
