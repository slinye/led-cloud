package com.led.common.dto;

import lombok.Data;

/** 分页查询请求 */
@Data
public class PageRequest {
    private int page = 1;
    private int size = 20;
    private String keyword;
}
