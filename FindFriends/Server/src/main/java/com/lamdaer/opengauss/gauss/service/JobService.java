package com.lamdaer.opengauss.gauss.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lamdaer.opengauss.gauss.entity.Job;

/**
 * <p>
 * 岗位 服务类
 * </p>
 * @author Lamdaer
 * @since 2020-10-24
 */
public interface JobService extends IService<Job> {
    
    /**
     * 添加岗位
     * @param name 岗位名称
     * @return
     */
    Boolean addJob(String name);
    
    /**
     * 获取岗位列表
     * @return
     */
    List<Job> getJobList();
}
