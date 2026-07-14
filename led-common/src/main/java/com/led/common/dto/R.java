package com.led.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** 统一响应结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    public static R<Void> okMsg(String message) {
        return new R<>(200, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }
}
