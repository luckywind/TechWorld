package com.cxf.batishelper.pojo;


/***
 * @desc rest 请求 返回数据体
 *
 * @author mengtao
 * @date 2019-09-26 17:33
 ***/
public class ResponseData<T> {
    private boolean success;
    private String msg;
    private int status = 0;
    private T data;

    public int getStatus() {
        return status;
    }

    public ResponseData setStatus(int status) {
        this.status = status;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public ResponseData setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public ResponseData setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public T getData() {
        return data;
    }

    public ResponseData setData(T data) {
        this.data = data;
        return this;
    }

}
