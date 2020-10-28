package com.lamdaer.opengauss.gauss.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lamdaer.opengauss.gauss.common.GaussException;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.Job;
import com.lamdaer.opengauss.gauss.entity.UserInfo;
import com.lamdaer.opengauss.gauss.entity.vo.UserInfoVo;
import com.lamdaer.opengauss.gauss.mapper.UserInfoMapper;
import com.lamdaer.opengauss.gauss.service.HobbyService;
import com.lamdaer.opengauss.gauss.service.JobService;
import com.lamdaer.opengauss.gauss.service.UserInfoService;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    @Autowired
    private HobbyService hobbyService;
    
    @Autowired
    private JobService jobService;
    
    @Override
    public Long add(UserInfoVo userInfoVo) {
        Long jobId = userInfoVo.getJobId();
        List<Long> hobbyIdList = userInfoVo.getHobbyIdList();
        
        // 检查岗位是否存在
        Job job = jobService.getById(jobId);
        if (job == null) {
            throw new GaussException(20001, "job is not exist.");
        }
        
        // 检查爱好是否存在
        for (Long hobbyId : hobbyIdList) {
            Hobby hobby = hobbyService.getById(hobbyId);
            if (hobby == null) {
                throw new GaussException(20001, "hobby is not exist.");
            }
        }
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(userInfoVo, userInfo);
        StringBuilder stringBuilder = new StringBuilder();
        
        // 爱好id列表转换为字符串 格式如：[11111,22222]
        for (int i = 0; i < hobbyIdList.size(); i++) {
            stringBuilder.append(hobbyIdList.get(i));
            if (i != hobbyIdList.size() - 1) {
                stringBuilder.append(",");
            }
        }
        String str = stringBuilder.toString();
        userInfo.setHobbyIdList(str);
        int insert = baseMapper.insert(userInfo);
        if (insert > 0) {
            return userInfo.getUserId();
        }
        return null;
    }
}
