package com.cxf.mp.util;

public class ResultUtil {

    public static Result success(Object object) {
        Result result = new Result();
        result.setCode("1");
        result.setMsg("success");
        result.setData(object);
        return result;
    }

    public static Result success() {
        return success(null);
    }

    public static Result error(String code, String msg) {
        Result result = new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
    public static Result error(String msg) {
        Result result = new Result();
        result.setMsg(msg);
        return result;
    }
}
