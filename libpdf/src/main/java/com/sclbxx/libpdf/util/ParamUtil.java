package com.sclbxx.libpdf.util;


import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * @author cc
 * @title: ParamUtil
 * @projectName trunk
 * @description: 接口参数工具类
 * @date 2021/3/3 9:32
 */
public class ParamUtil {

    public static final RequestBody getJsonParam(String json) {
        return RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
    }
}
