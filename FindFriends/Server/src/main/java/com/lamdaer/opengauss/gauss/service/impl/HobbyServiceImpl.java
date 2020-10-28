package com.lamdaer.opengauss.gauss.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lamdaer.opengauss.gauss.common.GaussException;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.HobbyParent;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyVo;
import com.lamdaer.opengauss.gauss.mapper.HobbyMapper;
import com.lamdaer.opengauss.gauss.service.HobbyParentService;
import com.lamdaer.opengauss.gauss.service.HobbyService;

/**
 * <p>
 * 爱好二级分类 服务实现类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Service
public class HobbyServiceImpl extends ServiceImpl<HobbyMapper, Hobby> implements HobbyService {
    @Autowired
    private HobbyParentService hobbyParentService;
    
    @Override
    public Boolean addHobby(HobbyVo hobbyVo) {
        HobbyParent hobbyParent = hobbyParentService.getById(hobbyVo.getParentId());
        if (hobbyParent == null) {
            throw new GaussException(20001, "hobby parent is not exist.");
        }
        Hobby hobby = new Hobby();
        BeanUtils.copyProperties(hobbyVo, hobby);
        int insert = baseMapper.insert(hobby);
        return insert > 0;
    }
    
    @Override
    public List<Hobby> getHobbyList() {
        List<Hobby> hobbies = baseMapper.selectList(null);
        return hobbies;
    }
}
