package com.szgx.kunpeng.screen.service.impl;

import com.szgx.kunpeng.basic.common.PrometheusApiConstant;
import com.szgx.kunpeng.screen.service.PrometheusService;
import com.szgx.kunpeng.basic.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author zhangrb
 * @date 2022/8/11 15:08
 */
@Service
public class PrometheusServiceImpl implements PrometheusService {

    @Value("${screen.baseurl}")
    private String baseUrl;

    @Override
    public Object systemData(Integer type, String startTime, String step) {
        return HttpClientUtil.doGet(baseUrl + getSystemUri(type,startTime,step));
    }

    @Override
    public Object gaussData(Integer type, String startTime, String step) {
        return HttpClientUtil.doGet(baseUrl + getGaussUri(type,startTime,step));
    }


    private String getGaussUri(Integer type, String startTime, String step) {
        StringBuilder uri = new StringBuilder();
        Long end = System.currentTimeMillis() / 1000;
        Long start = startTime == null? (end - 3600) : Long.valueOf(startTime);
        switch (type) {
            case 1:
                uri.append(PrometheusApiConstant.API_GS_CONNECT_URL);
                break;
            case 2:
                uri.append(PrometheusApiConstant.API_GS_DISK_URL);
                break;
            case 3:
                uri.append(PrometheusApiConstant.API_GS_LOCK_URL);
                break;
            case 4:
                uri.append(PrometheusApiConstant.API_GS_DEADLOCK_URL);
                break;
        }
        return uri.append("&start=").append(start).append("&end=").append(end).append("&step=").append(step == null? 14 : step).toString();
    }

    /**
     * 设置开始时间、结束时间
     *
     * @param type
     * @return
     */
    private String getSystemUri(Integer type, String startTime, String step) {

        StringBuilder uri = new StringBuilder();
        Long end = System.currentTimeMillis() / 1000;
        Long start = startTime == null? (end - 3600) : Long.valueOf(startTime);
        switch (type) {
            case 1:
                uri.append(PrometheusApiConstant.API_CPU_URL);
                break;
            case 2:
                uri.append(PrometheusApiConstant.API_RAM_URL);
                break;
            case 3:
                uri.append(PrometheusApiConstant.API_DISK_URL);
                break;
            case 4:
                uri.append(PrometheusApiConstant.API_DISK_R_W_IO_URL);
                break;
            case 5:
                uri.append(PrometheusApiConstant.API_DISK_R_URL);
                break;
            case 6:
                uri.append(PrometheusApiConstant.API_DISK_W_URL);
                break;
            case 7:
                uri.append(PrometheusApiConstant.API_DK_UP_URL);
                break;
            case 8:
                uri.append(PrometheusApiConstant.API_DK_DOWN_URL);
                break;
        }
        return uri.append("&start=").append(start).append("&end=").append(end).append("&step=").append(step == null? 14 : step).toString();
    }
}
