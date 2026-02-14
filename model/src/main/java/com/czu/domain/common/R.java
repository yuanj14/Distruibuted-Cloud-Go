package com.czu.domain.common;

import lombok.Data;

@Data
public class R<T> {
    private Integer code;
    private String msg;
    private Object data;

    public static R ok(){
        R res = new R();
        res.setCode(200);
        return res;
    }


    public static R ok(String msg, Object data){
        R res = new R();
        res.setCode(200);
        res.setMsg(msg);
        res.setData(data);
        return res;
    }

    public static R error(){
        R res = new R();
        res.setCode(500);
        return res;
    }

    public static R error(Integer code, String msg){
        R res = new R();
        res.setCode(code);
        res.setMsg(msg);
        return res;
    }
}
