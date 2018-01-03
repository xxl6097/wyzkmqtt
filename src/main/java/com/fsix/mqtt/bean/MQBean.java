package com.fsix.mqtt.bean;

import java.io.Serializable;

public class MQBean<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public MQBean() {
    }

    public MQBean(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public MQBean(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public MQBean(T data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MQBean{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
