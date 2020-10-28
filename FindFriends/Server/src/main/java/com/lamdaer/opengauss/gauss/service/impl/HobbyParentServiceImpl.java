package com.lamdaer.opengauss.gauss.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lamdaer.opengauss.gauss.common.GaussException;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.HobbyParent;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyListVo;
import com.lamdaer.opengauss.gauss.entity.vo.HobbyParentVo;
import com.lamdaer.opengauss.gauss.mapper.HobbyParentMapper;
import com.lamdaer.opengauss.gauss.service.HobbyParentService;
import com.lamdaer.opengauss.gauss.service.HobbyService;

/**
 * <p>
 * 爱好一级分类 服务实现类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Service
public class HobbyParentServiceImpl extends ServiceImpl<HobbyParentMapper, HobbyParent> implements HobbyParentService {
    @Autowired
    private HobbyService hobbyService;
    
    @Override
    public Boolean addHobbyParent(String name) {
        if (StringUtils.isEmpty(name) || name.trim() == "") {
            throw new GaussException(20001, "Illegal parameter.");
        }
        HobbyParent hobbyParent = new HobbyParent();
        hobbyParent.setName(name);
        int insert = baseMapper.insert(hobbyParent);
        return insert > 0;
    }
    
    @Override
    public List<HobbyParent> getHobbyParentList() {
        List<HobbyParent> hobbyParents = baseMapper.selectList(null);
        return hobbyParents;
    }
    
    @Override
    public List<HobbyParentVo> getHobbyParentListWithChildren() {
        List<HobbyParent> hobbyParentList = this.getHobbyParentList();
        List<HobbyParentVo> hobbyParentVoList = new ArrayList<>();
        for (HobbyParent parent : hobbyParentList) {
            Long parentId = parent.getId();
            
            // 通过一级爱好 ID 查询子爱好
            QueryWrapper<Hobby> hobbyQueryWrapper = new QueryWrapper<>();
            hobbyQueryWrapper.eq("parent_id", parentId);
            List<Hobby> hobbies = hobbyService.getBaseMapper().selectList(hobbyQueryWrapper);
            List<HobbyListVo> hobbyListVos = new ArrayList<>();
            for (Hobby hobby : hobbies) {
                HobbyListVo hobbyListVo = new HobbyListVo();
                BeanUtils.copyProperties(hobby, hobbyListVo);
                hobbyListVo.setText(hobby.getName());
                hobbyListVos.add(hobbyListVo);
            }
            
            HobbyParentVo hobbyParentVo = new HobbyParentVo();
            BeanUtils.copyProperties(parent, hobbyParentVo);
            hobbyParentVo.setText(parent.getName());
            hobbyParentVo.setChildren(hobbyListVos);
            hobbyParentVoList.add(hobbyParentVo);
        }
        return hobbyParentVoList;
    }
}
