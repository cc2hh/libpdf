package com.sclbxx.libpdf.pojo

import com.google.gson.annotations.SerializedName


/**

 * @Author cc
 * @Date 2020/5/21-10:30
 * @version 1.0
 */
data class ToPdf(
        @SerializedName("data")
        val `data`: Data,
        @SerializedName("error")
        val error: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("success")
        val success: Int
) {
    data class Data(
            @SerializedName("pdfUrl")
            val pdfUrl: String
    )
}
