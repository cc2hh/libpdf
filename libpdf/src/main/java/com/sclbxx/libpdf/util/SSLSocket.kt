package com.sclbxx.libpdf.util

import android.annotation.TargetApi
import android.os.Build
import com.sclbxx.libpdf.PdfActivity
import io.reactivex.Flowable
import okhttp3.ResponseBody
import retrofit2.Response
import zlc.season.rxdownload4.request.Request
import zlc.season.rxdownload4.request.RequestImpl
import zlc.season.rxdownload4.request.okHttpClient
import zlc.season.rxdownload4.request.request
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * @title: MyRequest
 * @projectName FamiliesSchoolConnection
 * @description: 实现X509TrustManager接口
 * @author cc
 * @date 2020/9/17 18:02
 */

class MySSLRequest : Request {

    private val mMyTrustManager = if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            MyTrustManager1() else MyTrustManager()

    private val httpClient = okHttpClient.newBuilder()
            .sslSocketFactory(createSSLSocketFactory(),mMyTrustManager)
            .build()

    private fun createSSLSocketFactory(): SSLSocketFactory {
        var ssfFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(mMyTrustManager), SecureRandom())
            ssfFactory = sc.socketFactory
        } catch (ignored: Exception) {
            ignored.printStackTrace()
            throw ignored
        } finally {
            return  ssfFactory
        }
    }

    override fun get(url: String, headers: Map<String, String>): Flowable<Response<ResponseBody>> =
            request<RequestImpl.Api>(client = httpClient).get(url, headers)
}

class MyTrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) = Unit
    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) = Unit

}


@TargetApi(Build.VERSION_CODES.N)
class MyTrustManager1 : X509ExtendedTrustManager() {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {}

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {}

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {}

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {}

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }

}


