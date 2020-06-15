package com.sclbxx.libpdf

import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.KeyEvent
import com.github.barteksc.pdfviewer.util.Util
import com.google.gson.Gson
import com.sclbxx.libpdf.base.BaseActivity
import com.sclbxx.libpdf.base.Constant
import com.sclbxx.libpdf.http.Network
import com.sclbxx.libpdf.pojo.Event
import com.sclbxx.libpdf.pojo.param.ToPdfParam
import com.sclbxx.libpdf.pojo.param.TokenParam
import com.sclbxx.libpdf.scrollhandle.WpsScrollHandle
import com.sclbxx.libpdf.util.*
import com.tencent.mmkv.MMKV
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

    private lateinit var kv: MMKV
    private var disposable: Disposable? = null
    private var disPdf: Disposable? = null
    private var disRx: Disposable? = null
    private lateinit var pdfUrl: String
    // 转换pdf失败重试次数
    private var retryIndex = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.libpdf_activity_pdf)
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
        MMKV.initialize(this)
        kv = MMKV.defaultMMKV()

        showProgress().setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
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

        // 本地文件不存在
        val file = File(mUrl)
        if (!mUrl.startsWith("http") && !file.exists()) {
            toast("文件不存在")
            finish()
            return
        }

        // 初始化重试次数
        retryIndex = 0

        val tempUel: String? = kv.decodeString(mUrl) ?: ""
        when {
            // 不管服务文件有没有，先拼凑链接地址尝试直接下载文件
            // 源文件就是网络文件
            mUrl.startsWith("http") -> tryDown(mUrl, file.nameWithoutExtension)
            // 源文件已有缓存阿里云地址
            TextUtils.isEmpty(tempUel) -> {
                tryDown(tempUel!!, File(tempUel).nameWithoutExtension)
            }
            else -> initRx()
        }
    }

    /**
     *  尝试下载pdf
     * @Author cc
     * @Date 2020/5/27 18:18
     * @version 1.0
     */
    private fun tryDown(url: String, extension: String) {
        val temp = "${url.substring(0, url.lastIndexOf("/") + 1)}$extension.pdf"
        downloadFile(temp, true)
    }

    private fun initRx() {

        disRx?.apply { if (!isDisposed) dispose() }

        // 转换后的文件
        val file = File("$savePath/$saveName.pdf")

        disRx = RxBusNew.getInstance().toObservableSticky(Event::class.java)
                // 检测文件类型
                .filter {
                    val extension = File(mUrl).parent
                    if (FileUtil.checkType(extension)) {
                        true
                    } else {
                        toast("$extension 为不支持的文件类型")
                        finish()
                        false
                    }
                }
                .filter {
                    // 如果保存文件已存在
                    if (file.exists()) {
                        // 强制重新下载，并且源文件为网络文件
                        if (isDown && mUrl.startsWith("http")) {
                            // 源文件是pdf直接下载，并删除本地pdf文件
                            // 源文件是其他类型走正常转换流程
                            if (mUrl.endsWith(".pdf")) {
                                FileUtil.deleteFile(file.absolutePath)
                                downloadFile(mUrl, false)
                                return@filter false
                            }
                        } else {
                            //保存文件已存在且没要求强制重新下载，直接加载pdf
                            loadPdf(file)
                            return@filter false
                        }
                    } else if (mUrl.endsWith(".pdf") && mUrl.startsWith("http")) {
                        // 保存文件不存在
                        // 如果源文件就是pdf且是网络文件，直接下载
                        downloadFile(mUrl, false)
                        return@filter false
                    }
                    true
                }
                .filter {
                    // 判断源文件是否已上传到阿里云
                    pdfUrl = kv.decodeString(mUrl) ?: ""
                    val token = kv.decodeString(Constant.KEY_TOKEN) ?: ""
                    // 源文件已有缓存阿里云地址，直接尝试转换
                    if (TextUtils.isEmpty(pdfUrl)&&TextUtils.isEmpty(token)) {
                        toPdf(token)
                        return@filter false
                    }
                    true
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
                                .setCancelable(false)
                                .show()
                        false
                    }
                }
                .observeOn(Schedulers.io())
                .map {
                    val login = XMLUtils.readBaseInfo()
                    kv.encode(Constant.KEY_USERID, login.studentId)
                    kv.encode(Constant.KEY_SCHOOLID, login.schoolId)
//                    kv.encode("url", login.url)
                    kv.encode(Constant.KEY_ACCOUNT, login.userAccount)
                    kv.encode(Constant.KEY_PWD, login.userPwd)
                    Network.URL = login.url.replace("/zhjy", "")
                }
                .flatMap {
                    when {
                        //  本地文件已上传阿里云
                        mUrl.startsWith("http") -> Observable.just(mUrl)
                        else -> OSSPutObject.getInstance(this).connOssKey()
                                .map { oss ->
                                    oss.putObjectFromLocalFile(mUrl)
                                }
                    }
                }
                .filter {
                    // 本地pdf上传阿里云，并下载到指定位置
                    if (it.endsWith(".pdf")) {
                        downloadFile(it, false)
                        false
                    } else {
                        true
                    }
                }
                .toFlowable(BackpressureStrategy.BUFFER)
                .flatMap {
                    // 缓存已上传到阿里云的源文件连接
                    kv.encode(mUrl, it)
                    pdfUrl = it
                    // token 每天头次访问时更新
                    val timeToken = kv.decodeLong(Constant.KEY_TIMETOKEN)
                    var token = kv.decodeString(Constant.KEY_TOKEN) ?: ""
                    // 获取时间过10小时就更新token
                    if (Date().time - timeToken > 10 * 60 * 60 * 1000) {

                        val param = TokenParam()
                        param.accountName = kv.decodeString(Constant.KEY_ACCOUNT) ?: ""
                        param.password = UpData.updateService
                                .decryptAndEncrypt(param.accountName, kv.decodeString(Constant.KEY_PWD)
                                        ?: "")
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
                                    kv.encode(Constant.KEY_TOKEN, token)
                                    kv.encode(Constant.KEY_TIMETOKEN, Date().time)
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
                    finish()
                })
    }

    /**
     * 加载pdf
     *
     * @Author cc
     * @Date 2020/5/26 16:01
     * @version 1.0
     */
    private fun loadPdf(file: File) {

        val cPager = kv.decodeInt("${savePath}_$saveName")
        libpdf_main_pdf.fromFile(file)
                .pageSnap(true)
                .defaultPage(cPager)
                .linkHandler { }
                .onPageError { page, t ->
                    hideProgress()
                    toast("pdf加载:$page 页 $t")
                }
                .onError {
                    hideProgress()
                    toast("pdf打开:$it")
                }
                .onLoad { hideProgress() }
                .spacing(Util.getDP(this, 8)) // 每页间隔（需要添加控件背景）
                .scrollHandle(WpsScrollHandle(this)) // 滑动栏
                .load()
        // 转换后的文件
        if (!mUrl.startsWith("http") && (!mUrl.endsWith(".pdf"))) {
            FileUtil.deleteFile(mUrl)
        }
    }


    /**
     * 转换成pdf
     * @Author cc
     * @Date 2020/5/26 16:02
     * @version 1.0
     */
    private fun toPdf(token: String) {

        disPdf?.apply { if (!isDisposed) dispose() }

        val param = ToPdfParam()
        param.ossfileUrl = pdfUrl
        val body = RequestBody.create(MediaType.parse(
                "application/json; charset=utf-8"), Gson().toJson(param))
        disPdf = Network.getAPI(this).fileToPdf(token, body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe({
                    if (it.success == 1) {
                        downloadFile(it.data.pdfUrl, false)
                    } else if (it.error.contains("转化中") && retryIndex < DEFAULT_RETRY) {
                        // 延迟2s再重试
                        window.decorView.postDelayed({
                            retryIndex++
                            if (retryIndex == DEFAULT_RETRY) {
                                showRetry(true)
                            } else {
                                toPdf(token)
                            }
                        }, 2000)

                    } else {
                        toast("转换:$it.error")
                        finish()
                    }
                }, {
                    val retry = kv.decodeBool(Constant.KEY_RETRY)
                    if ((it.toString().contains("HTTP 500") || retry) && retryIndex < DEFAULT_RETRY) {
                        // 延迟2s再重试
                        window.decorView.postDelayed({
                            retryIndex++
                            if (retryIndex == DEFAULT_RETRY) {
                                showRetry(true)
                            } else {
                                toPdf(token)
                            }
                        }, 2000)
                    } else {
                        toast("转换异常:$it")
                        finish()
                    }
                })
    }

    /**
     *  下载文件
     *
     */

    private fun downloadFile(pdfUrl: String, isTry: Boolean) {
        val task = Task(url = pdfUrl, saveName = "$saveName.pdf", savePath = savePath)
        val file = task.file()
        // 文件已存在，则直接使用
        if (file.exists()) {
            loadPdf(file)
            return
        }
        disposable = task.download()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                }, onComplete = {
                    loadPdf(file)
                }, onError = {
                    if (isTry) {
                        initRx()
                    } else {
                        toast("下载异常:$it")
                        finish()
                    }
                })
    }

    /**
     *  显示重试对话框
     * @Author cc
     * @Date 2020/5/26 16:45
     * @version 1.0
     */
    private fun showRetry(type: Boolean) {
        AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("文件转换中，继续等待")
                .setPositiveButton("确定") { d, _ ->
                    d.dismiss()
                    retryIndex = 0
                    if (type) {
                        toPdf(kv.decodeString(Constant.KEY_TOKEN) ?: "")
                    } else {
                        val tempUrl = kv.decodeString(mUrl) ?: ""
                        val tempFile = File(tempUrl)
                        val temp = tempUrl.substring(0, tempUrl.lastIndexOf("/") + 1) +
                                tempFile.nameWithoutExtension + ".pdf"
                        downloadFile(temp, true)
                    }
                }
                .setNegativeButton("退出") { d, _ ->
                    d.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
    }


    override fun onDestroy() {
        super.onDestroy()
        hideProgress()
        kv.encode("${savePath}_$saveName", libpdf_main_pdf.currentPage)
        disRx?.apply { if (!isDisposed) dispose() }
        disPdf?.apply { if (!isDisposed) dispose() }
        disposable?.apply { if (!isDisposed) dispose() }
        UpData.destroy(this)
        RxBusNew.getInstance().reset()
    }

    companion object {
        // 源文件url或路径
        private lateinit var mUrl: String
        // 保存地址
        private lateinit var savePath: String
        // 保存名称
        private lateinit var saveName: String
        // 强制下载
        private var isDown: Boolean = false

        // 转换pdf失败默认重试次数
        private const val DEFAULT_RETRY = 5
        // 获取读写权限
        private const val CODE_PERMISSION_READ = 0

        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径，默认保存在 根路径/pdf/
         * @param name 转换后的pdf文件本地保存名称，纯文件名，不带后缀
         * @param down true：强制下载文件；false：如果文件已存在则直接打开，不存在则进行转换后下载并打开
         */
        fun start(ctx: Context, url: String, path: String = FileUtil.getDirPath(ctx) + "/pdf/",
                  name: String = File(url).nameWithoutExtension, down: Boolean = false) {
            val intent = Intent(ctx, PdfActivity::class.java)

            mUrl = url
            savePath = path
            saveName = name
            isDown = down

            ctx.startActivity(intent)
        }

    }
}
