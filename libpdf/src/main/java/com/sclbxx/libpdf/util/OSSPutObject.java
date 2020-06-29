package com.sclbxx.libpdf.util;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.common.utils.BinaryUtil;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.AppendObjectRequest;
import com.alibaba.sdk.android.oss.model.AppendObjectResult;
import com.alibaba.sdk.android.oss.model.DeleteObjectRequest;
import com.alibaba.sdk.android.oss.model.DeleteObjectResult;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.sclbxx.libpdf.base.Constant;
import com.sclbxx.libpdf.http.Network;
import com.sclbxx.libpdf.pojo.OSSKey;
import com.tencent.mmkv.MMKV;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import io.reactivex.Flowable;
import io.reactivex.Observable;

/**
 * auther：vipvbon on 16-9-27 16:19
 * email：664954331@qq.com.com
 */
public class OSSPutObject {
    private final static String TAG = "OSSPutObject";

    private static String endpoint = "http://oss-cn-zhangjiakou.aliyuncs.com";
    private static String accessKeyId = null;
    private static String accessKeySecret = null;
    private static String securityToken = null;
    private static String testBucket = "istudyway-huabei3";
    private static String key;
    private String uploadFilePath;
    private Context context;
    private static OSS oss = null;
    private static OSSPutObject ossPutObject = null;

    public static OSSPutObject getInstance(Context context) {
        if (ossPutObject == null) {
            ossPutObject = new OSSPutObject(context);
        }
        return ossPutObject;
    }

    private OSSPutObject(Context context) {
        this.context = context;
//        connOssKey();
//        connOssKey(path);
    }

    /**
     * 从本地文件上传，采用阻塞的同步接口
     *
     * @return
     */
    public String putObjectFromLocalFile(String uploadFilePath) throws ClientException, ServiceException{
        this.uploadFilePath = uploadFilePath;
//        key = uploadFilePath.substring(uploadFilePath.indexOf("workspace"), uploadFilePath.length())
//                .replaceAll("\\" + File.separator, "/");

        String uuid = "android_" + UUID.randomUUID().toString() + uploadFilePath.substring(uploadFilePath.lastIndexOf("."));
        key = "file" + File.separator + "pdf" + File.separator +
                DateUtil.date2Str(new Date(), DateUtil.FORMAT_YMD) + File.separator + uuid;

//        try {
            // 构造上传请求
            PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadFilePath);
            PutObjectResult putResult = oss.putObject(put);
            return "http://" + testBucket + "." + endpoint.replace("http://", "") + "/" + key;
//        } catch (ClientException e) {
//            // 本地异常如网络异常等
//            return e.getClass().getName();
//        } catch (ServiceException e) {
//            // 服务异常
//            Log.e("RequestId", e.getRequestId());
//            Log.e("ErrorCode", e.getErrorCode());
//            Log.e("HostId", e.getHostId());
//            Log.e("RawMessage", e.getRawMessage());
//            return e.getClass().getName();
//        } catch (Exception e) {
//            securityToken = null;
////            connOssKey();
//            return e.getClass().getName();
//        }
    }

    // 从本地文件上传，使用非阻塞的异步接口
    public void asyncPutObjectFromLocalFile(String uploadFilePath) {
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadFilePath);

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.e("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.e("PutObject", "UploadSuccess");
                Log.e("ETag", result.getETag());
                Log.e("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    // 直接上传二进制数据，使用阻塞的同步接口
    public void putObjectFromByteArray() {
        // 构造测试的上传数据
        byte[] uploadData = new byte[100 * 1024];
        new Random().nextBytes(uploadData);

        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadData);

        try {
            PutObjectResult putResult = oss.putObject(put);

            Log.d("PutObject", "UploadSuccess");

            Log.d("ETag", putResult.getETag());
            Log.d("RequestId", putResult.getRequestId());
        } catch (ClientException e) {
            // 本地异常如网络异常等
            e.printStackTrace();
        } catch (ServiceException e) {
            // 服务异常
            Log.e("RequestId", e.getRequestId());
            Log.e("ErrorCode", e.getErrorCode());
            Log.e("HostId", e.getHostId());
            Log.e("RawMessage", e.getRawMessage());
        }
    }

    // 上传时设置ContentType等，也可以添加自定义meta信息
    public void putObjectWithMetadataSetting() {
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadFilePath);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        metadata.addUserMetadata("x-oss-meta-name1", "value1");

        put.setMetadata(metadata);

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");

                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    // 上传文件可以设置server回调
    public void asyncPutObjectWithServerCallback() {
        // 构造上传请求
        final PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadFilePath);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");

        put.setMetadata(metadata);

        put.setCallbackParam(new HashMap<String, String>() {
            {
                put("callbackUrl", "110.75.82.106/mbaas/callback");
                put("callbackBody", "test");
            }
        });

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");

                // 只有设置了servercallback，这个值才有数据
                String serverCallbackReturnJson = result.getServerCallbackReturnBody();

                Log.d("servercallback", serverCallbackReturnJson);
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    public void asyncPutObjectWithMD5Verify() {
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(testBucket, key, uploadFilePath);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        try {
            // 设置Md5以便校验
            metadata.setContentMD5(BinaryUtil.calculateBase64Md5(uploadFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        put.setMetadata(metadata);

        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");

                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    // 追加文件
    public void appendObject() {
        // 如果bucket中objectKey存在，将其删除
        try {
            DeleteObjectRequest delete = new DeleteObjectRequest(testBucket, key);
            DeleteObjectResult result = oss.deleteObject(delete);
        } catch (ClientException clientException) {
            clientException.printStackTrace();
        } catch (ServiceException serviceException) {
            Log.e("ErrorCode", serviceException.getErrorCode());
            Log.e("RequestId", serviceException.getRequestId());
            Log.e("HostId", serviceException.getHostId());
            Log.e("RawMessage", serviceException.getRawMessage());
        }
        AppendObjectRequest append = new AppendObjectRequest(testBucket, key, uploadFilePath);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("application/octet-stream");
        append.setMetadata(metadata);

        // 设置追加位置，只能从文件末尾开始追加，如果是新文件，从0开始
        append.setPosition(0);

        append.setProgressCallback(new OSSProgressCallback<AppendObjectRequest>() {
            @Override
            public void onProgress(AppendObjectRequest request, long currentSize, long totalSize) {
                Log.d("AppendObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });

        OSSAsyncTask task = oss.asyncAppendObject(append, new OSSCompletedCallback<AppendObjectRequest, AppendObjectResult>() {
            @Override
            public void onSuccess(AppendObjectRequest request, AppendObjectResult result) {
                Log.d("AppendObject", "AppendSuccess");
                Log.d("NextPosition", "" + result.getNextPosition());
            }

            @Override
            public void onFailure(AppendObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    /**
     * OSS key 获取
     */
    public Flowable<OSSPutObject> connOssKey() {
        Map<String, Object> params = new HashMap();
        MMKV kv = MMKV.defaultMMKV();
        params.put("id", kv.decodeString(Constant.KEY_USERID));
        params.put("fkSchoolId", kv.decodeString(Constant.KEY_SCHOOLID));
        return Network.getAPI(context).upLoadLogALiYun(params)
                .map(data -> doResult(data));
    }

    private OSSPutObject doResult(OSSKey ossKey) {
        try {
            if (ossKey != null) {
                accessKeyId = ossKey.getAccessKeyId();
                accessKeySecret = ossKey.getAccessKeySecret();
                securityToken = ossKey.getSecurityToken();

                ClientConfiguration conf = new ClientConfiguration();
                conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
                conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
                conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
                conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
                OSSCredentialProvider credentialProvider =
                        new OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret, securityToken);

                oss = new OSSClient(context, endpoint, credentialProvider, conf);
            } else {
                Log.e(TAG, "获取OSS Key 为空");
            }
        } catch (Exception e) {
            Log.e(TAG, "服务器异常,获取OSS Key失败", e);
        } finally {
            return this;
        }

    }
}
