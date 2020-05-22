package com.sclbxx.libpdf.pojo;

import java.io.Serializable;

/**
 * auther：vipvbon on 16-9-27 16:19
 * email：664954331@qq.com.com
 */
public class OSSKey implements Serializable{

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }
}
