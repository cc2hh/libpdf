package com.sclbxx.libpdf.http;

import android.content.Context;
import android.util.Log;

import com.sclbxx.libpdf.util.ACache;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * function: 网络请求相关<p>
 * author: cc <p>
 * data: 2016/12/14 15:42 <p>
 * version: V1.0 <P/>
 */
public class Network {


    //网络URL
    public static String URL = "http://192.168.0.252/zhjy";
    public static String URL_DEFLUT = "http://192.168.0.252/zhjy";
//    public static String URL_DEFLUT = "http://www.istudyway.com.cn/zhjy";
//    public static String URL = "http://www.istudyway.com.cn/zhjy";
    //设缓存有效期为两天
    private static final long CACHE_STALE_SEC = 60 * 60 * 24 * 2;
    private static OkHttpClient mOkHttpClient;
    private static GetAPI api;
    private static Context mContext;

    /**
     * function: 获取网络请求接口实例
     * author: cc
     * data: 2016/4/12 9:35
     * version: V1.0
     */

    public static GetAPI getAPI(Context context) {
        mContext = context;
        return api == null ? api = getInstance().create(GetAPI.class) : api;
    }

    /**
     * function: 重置网络请求接口实例<p>
     * author: cc <p>
     * data: 2016/11/14 16:13 <p>
     * version: V1.0 <p/>
     */
    public static void resetAPI(String url) {
        if (url == null || url.equals("")) {
            return;
        }
        URL = url;
        api = getInstance().create(GetAPI.class);
    }

    private static Retrofit getInstance() {

        initOkHttpClient();
        if (URL == null || URL.equals("")) {
            URL = URL_DEFLUT;
        }

        Retrofit.Builder builder = new Retrofit.Builder();
        builder.client(mOkHttpClient);
        return builder
                .baseUrl(URL + "/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // 配置OkHttpClient
    private static void initOkHttpClient() {
        synchronized (Network.class) {
            // OkHttpClient配置是一样的,静态创建一次即可
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.addInterceptor(mLoggingInterceptor);
            mOkHttpClient = builder
//                    .addNetworkInterceptor(mRewriteCacheControlInterceptor)
                    .connectTimeout(0, TimeUnit.SECONDS)
                    .build();

        }
    }

//    private static Interceptor tokenHeaderInterceptor = chain -> {
//
//        String token = ACache.get(mContext).getAsString("token");
//        if ("".equals(token)) {
//            Request originalRequest = chain.request();
//            return chain.proceed(originalRequest);
//        } else {
//            Request originalRequest = chain.request();
//            Request updateRequest = originalRequest.newBuilder().header("Authorization", token).build();
//            return chain.proceed(updateRequest);
//        }
//
//    };


//    // 云端响应头拦截器，用来配置缓存策略
//    private static Interceptor mRewriteCacheControlInterceptor = chain -> {
//        Request request = chain.request();
//        if (!NetUtil.isConnected(MyApp.getContext())) {
//            request = request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build();
//            KLog.e("no network");
//        }
//        Response originalResponse = chain.proceed(request);
//
//        if (NetUtil.isConnected(MyApp.getContext())) {
//            //有网的时候读接口上的@Headers里的配置，你可以在这里进行统一的设置
//            String cacheControl = request.cacheControl().toString();
//            return originalResponse.newBuilder().header("Cache-Control", cacheControl)
//                    .removeHeader("Pragma").build();
//        } else {
//            return originalResponse.newBuilder()
//                    .header("Cache-Control", "public, only-if-cached," + CACHE_STALE_SEC)
//                    .removeHeader("Pragma").build();
//        }
//    };

    // 打印返回的json数据拦截器
    private static Interceptor mLoggingInterceptor = chain -> {
        Response response = null;

        final Request request = chain.request();

//        KLog.d(request);

        response = chain.proceed(request);

        final ResponseBody responseBody = response.body();
        final long contentLength = responseBody.contentLength();

        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();

        Charset charset = Charset.forName("UTF-8");
        MediaType contentType = responseBody.contentType();
        if (contentType != null) {
            try {
                charset = contentType.charset(charset);
            } catch (UnsupportedCharsetException e) {
                Log.e("Network", "");
                Log.e("Network", "Couldn't decode the response body; charset is likely malformed.");
                return response;
            }
        }
        if (request != null) {
            Log.v("Network", "--------------------------------------------开始打印请求数据----------------------------------------------------");
            Log.v("Network",
                    String.format("Sending request %s on %s%n%s", request.url(), chain.connection(),
                            request.headers()));
            Log.v("Network", "--------------------------------------------结束打印请求数据----------------------------------------------------");
        }
        if (contentLength != 0) {
            Log.v("Network", "--------------------------------------------开始打印返回数据----------------------------------------------------");
            String msg = buffer.clone().readString(charset);
            if (msg != null && msg.contains("远程服务繁忙")) {
                ACache.get(mContext).put("retry", "retry");
            }
            Log.v("Network", msg);
//            KLog.json(buffer.clone().readString(charset));
            Log.v("Network", "--------------------------------------------结束打印返回数据----------------------------------------------------");
        }
        return response;
    };

}
