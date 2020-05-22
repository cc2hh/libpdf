package com.sclbxx.libpdf.base

import android.app.ProgressDialog
import android.arch.lifecycle.Lifecycle
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.widget.Toast
import com.sclbxx.libpdf.MyProgressDialog
import com.sclbxx.libpdf.R
import com.sclbxx.libpdf.util.ToastUtils
import com.socks.library.KLog
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider


/**
 * fuction: 基础activity类
 * author: cc
 * data: 2016/10/24 9:56
 * version: V1.0
 */

open class BaseActivity : AppCompatActivity(), BaseView {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        if (Build.VERSION_CODES.LOLLIPOP > Build.VERSION.SDK_INT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(R.color.mTheme)
        }
    }

    /**
     * 请求权限
     */
    protected fun requestPermissions(code: Int, hint: String, vararg permissions: String) {
        RxPermissions(this).request(*permissions)
                .`as`(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider
                        .from(this, Lifecycle.Event.ON_DESTROY)))
                .subscribe { granted ->
                    if (granted!!) {
                        doPermissions(code)
                    } else {
                        toast("需要授权${hint}才能使用")
                        finish()
                    }
                }
    }

    /**
     * 请求权限成功后的操作
     */
    protected open fun doPermissions(code: Int) {
    }

    override fun toast(msg: String) {
        ToastUtils.showToast(this,msg)
    }

    override fun toast(id: Int) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show()
    }

    override fun showProgress(): ProgressDialog {
        return MyProgressDialog.showProgressDialog(this)
    }

    override fun hideProgress() {
        MyProgressDialog.dimssProgressDialog()
    }
}
