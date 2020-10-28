package com.lamdaer.opengauss.gauss.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lamdaer.opengauss.gauss.common.GaussException;
import com.lamdaer.opengauss.gauss.entity.Hobby;
import com.lamdaer.opengauss.gauss.entity.UserInfo;
import com.lamdaer.opengauss.gauss.service.HobbyService;
import com.lamdaer.opengauss.gauss.service.SimilarityService;
import com.lamdaer.opengauss.gauss.service.UserInfoService;
import com.lamdaer.opengauss.gauss.utils.MapUtil;

/**
 * @author lamdaer
 * @createTime 2020/10/24
 */
@Service
public class SimilarityServiceImpl implements SimilarityService {
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private HobbyService hobbyService;
    
    
    @Override
    public List<UserInfo> similarity(Long userId) {
        if (userId == null) {
            throw new GaussException(20001, "Illegal parameter.");
        }
        UserInfo userInfo = userInfoService.getById(userId);
        Long jobId = userInfo.getJobId();
        Integer age = userInfo.getAge();
        Integer sex = userInfo.getSex();
        List<Hobby> hobbyList = hobbyService.getHobbyList();
        List<UserInfo> userInfoList = userInfoService.getBaseMapper().selectList(null);
        Map<Long, Integer> result = new HashMap<>(userInfoList.size());
        for (UserInfo user : userInfoList) {
            if (user.getUserId().equals(userId)) {
                continue;
            }
            int count = 0;
            
            // 岗位相似度
            if (user.getJobId().equals(jobId)) {
                count += 4;
            }
            
            // 性别
            if (!user.getSex().equals(sex)){
                count += 10;
            }
            
            // 年龄相似度
            int differenceAge = Math.abs(age - user.getAge());
            switch (differenceAge) {
                case 0:
                    count += 5;
                    break;
                case 1:
                case 2:
                case 3:
                    count += 4;
                    break;
                case 4:
                case 5:
                case 6:
                    count += 3;
                    break;
                case 7:
                case 8:
                case 9:
                    count += 2;
                    break;
                default:
                    count += 1;
            }
            
            // 爱好相似度
            String[] split = user.getHobbyIdList().split(",");
            List<Long> hobbies = Arrays.stream(split).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
            if (user.getJobId().equals(jobId)) {
                count += 4;
            }
            Set<Long> same = new HashSet<>();
            Set<Long> temp = new HashSet<>();
            for (int i = 0; i < hobbyList.size(); i++) {
                temp.add(hobbyList.get(i).getId());
            }
            for (int j = 0; j < hobbies.size(); j++) {
                if (!temp.add(hobbies.get(j))) {
                    same.add(hobbies.get(j));
                }
            }
            count += same.size() * 5;
            
            result.put(user.getUserId(), count);
            
        }
        Map<Long, Integer> longIntegerMap = MapUtil.sortByValueDesc(result);
        Set<Long> userIds = longIntegerMap.keySet();
        List<Long> userIdList = new ArrayList<>(userIds);
        List<UserInfo> userInfos = new ArrayList<>();
        for (Long id : userIdList.subList(0, 3)) {
            UserInfo user = userInfoService.getById(id);
            userInfos.add(user);
        }
        return userInfos;
    }
    
    
}
