package com.sclbxx.libpdf;

import android.app.Activity;
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
    private static Context mContext;

    /**
     * 显示加载对话框
     */
    public static ProgressDialog showProgressDialog(Context context) {
        if (!mIsShow) {
            Activity activity = (Activity) context;
            if (!activity.isDestroyed() && !activity.isFinishing()) {
                mContext = context;
                progressDialog = new ProgressDialog(context);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
                progressDialog.setContentView(R.layout.libpdf_progress_load);
                mIsShow = true;
            }
        }
        return progressDialog;
    }

    /**
     * 隐藏加载对话框
     */
    public static void dimssProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            Activity activity = (Activity) mContext;
            // 判断对话框activi是否还有效
            if (!activity.isDestroyed() && !activity.isFinishing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
            mIsShow = false;
        }
    }

}
