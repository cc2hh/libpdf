package com.sclbxx.libpdf

import android.Manifest
import android.app.Activity
import androidx.lifecycle.Lifecycle
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import android.view.View
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
        requestPermissions(CODE_PERMISSION_READ, "存储", Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

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

        // txt文件单独处理，直接下载后本地显示
        if (srcExtension == "txt") {
            when {
                mUrl.startsWith("http") -> downloadTxtFile()
                File(mUrl).exists() -> showTxt(mUrl)
                else -> {
                    toast("文件不存在")
                    onBackPressed()
                }
            }
            return
        }
        showDialog()
        initData()

    }

    private fun showTxt(path: String) {
        libpdf_main_sv.visibility = View.VISIBLE
        libpdf_main_tv.text = FileUtil.readTxt(path)
    }

    /**
     *  连接管家服务
     * @Author cc
     * @Date 2020/6/23 14:27
     * @version 1.0
     */
    private fun showDialog() {
        showProgress().setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                hideProgress()
                onBackPressed()
            }
            false
        }
    }

    /**
     *  初始化数据
     *
     */
    private fun initData() {

        // 初始化重试次数
        retryIndex = 0

        val filePdf = File("$savePath/$saveName.$EXTENSION")

//        // 源文件对应的阿里云地址
//        val ossUrl = kv.decodeString(mUrl)

        // 不管服务文件有没有，先拼凑链接地址尝试直接下载文件
        when {
            // 检测文件类型
            !FileUtil.checkType(srcExtension) -> {
                toast("$srcExtension 为不支持的文件类型")
                onBackPressed()
            }
            // 网络文件类型
            mUrl.startsWith("http") -> {
                val wvUrl = kv.decodeString(WEBVIEWURL + mUrl, "")
                when {
                    mUrl.contains("ow365.cn") -> {
                        gotoWebView(mUrl)
                        return
                    }
                    wvUrl != "" -> {
                        gotoWebView(wvUrl)
                        return
                    }
                    isDown -> // 强制重新下载，删除本地pdf文件
                        // 转换后的文件
                        FileUtil.deleteFile(filePdf.absolutePath)
                    filePdf.exists() -> {
                        // 没要求强制重新下载，转换后的文件已存在
                        // 直接加载pdf
                        loadPdf(filePdf)
                        return
                    }
                }

                // ppt强制走在线模式
                if (srcExtension.contains("ppt")) {
                    ishtml = 1
                    val wvUrl = kv.decodeString(WEBVIEWURL + mUrl, "")
                    // 已有缓存在线地址直接预览，否则需要走接口请求在线地址
                    if (wvUrl != "") {
                        gotoWebView(wvUrl)
                    } else {
                        initRx()
                    }
                } else {
                    tryDown()
                }
            }
            // 源文件是本地pdf文件
            srcExtension == EXTENSION -> {
                loadPdf(File(mUrl))
            }
            else -> {
                toast("不支持本地文件转换服务")
                onBackPressed()
            }
        }
    }

    /**
     *  尝试下载pdf
     * @Author cc
     * @Date 2020/5/27 18:18
     * @version 1.0
     */
    private fun tryDown() {
        val file = File(mUrl)
        // 兼容后缀名大小写
//        val extension = if (srcExtension == EXTENSION) file.extension else EXTENSION
        val temp = "${mUrl.substring(0, mUrl.lastIndexOf("/") + 1) + file.nameWithoutExtension}.$EXTENSION"
        downloadFile(temp, true)
    }


    /**
     *  pdf转换主体流程
     * @Author cc
     * @Date 2020/5/27 18:18
     * @version 1.0
     */
    private fun initRx() {

        Flowable.just(XMLUtils.readBaseInfo())
                // 切换到子线程，网络请求
                .observeOn(Schedulers.io())
                .flatMap {
                    Network.URL = it.url.replace("/zhjy", "")
                    // 缓存已上传到阿里云的源文件连接
//                    kv.encode(mUrl, it)
                    // token 每天头次访问时更新
                    val timeToken = kv.decodeLong(Constant.KEY_TIMETOKEN)
                    var token = kv.decodeString(Constant.KEY_TOKEN)
                    // 获取时间过10小时就更新token
                    if (Date().time - timeToken <= 10 * 60 * 60 * 1000 && !token.isNullOrEmpty()) {
                        Flowable.just(token)
                    } else {

                        val account = it.userAccount
                        val pwd = it.userPwd

                        // 检查账号或密码为空
                        if (account.isNullOrEmpty() || pwd.isNullOrEmpty()) {
                            throw Exception("账号或者密码为空")
                        }

                        val password = DesUtil.md5(DesUtil.decode(DesUtil.KEY, account, pwd))
                        val json = Gson().toJson(TokenParam(account, password))
                        val body = ParamUtil.getJsonParam(json)

                        println("打印参数---getToken:$json")
                        Network.getAPI(this).getToken(body)
                                .observeOn(Schedulers.io())
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
//        param.ossfileUrl = kv.decodeString(mUrl)
        param.ossfileUrl = mUrl
        // 兼容旧版（未增加webview服务前），之后版本传该参数，后台好识别业务流程
        param.ishtml = ishtml

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
                        it.success == 1 -> {
                            // webview流程
                            if (it.data.pdfUrl.contains("&ishtml=1")) {
                                kv.encode(WEBVIEWURL + mUrl, it.data.pdfUrl)

                                gotoWebView(it.data.pdfUrl)
                            } else {
                                downloadFile(it.data.pdfUrl, false)
                            }
                        }
                        // token 错误，重新获取token
                        it.error.contains("token") || it.error.contains("用户不存在") -> {
                            kv.encode(Constant.KEY_TIMETOKEN, 0L)
                            RxBusNew.getInstance().postSticky(Event(Event.CODE_MDM, true))
                        }
                        else -> showRetry(true, it.error)
                    }
                }, {
                    if ((it.toString().contains("HTTP 500") || kv.decodeBool(Constant.KEY_RETRY))) {
//                        retryIndex = DEFAULT_RETRY
                        showRetry(true, "远程服务繁忙...请稍后再试")
                    } else {

                        toast("转换异常:$it")
                        onBackPressed()
                    }
                })
    }


    /**
     *  跳转到webview流程
     *
     */
    private fun gotoWebView(url: String) {
        hideProgress()
        WebViewActivity.start(this@PdfActivity, url)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.REQUEST_CODE_ZERO) {
            if (resultCode == Activity.RESULT_CANCELED) {
                // webview访问失败时
                showDialog()
                ishtml = 0
                tryDown()
            }
        }
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

        disposable = task.download(request = MySSLRequest())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                }, onComplete = {
                    loadPdf(file)
                }, onError = {
                    when {
                        srcExtension == EXTENSION -> {
                            toast("原始pdf文件下载失败，请检查原始文件是否正常")
                            onBackPressed()
                        }
                        isTry -> initRx()
                        else -> showRetry(false, it.toString())
                    }
                })
    }

    /**
     *  下载TXT文件
     *
     */
    private fun downloadTxtFile() {
        val task = Task(url = mUrl, saveName = "$saveName.$srcExtension", savePath = savePath)
        val file = task.file()
        // 文件已存在，则直接使用
        if (file.exists()) {
            showTxt(file.absolutePath)
            return
        }

        disposable?.apply { if (!isDisposed) dispose() }

        disposable = task.download(request = MySSLRequest())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                }, onComplete = {
                    showTxt(file.absolutePath)
                }, onError = {
                    toast("下载TXT文件：$it")
                    onBackPressed()
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
        if (!mUrl.startsWith("http") && srcExtension != EXTENSION) {
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
                    FileUtil.deleteFile(file.absolutePath)

                }
                .onError {
                    hideProgress()
                    toast("pdf文件损坏:$it")
                    FileUtil.deleteFile(file.absolutePath)
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

            if (!isFinishing) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed) {
                    return@postDelayed
                }
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
        setResult(-1)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideProgress()
        kv.encode("${savePath}_$saveName", libpdf_main_pdf.currentPage)
        disposable?.apply { if (!isDisposed) dispose() }
        disPdf?.apply { if (!isDisposed) dispose() }
        RxBusNew.getInstance().reset()
    }

    companion object {
        // 源文件url或路径
        private lateinit var mUrl: String
        // 保存地址
        private lateinit var savePath: String
        // 保存名称
        private lateinit var saveName: String
        // 源文件后缀名
        private lateinit var srcExtension: String
        // 强制下载
        private var isDown: Boolean = false
        // 是否执行webview流程，默认走
        private var ishtml: Int = 1

        // 转换pdf失败默认重试次数
        private const val DEFAULT_RETRY = 5
        // 获取读写权限
        private const val CODE_PERMISSION_READ = 0
        // 默认文件后缀名
        private const val EXTENSION = "pdf"
        // 缓存webviewUrl
        private const val WEBVIEWURL = "webviewUrl"

        private var resultOk = Activity.RESULT_CANCELED

        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径，默认保存在 根路径/pdf/
         * @param name 转换后的pdf文件本地保存名称，纯文件名，不带后缀
         * @param down true：强制下载文件；false：如果文件已存在则直接打开，不存在则进行转换后下载并打开
         * @param code 需要startActivityForResult的code值
         * @param html 默认 1 ，兼容webview模式；0，只执行转换服务
         */
        fun start(activity: AppCompatActivity,
                  url: String,
                  path: String = FileUtil.getDirPath(activity) + "/pdf/",
                  name: String = File(url).nameWithoutExtension,
                  down: Boolean = false,
                  code: Int = 0,
                  html: Int = 1,
                  extension: String = File(url).extension) {
            val intent = Intent(activity, PdfActivity::class.java)

            mUrl = url.replace("https", "http")
            savePath = path
            saveName = name
            srcExtension = extension.toLowerCase()
            isDown = down
            ishtml = html
            resultOk = Activity.RESULT_CANCELED
            activity.startActivityForResult(intent, code)
        }


        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径，默认保存在 根路径/pdf/
         * @param name 转换后的pdf文件本地保存名称，纯文件名，不带后缀
         * @param down true：强制下载文件；false：如果文件已存在则直接打开，不存在则进行转换后下载并打开
         * @param code 需要startActivityForResult的code值
         */
        fun start(activity: AppCompatActivity,
                  url: String,
                  path: String = FileUtil.getDirPath(activity) + "/pdf/",
                  name: String = File(url).nameWithoutExtension,
                  down: Boolean = false,
                  code: Int = 0) {
            start(activity, url, path, name, down, code, 1)
        }


        /**
         *  跳转
         *
         * @param url 文件本地路径或网络链接
         * @param path 转换后的pdf文件本地保存路径，默认保存在 根路径/pdf/
         * @param name 转换后的pdf文件本地保存名称，纯文件名，不带后缀
         * @param down true：强制下载文件；false：如果文件已存在则直接打开，不存在则进行转换后下载并打开
         * @param code 需要startActivityForResult的code值
         * @param html 默认 1 ，兼容webview模式；0，只执行转换服务
         * @param extension 默认从文件本地路径或网络链接获取
         */
        fun startCtx(ctx: Context,
                     url: String,
                     path: String = FileUtil.getDirPath(ctx) + "/pdf/",
                     name: String = File(url).nameWithoutExtension,
                     down: Boolean = false,
                     html: Int = 1,
                     extension: String = File(url).extension) {

            val intent = Intent(ctx, PdfActivity::class.java)
            // android 10 后Calling startActivity() from outside of an Activity  context requires the FLAG_ACTIVITY_NEW_TASK flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            mUrl = url.replace("https", "http")
            savePath = path
            saveName = name
            isDown = down
            ishtml = html
            srcExtension = extension.toLowerCase()

            ctx.startActivity(intent)
        }

    }
}
