package com.sclbxx.libpdf.pojo;

/**
 * 服务器端的Token值
 *
 * @version 1.0
 * @Author cc
 * @Date 2020/3/18-1426
 */
public class Token {
    public Data data;
    public String error;
    public int success;

    public  class Data {
        public String imageUrl;
        public int islock;
        public String password;
        public int schoolId;
        public String schoolName;
        public String schoolUniqueId;
        public String token;
        public String userId;
        public String userName;
        public int userType;
    }
}
