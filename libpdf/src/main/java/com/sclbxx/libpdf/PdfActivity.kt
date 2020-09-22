package com.sclbxx.libpdf

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
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
import io.reactivex.Flowable
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
import java.lang.NullPointerException
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
    // 下载的文件
    private lateinit var loadUrl: String
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
        connectMdm()
        initData()

    }

    /**
     *  连接管家服务
     * @Author cc
     * @Date 2020/6/23 14:27
     * @version 1.0
     */
    private fun connectMdm() {
        showProgress().setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                onBackPressed()
            }
            false
        }
        UpData.updateByService(this)
    }

    /**
     *  初始化数据
     *
     */
    private fun initData() {

        // 初始化重试次数
        retryIndex = 0
        // 源文件
        val file = File(mUrl)
        // 源文件的后缀名，转小写后
        val extension = file.extension.toLowerCase()
        // 转换后的文件
        val filePdf = File("$savePath/$saveName.$EXTENSION")
        // 源文件对应的阿里云地址
        val ossUrl = kv.decodeString(mUrl)

        // 不管服务文件有没有，先拼凑链接地址尝试直接下载文件
        when {
            // 检测文件类型
            !FileUtil.checkType(extension) -> {
                toast("$extension 为不支持的文件类型")
                onBackPressed()
            }
            // 网络文件类型
            mUrl.startsWith("http") -> {
                if (isDown) {
                    // 强制重新下载，删除本地pdf文件
                    FileUtil.deleteFile(filePdf.absolutePath)
                } else if (filePdf.exists()) {
                    // 没要求强制重新下载，转换后的文件已存在
                    // 直接加载pdf
                    loadPdf(filePdf)
                    return
                }
                tryDown(mUrl)
            }
            // 源文件是本地文件且不存在
            !file.exists() -> {
                toast("文件不存在")
                onBackPressed()
            }
            // 源文件是本地文件且存在，转换后的文件已存在
            filePdf.exists() -> loadPdf(filePdf)
            // 源文件是本地文件且存在，已有缓存阿里云地址
            !ossUrl.isNullOrEmpty() -> tryDown(ossUrl)
            else -> initRx()
        }
    }

    /**
     *  尝试下载pdf
     * @Author cc
     * @Date 2020/5/27 18:18
     * @version 1.0
     */
    private fun tryDown(url: String) {
        val file = File(url)
        // 兼容后缀名大小写
        val extension = if (file.extension.toLowerCase() == EXTENSION) file.extension else EXTENSION
        val temp = "${url.substring(0, url.lastIndexOf("/") + 1) + file.nameWithoutExtension}.$extension"
        downloadFile(temp, true)
    }

    /**
     *  pdf转换主体流程
     * @Author cc
     * @Date 2020/5/27 18:18
     * @version 1.0
     */
    private fun initRx() {
        // 接受管家服务连接信息
        RxBusNew.getInstance().toObservableSticky(Event::class.java)
                .filter {
                    if (it.boolean) true
                    else {
                        hideProgress()
                        AlertDialog.Builder(this)
                                .setTitle("提示")
                                .setMessage("尝试重连管家服务")
                                .setPositiveButton("确定") { v, _ ->
                                    connectMdm()
                                    v.dismiss()
                                }
                                .setNegativeButton("退出") { v, _ ->
                                    v.dismiss()
                                    onBackPressed()
                                }
                                .setCancelable(false)
                                .show()
                        false
                    }
                }
                // 切换到子线程，网络请求
                .observeOn(Schedulers.io())
                // oss上传文件需要参数，所以先进行参数缓存
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
                    val ossUrl = kv.decodeString(mUrl)
                    when {
                        mUrl.startsWith("http") -> Flowable.just(mUrl)
                        !ossUrl.isNullOrEmpty() -> Flowable.just(ossUrl)
                        //  本地文件上传阿里云
                        else -> OSSPutObject.getInstance(this).connOssKey()
                                .map { oss -> oss.putObjectFromLocalFile(mUrl) }
                    }
                }
                .flatMap {
                    // 缓存已上传到阿里云的源文件连接
                    kv.encode(mUrl, it)
                    // token 每天头次访问时更新
                    val timeToken = kv.decodeLong(Constant.KEY_TIMETOKEN)
                    var token = kv.decodeString(Constant.KEY_TOKEN)
                    // 获取时间过10小时就更新token
                    if (Date().time - timeToken <= 10 * 60 * 60 * 1000 && !token.isNullOrEmpty()) {
                        Flowable.just(token)
                    } else {
                        val account = kv.decodeString(Constant.KEY_ACCOUNT)
                        val pwd = kv.decodeString(Constant.KEY_PWD)
                        // 检查账号或密码为空
                        if (account.isNullOrEmpty() || pwd.isNullOrEmpty()) {
                            throw Exception("账号或者密码为空")
                        }
                        val param = TokenParam()
                        param.accountName = account
                        try {
                            param.password = UpData.updateService.decryptAndEncrypt(account, pwd)
                        } catch (e: Exception) {
                            throw Exception("管家服务：$e")
                        }
                        val strParam = Gson().toJson(param)
                        println("打印参数---getToken:$strParam")
                        val body = RequestBody.create(MediaType.parse(
                                "application/json; charset=utf-8"), strParam)
                        Network.getAPI(this).getToken(body)
                                .observeOn(AndroidSchedulers.mainThread())
                                .filter { item ->
                                    if (item.success == 1) {
                                        true
                                    } else {
                                        toast("token异常:${item.error}")
                                        onBackPressed()
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
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe({ toPdf(it) }, {
                    toast("Rx异常：$it")
                    onBackPressed()
                })
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
        param.ossfileUrl = kv.decodeString(mUrl)
        val strParam = Gson().toJson(param)
        println("打印参数---fileToPdf:$strParam")
        val body = RequestBody.create(MediaType.parse(
                "application/json; charset=utf-8"), strParam)
        disPdf = Network.getAPI(this).fileToPdf(token, body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe({
                    when {
                        it.success == 1 -> downloadFile(it.data.pdfUrl, false)
                        // token 错误，重新获取token
                        it.error.contains("token") || it.error.contains("用户不存在") -> {
                            kv.encode(Constant.KEY_TIMETOKEN, 0L)
                            RxBusNew.getInstance().postSticky(Event(Event.CODE_MDM, true))
                        }
                        else -> showRetry(true, it.error)
                    }
                }, {
                    if ((it.toString().contains("HTTP 500") || kv.decodeBool(Constant.KEY_RETRY))) {
                        showRetry(true, it.toString())
                    } else {
                        toast("转换异常:$it")
                        onBackPressed()
                    }
                })
    }

    /**
     *  下载文件
     *
     */
    private fun downloadFile(url: String, isTry: Boolean) {
        val task = Task(url = url, saveName = "$saveName.$EXTENSION", savePath = savePath)
        val file = task.file()
        // 文件已存在，则直接使用
        if (file.exists()) {
            loadPdf(file)
            return
        }
        loadUrl = url

        disposable?.apply { if (!isDisposed) dispose() }

        task.download()
        disposable = task.download(request = MySSLRequest())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                }, onComplete = {
                    loadPdf(file)
                }, onError = {
                    if (isTry) {
                        initRx()
                    } else {
                        showRetry(false, it.toString())
                    }
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

        // 删除源文件是本地且不是pdf的文件
        if (!mUrl.startsWith("http") && File(mUrl).extension.toLowerCase() != EXTENSION) {
            FileUtil.deleteFile(mUrl)
        }

        // 缓存的该文件之前关闭时页码，默认0
        val cPager = kv.decodeInt("${savePath}_$saveName")
        libpdf_main_pdf.fromFile(file)
                .pageSnap(true)
                .defaultPage(cPager)
                .linkHandler { }
                .onPageError { page, t ->
                    hideProgress()
                    toast("pdf文件错误，加载:$page 页 $t")
                }
                .onError {
                    hideProgress()
                    toast("pdf文件损坏:$it")
                }
                .onLoad {
                    hideProgress()
                    resultOk = Activity.RESULT_OK
                }  // 加载完成
                .spacing(Util.getDP(this, 8)) // 每页间隔（需要添加控件背景）
                .scrollHandle(WpsScrollHandle(this)) // 滑动栏
                .load()
    }

    /**
     *  显示重试对话框
     * @Author cc
     * @Date 2020/5/26 16:45
     * @version 1.0
     */
    private fun showRetry(type: Boolean, msg: String) {
        // 延迟2s再重试
        window.decorView.postDelayed({
            retryIndex++
            if (retryIndex >= DEFAULT_RETRY) {
                toast("重试：$msg")
                retryIndex = 0
                AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("文件转换中，继续等待\n $msg")
                        .setPositiveButton("确定") { d, _ ->
                            swichOpera(type)
                            d.dismiss()
                        }
                        .setNegativeButton("退出") { d, _ ->
                            d.dismiss()
                            onBackPressed()
                        }
                        .setCancelable(false)
                        .show()
            } else {
                swichOpera(type)
            }
        }, 2000)
    }

    /**
     *  在那阶段发生的错误，从那阶段重试
     * @Author cc
     * @Date 2020/5/26 16:45
     * @version 1.0
     */
    private fun swichOpera(type: Boolean) {
        if (type) {
            toPdf(kv.decodeString(Constant.KEY_TOKEN) ?: "")
        } else {
            downloadFile(loadUrl, false)
        }
    }

    override fun onBackPressed() {
        setResult(resultOk)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgress()
        kv.encode("${savePath}_$saveName", libpdf_main_pdf.currentPage)
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
        // 默认文件后缀名
        private const val EXTENSION = "pdf"
        private var resultOk = Activity.RESULT_CANCELED

        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径，默认保存在 根路径/pdf/
         * @param name 转换后的pdf文件本地保存名称，纯文件名，不带后缀
         * @param down true：强制下载文件；false：如果文件已存在则直接打开，不存在则进行转换后下载并打开
         */
        fun start(ctx: Context, url: String, path: String = FileUtil.getDirPath(ctx) + "/pdf/",
                  name: String = File(url).nameWithoutExtension, down: Boolean = false, code: Int = 0) {
            val intent = Intent(ctx, PdfActivity::class.java)

            mUrl = url
            savePath = path
            saveName = name
            isDown = down
            resultOk = Activity.RESULT_CANCELED
            (ctx as Activity).startActivityForResult(intent, code)
        }


    }
}
