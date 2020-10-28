package com.lamdaer.opengauss.gauss.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lamdaer.opengauss.gauss.entity.HobbyParent;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyParentVo;

/**
 * <p>
 * 爱好一级分类 服务类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
public interface HobbyParentService extends IService<HobbyParent> {
    /**
     * 添加爱好一级分类
     * @param name 一级分类名称
     * @return
     */
    Boolean addHobbyParent(String name);
    
    /**
     * 获取爱好一级分类列表
     * @return
     */
    List<HobbyParent> getHobbyParentList();
    
    /**
     * 获取爱好一级分类及其对应的二级分类列表
     * @return
     */
    List<HobbyParentVo> getHobbyParentListWithChildren();
}
