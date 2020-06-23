package com.sclbxx.libpdf.http;

import com.sclbxx.libpdf.pojo.OSSKey;
import com.sclbxx.libpdf.pojo.ToPdf;
import com.sclbxx.libpdf.pojo.Token;

import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

/**
 * function: 请求接口定义<p>
 * author: cc <p>
 * data: 2016/12/15 17:16 <p>
 * version: V1.0 <P/>
 */

public interface GetAPI {
    /**
     * 阿里云日志服务器 OSS key 获取
     */
    @POST("/zhjy/ossremote/getAccessInfoRemote.action")
    Flowable<OSSKey> upLoadLogALiYun(@QueryMap Map<String, Object> paramInt);

    @POST("/zhjy-basedata-customer/serverutil/getUserToken.action")
    Flowable<Token> getToken(@Body RequestBody param);

    @POST("/zhjy-basedata-customer/ossremote/getOSSFileConvert.action")
    Flowable<ToPdf> fileToPdf(@Header("Authorization") String token,@Body RequestBody param);


}
