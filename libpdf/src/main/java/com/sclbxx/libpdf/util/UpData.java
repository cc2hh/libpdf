package com.sclbxx.libpdf.util;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.jddz.service.UpdateService;
import com.sclbxx.libpdf.pojo.Event;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

/**
 * function: <p>
 * author: cc <p>
 * data: 2017/12/6 10:15 <p>
 */

public class UpData {

    private static ServiceConnection switchServiceConnection;
    public static boolean isConnection;
    public static UpdateService updateService;
    private static Disposable disposable;

    // 慧道市场更新
    public static void updateByService(Activity context) {

        if (switchServiceConnection == null) {
            switchServiceConnection = new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    updateService = UpdateService.Stub.asInterface(service);
                    isConnection = true;
                    RxBusNew.getInstance().postSticky(new Event(Event.Companion.getCODE_MDM(), true));
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    destroy(context);
                }

//                @Override
//                public void onNullBinding(ComponentName name) {
////                    ToastUtils.showToast("onNullBinding");
//                }

                @Override
                public void onBindingDied(ComponentName name) {
                    destroy(context);
//                    ToastUtils.showToast("onBindingDied");
                }
            };
            Intent intent = new Intent();
            intent.setAction("com.jddz.aidl.updateAppService");
            intent.setPackage("com.jddz.mdm");
            context.bindService(intent, switchServiceConnection, Service.BIND_AUTO_CREATE);
            // 延迟5s没有连接回复，就提示
            disposable = Flowable.interval(1, TimeUnit.SECONDS)
                    .skip(5)
                    .take(1)
                    .filter(it -> !isConnection)
                    .subscribe(it -> {
                        RxBusNew.getInstance().postSticky(new Event(Event.Companion.getCODE_MDM(), false));
                        destroy(context);
                    });
        }
    }

    /**
     * 注销更新绑定
     */
    public static void destroy(Context context) {
        if (switchServiceConnection != null && isConnection) {
            isConnection = false;
            context.unbindService(switchServiceConnection);
            switchServiceConnection = null;
            updateService = null;
        }

        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

}
