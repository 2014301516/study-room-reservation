package com.studyroom.util;

import java.util.HashMap;
import java.util.Map;

public class Result {
    private int code;
    private String message;
    private Object data;

    private Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Map<String, Object> ok(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "success");
        map.put("data", data);
        return map;
    }

    public static Map<String, Object> ok(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", message);
        map.put("data", null);
        return map;
    }

    public static Map<String, Object> ok() {
        return ok("success");
    }

    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("data", null);
        return map;
    }

    public static Map<String, Object> error(String message) {
        return error(400, message);
    }

    // getters for serialization
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
