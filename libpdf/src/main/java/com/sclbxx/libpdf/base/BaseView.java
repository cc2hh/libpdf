package com.sclbxx.libpdf.base;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;

/**
 * fuction: 视图基类
 * author: cc
 * data: 2016/4/8 17:26
 * version: V1.0
 */
public interface BaseView {


    /**
     * Toast通知方法
     */
    void toast(@NonNull String msg);


    /**
     * Toast通知方法,从字符串资源id取值
     */
    void toast(int id);

//    /**
//     * Snackbar通知方法
//     */
//    void snackbar(@NonNull String msg);
//
//    /**
//     * Snackbar通知方法,从字符串资源id取值
//     */
//    void snackbar(int id);

    /**
     * 显示加载对话框
     */
    ProgressDialog showProgress();

    /**
     * 隐藏加载对话框
     */
    void hideProgress();
}
