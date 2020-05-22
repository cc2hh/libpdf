package com.sclbxx.libpdf

import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.KeyEvent
import com.google.gson.Gson
import com.sclbxx.libpdf.base.BaseActivity
import com.sclbxx.libpdf.http.Network
import com.sclbxx.libpdf.pojo.Event
import com.sclbxx.libpdf.pojo.param.ToPdfParam
import com.sclbxx.libpdf.pojo.param.TokenParam
import com.sclbxx.libpdf.util.*
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.libpdf_activity_pdf.*
import okhttp3.MediaType
import okhttp3.RequestBody
import zlc.season.rxdownload4.download
import zlc.season.rxdownload4.file
import zlc.season.rxdownload4.task.Task
import java.io.File
import java.util.*

/**
 *  pdf查看界面
 * @Author cc
 * @Date 2020/5/22 11:43
 * @version 1.0
 */
class PdfActivity : BaseActivity() {

    private val CODE_PERMISSION_READ = 0
    private var disposable: Disposable? = null
    private lateinit var _cache: ACache
    private lateinit var pdfUrl: String
    // 转换pdf失败重试次数
    private var retryIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.libpdf_activity_pdf)
        _cache = ACache.get(this)
        requestPermissions(CODE_PERMISSION_READ, "存储", Manifest.permission.READ_EXTERNAL_STORAGE)

    }

    override fun doPermissions(code: Int) {
        super.doPermissions(code)
        when (code) {
            CODE_PERMISSION_READ -> init()
        }
    }

    /**
     *  初始化
     *
     */
    private fun init() {
        showProgress().setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                hideProgress()
                finish()
            }
            false
        }
        UpData.updateByService(this)
        initData()

    }

    /**
     *  初始化数据
     *
     */
    private fun initData() {
        val isDown = intent.getBooleanExtra(isDown, false)
        val url = intent.getStringExtra(mUrl)
        val savePath = intent.getStringExtra(savePath)
        val file = File(savePath + File(url).nameWithoutExtension + ".pdf")

        // 如果文件已存在
        if (!isDown && file.exists()) {
            hideProgress()
            libpdf_main_pdf.fromFile(file).load()
            return
        }

        if (!url.startsWith("http") && !File(url).exists()) {
            toast("文件不存在")
            finish()
            return
        }

        RxBusNew.getInstance().toObservableSticky(Event::class.java)
                // 检测文件类型
                .filter {
                    val extension = File(url).extension
                    if (FileUtil.checkType(extension)) {
                        true
                    } else {
                        hideProgress()
                        toast("$extension 为不支持的文件类型")
                        finish()
                        false
                    }
                }
                .filter {
                    if (it.boolean)
                        true
                    else {
                        hideProgress()
                        AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("尝试重连管家服务")
                                .setPositiveButton("确定") { v, _ ->
                                    init()
                                    v.dismiss()
                                }
                                .setNegativeButton("退出") { v, _ ->
                                    v.dismiss()
                                    finish()
                                }
                                .show()
                        false
                    }
                }
                .observeOn(Schedulers.io())
                .map {
                    val login = XMLUtils.readBaseInfo()
                    _cache.put("userId", login.studentId)
                    _cache.put("schoolId", login.schoolId)
                    _cache.put("url", login.url)
                    _cache.put("userAccount", login.userAccount)
                    _cache.put("userPwd", login.userPwd)
                }
                .flatMap {
                    if (url.startsWith("http")) {
                        Observable.just(url)
                    } else {
                        OSSPutObject.getInstance(this).connOssKey()
                                .map { oss -> oss.putObjectFromLocalFile(url) }
                    }
                }
                .toFlowable(BackpressureStrategy.BUFFER)
                .flatMap {
                    pdfUrl = it
                    // token 每天头次访问时更新
                    val upTimeToken = _cache.getAsString("upTimeToken") ?: ""
                    var token = _cache.getAsString("token") ?: ""
                    if (DateUtil.date2Str(Date(), DateUtil.FORMAT_YMD) > upTimeToken) {

                        Network.URL = _cache.getAsString("url").replace("/zhjy", "")
                        val param = TokenParam()
                        param.accountName = _cache.getAsString("userAccount")
                        param.password = UpData.updateService
                                .decryptAndEncrypt(_cache.getAsString("userAccount"),
                                        _cache.getAsString("userPwd"))
                        val body = RequestBody.create(MediaType.parse(
                                "application/json; charset=utf-8"), Gson().toJson(param))
                        Network.getAPI(this).getToken(body)
                                .filter { item ->
                                    if (item.success == 1) {
                                        true
                                    } else {
                                        toast("token异常:${item.error}")
                                        finish()
                                        false
                                    }
                                }
                                .map { item ->
                                    // 缓存token
                                    token = item.data.token
                                    _cache.put("token", token)
                                    _cache.put("upTimeToken", DateUtil.date2Str(Date(), DateUtil.FORMAT_YMD))
                                    token
                                }
                    } else {
                        Flowable.just(token)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe({ toPdf(it) }, {
                    toast(it.toString())
                    hideProgress()
                    finish()
                })
    }


    private fun toPdf(token: String) {

        val param = ToPdfParam()
        param.ossfileUrl = pdfUrl
        val body = RequestBody.create(MediaType.parse(
                "application/json; charset=utf-8"), Gson().toJson(param))
        Network.getAPI(this).fileToPdf(token, body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe({
                    if (it.success == 1) {
                        downloadFile(it.data.pdfUrl)
                    } else if (it.error.contains("文档转化") && retryIndex < 3) {
                        retryIndex++
                        toPdf(_cache.getAsString("token"))
                    } else {
                        hideProgress()
                        toast("转换异常:$it.error")
                        finish()
                    }
                }, {
                    val retry = _cache.getAsString("retry") ?: ""
                    if (it.toString().contains("HTTP 500") && retryIndex < 3 && "" != retry) {
                        retryIndex++
                        Log.e("retryIndex:", "$retryIndex")
                        toPdf(_cache.getAsString("token"))
                    } else {
                        toast(it.toString())
                        hideProgress()
                        finish()
                    }
                })
    }

    /**
     *  下载文件
     *
     */
    private fun downloadFile(pdfUrl: String) {
        val savePath = intent.getStringExtra(savePath)
        val url = intent.getStringExtra(mUrl)
        val saveName = "${File(url).nameWithoutExtension}.${File(pdfUrl).extension}"
        disposable = Task(url = pdfUrl, saveName = saveName, savePath = savePath).download()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                }, onComplete = {
                    hideProgress()
                    libpdf_main_pdf.fromFile(pdfUrl.file()).load()
                    if (!url.startsWith("http")) {
                        FileUtil.deleteFile(url)
                    }
                }, onError = {
                    toast("下载失败:$it")
                    hideProgress()
                    finish()
                })
    }


    override fun onDestroy() {
        super.onDestroy()
        disposable?.apply { if (isDisposed) dispose() }
        UpData.destroy(this)
        RxBusNew.getInstance().reset()
    }

    companion object {
        // 源文件url或路径
        private const val mUrl = "pdfUrl"
        // 源文件需保存地址
        private const val savePath = "savePath"
        // 强制下载
        private const val isDown = "isDown"

        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径
         * @param down 强制下载文件
         */
        fun start(ctx: Context, url: String, path: String? = null, down: Boolean = false) {
            val intent = Intent(ctx, PdfActivity::class.java)
            intent.putExtra(mUrl, url)
            intent.putExtra(savePath, path ?: FileUtil.getDirPath() + "/pdf/")
            intent.putExtra(isDown, down)
            ctx.startActivity(intent)
        }

    }
}
