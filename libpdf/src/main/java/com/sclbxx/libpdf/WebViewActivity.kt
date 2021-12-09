package com.sclbxx.libpdf

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import androidx.appcompat.app.AlertDialog
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sclbxx.libpdf.base.BaseActivity
import com.sclbxx.libpdf.base.Constant
import kotlinx.android.synthetic.main.activity_web_view_pdf.*

class WebViewActivity : BaseActivity() {


    private var windowWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view_pdf)

        showProgress()

        libpdf_webview_toolbar.setNavigationOnClickListener { onBackPressed() }
        val webSettings = libpdf_webview_wv.settings
        webSettings.javaScriptEnabled = true

        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.setSupportMultipleWindows(true)

        //设置可以访问文件
        webSettings.allowFileAccess = false
        //屏幕大小自适应
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true
        //设置支持缩放
        webSettings.builtInZoomControls = false
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        libpdf_webview_wv.webViewClient = WebViewClient()

        libpdf_webview_wv.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)

                // 处理网页访问失败
                if (title?.equals(mUrl) == true) {
                    runOnUiThread {
                        AlertDialog.Builder(this@WebViewActivity)
                                .setTitle("ppt在线预览失败切换到本地模式")
                                .setMessage("访问地址：$title")
                                .setPositiveButton("切换") { v, _ ->
                                    v.dismiss()
                                    code = Activity.RESULT_CANCELED
                                    onBackPressed()
                                }
                                .setNegativeButton("取消") { v, _ ->
                                    v.dismiss()
                                    code = Activity.RESULT_OK
                                    onBackPressed()
                                }
                                .setCancelable(false)
                                .show()
                    }
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 100) {
                    hideProgress()
                }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                handleCreateWebWindowRequest(resultMsg)
                return true
            }
        }
        libpdf_webview_wv.loadUrl(mUrl)

//        Log.d("WebView", mUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun handleCreateWebWindowRequest(resultMsg: Message?) {
        if (resultMsg == null) return
        if (resultMsg.obj != null && resultMsg.obj is WebView.WebViewTransport) {
            showProgress()
            val transport = resultMsg.obj as WebView.WebViewTransport
            windowWebView = WebView(this)
            windowWebView?.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)

            val settings = windowWebView?.settings

            settings?.javaScriptEnabled = true
            settings?.javaScriptCanOpenWindowsAutomatically = true
            settings?.setSupportMultipleWindows(true)

            windowWebView?.webViewClient = WebViewClient()

            windowWebView?.webChromeClient = object : WebChromeClient() {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (newProgress >= 100) {
                        hideProgress()
                    }
                }


                override fun onCloseWindow(window: WebView?) {
                    super.onCloseWindow(window)
                    handleCloseWebWindowRequest()
                }
            }

            libpdf_webview_rl.addView(windowWebView)
//            wv.visibility = View.GONE
            transport.webView = windowWebView
            resultMsg.sendToTarget()
        }
    }


    private fun handleCloseWebWindowRequest() {
        if (windowWebView == null) return

        libpdf_webview_rl.removeView(windowWebView)
//        wv.visibility = View.VISIBLE
        windowWebView = null
    }

    override fun onBackPressed() {
        if (windowWebView == null) {
            setResult(code)
            finish()
        } else {
            handleCloseWebWindowRequest()
        }

    }

    companion object {

        // 源文件url或路径
        private lateinit var mUrl: String
        private var code: Int = Activity.RESULT_OK

        fun start(ctx: Context, url: String) {
            val intent = Intent(ctx, WebViewActivity::class.java)
            mUrl = url
            (ctx as Activity).startActivityForResult(intent, Constant.REQUEST_CODE_ZERO)
        }
    }
}
