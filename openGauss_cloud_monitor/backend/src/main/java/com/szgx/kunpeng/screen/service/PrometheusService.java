package com.szgx.kunpeng.screen.service;

/**
 * @author zhangrb
 * @date 2022/8/11 15:07
 */

public interface PrometheusService {

    Object systemData(Integer type, String startTime, String step);

    Object gaussData(Integer type, String startTime, String step);
}
