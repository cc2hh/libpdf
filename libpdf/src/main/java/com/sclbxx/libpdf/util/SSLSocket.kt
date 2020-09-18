package com.sclbxx.libpdf.util

import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.request.okHttpClient
import zlc.season.rxdownload4.request.request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * @title: MyRequest
 * @projectName FamiliesSchoolConnection
 * @description: 实现X509TrustManager接口
 * @author cc
 * @date 2020/9/17 18:02
 */

class MySSLRequest : Request {

    private val httpClient = okHttpClient.newBuilder().sslSocketFactory(createSSLSocketFactory()).build()

    private fun createSSLSocketFactory(): SSLSocketFactory? {
        var ssfFactory: SSLSocketFactory? = null
        try {
            val mMyTrustManager = MyTrustManager()
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(mMyTrustManager), SecureRandom())
            ssfFactory = sc.socketFactory
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }

        return ssfFactory
    }

    override fun get(url: String, headers: Map<String, String>): Flowable<Response<ResponseBody>> =
            request<RequestImpl.Api>(client = httpClient).get(url, headers)
}

class MyTrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) = Unit

}


