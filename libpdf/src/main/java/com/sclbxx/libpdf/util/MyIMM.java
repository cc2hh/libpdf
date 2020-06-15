package com.sclbxx.libpdf.util;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * author cc
 * data 2016/1/21
 * version V1.0
 */

public class MyIMM {

    /**
     * 隐藏界面输入法
     *
     * @param context 当前活动
     * @param v       焦点view
     */
    public static void hideSoftInput(Context context, View v) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0); // 强制隐藏键盘
    }

    //显示虚拟键盘
    public static void ShowKeyboard(Context context, View v) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
    }
}
