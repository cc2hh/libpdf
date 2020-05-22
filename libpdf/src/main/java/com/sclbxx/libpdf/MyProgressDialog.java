package com.sclbxx.libpdf;

import android.app.ProgressDialog;
import android.content.Context;


/**
 * function: 自定义加载对话框 <p>
 * author: cc <p>
 * data: 2016/11/29 16:25 <p>
 * version: V1.0 <p/>
 */
public class MyProgressDialog {

    private static ProgressDialog progressDialog;
    // 是否显示对话框
    private static boolean mIsShow;

    /**
     * 显示加载对话框
     */
    public static ProgressDialog showProgressDialog(Context context) {
        if (!mIsShow) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            progressDialog.setContentView(R.layout.libpdf_progress_load);
            mIsShow = true;
        }
        return progressDialog;
    }

    /**
     * 隐藏加载对话框
     */
    public static void dimssProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
            mIsShow = false;
        }
    }

}
