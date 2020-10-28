package com.lamdaer.opengauss.gauss.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lamdaer.opengauss.gauss.common.GaussException;
import com.lamdaer.opengauss.gauss.entity.Job;
import com.lamdaer.opengauss.gauss.mapper.JobMapper;
import com.lamdaer.opengauss.gauss.service.JobService;

/**
 * <p>
 * 岗位 服务实现类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
@Service
public class JobServiceImpl extends ServiceImpl<JobMapper, Job> implements JobService {
    @Override
    public Boolean addJob(String name) {
        if (StringUtils.isEmpty(name) || name.trim() == "") {
            throw new GaussException(20001, "Illegal parameter.");
        }
        Job job = new Job();
        job.setName(name);
        int insert = baseMapper.insert(job);
        return insert > 0;
    }
    
    @Override
    public List<Job> getJobList() {
        List<Job> jobList = baseMapper.selectList(null);
        return jobList;
    }
}
