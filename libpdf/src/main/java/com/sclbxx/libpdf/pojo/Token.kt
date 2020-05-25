package com.sclbxx.libpdf.pojo

import com.google.gson.annotations.SerializedName


/**
 *
 * 获取服务器端的Token值
 * @Author cc
 * @Date 2019/10/18-17:18
 * @version 1.0
 */
data class Token(
        @SerializedName("data")
        val `data`: Data,
        @SerializedName("error")
        val error: String,
        @SerializedName("success")
        val success: Int
) {

    data class Data(
            @SerializedName("imageUrl")
            val imageUrl: String,
            @SerializedName("islock")
            val islock: Int,
            @SerializedName("password")
            val password: String,
            @SerializedName("schoolId")
            val schoolId: Int,
            @SerializedName("schoolName")
            val schoolName: String,
            @SerializedName("schoolUniqueId")
            val schoolUniqueId: String,
            @SerializedName("token")
            val token: String,
            @SerializedName("userId")
            val userId: String,
            @SerializedName("userName")
            val userName: String,
            @SerializedName("userType")
            val userType: Int,
            @SerializedName("activeTime")
            val activeTime: Int

    )
}
