package com.lamdaer.opengauss.gauss.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lamdaer
 * @createTime 2020/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GaussException extends RuntimeException {
    private Integer code;
    private String message;
}
