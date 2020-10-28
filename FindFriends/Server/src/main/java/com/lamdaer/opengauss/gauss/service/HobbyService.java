package com.lamdaer.opengauss.gauss.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyVo;

/**
 * <p>
 * 爱好二级分类 服务类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
public interface HobbyService extends IService<Hobby> {
    
    /**
     * 添加爱好
     * @param hobbyVo 爱好vo
     * @return
     */
    Boolean addHobby(HobbyVo hobbyVo);
    
    /**
     * 获取爱好列表
     * @return
     */
    List<Hobby> getHobbyList();
}
