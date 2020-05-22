package com.sclbxx.libpdf.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.socks.library.KLog;

/**
 * Created by cc on 2018/6/4.
 * <p>
 * function :
 */

public class ToastUtils {

    private static Toast toast;

    public static void showToast(Context context,String msg) {
        SystemUtil.runOnUiThread(() -> {
            if (toast == null) {
                toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
            } else {
                toast.setText(msg);
            }
            toast.show();
            Log.e(context.getClass().getName(),msg);
            System.out.println(msg);
        });
    }
}
