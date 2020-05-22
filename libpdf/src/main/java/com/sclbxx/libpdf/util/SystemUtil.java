package com.sclbxx.libpdf.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by cc on 2018/4/2.
 * <p>
 * function :
 */

public class SystemUtil {

    public static Intent getAppOpenIntentByPackageName(Context context, String packageName) {
        // 方法一
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);

        // 方法二
//        String mainAct = null;
//        PackageManager pkgMag = context.getPackageManager();
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
//
//        @SuppressLint("WrongConstant")
//        List<ResolveInfo> list = pkgMag.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
//        for (int i = 0; i < list.size(); i++) {
//            ResolveInfo info = list.get(i);
//            if (info.activityInfo.packageName.equals(packageName)) {
//                mainAct = info.activityInfo.name;
//                break;
//            }
//        }
//        if (TextUtils.isEmpty(mainAct)) {
//            return null;
//        }
//        intent.setComponent(new ComponentName(packageName, mainAct));
        return intent;

    }

    /**
     * 打开其他应用，具体到哪个界面
     */
    public static Intent openApp(String packageName, String className) {
        Intent intent = new Intent();
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ComponentName cn = new ComponentName(packageName, className);
        intent.setComponent(cn);
        return intent;
    }



    public static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
}
