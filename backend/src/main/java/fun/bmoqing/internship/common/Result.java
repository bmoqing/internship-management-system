/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

package fun.bmoqing.internship.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 200成功，其他为失败
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(400);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> validationError(String message) {
        Result<T> result = new Result<>();
        result.setCode(422);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> serverError(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> notFound(String message) {
        Result<T> result = new Result<>();
        result.setCode(404);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> conflict(String message) {
        Result<T> result = new Result<>();
        result.setCode(409);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> unauthorized(String message) {
        Result<T> result = new Result<>();
        result.setCode(401);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> forbidden(String message) {
        Result<T> result = new Result<>();
        result.setCode(403);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> tooManyRequests(String message) {
        Result<T> result = new Result<>();
        result.setCode(429);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> methodNotAllowed(String message) {
        Result<T> result = new Result<>();
        result.setCode(405);
        result.setMessage(message);
        return result;
    }
}
