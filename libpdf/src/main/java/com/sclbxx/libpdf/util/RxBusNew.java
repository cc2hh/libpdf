package com.sclbxx.libpdf.util;


import com.jakewharton.rxrelay2.ReplayRelay;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * @version 1.0
 * @Author cc
 * @Date 2018/12/26-11:56
 */
public class RxBusNew {
    private volatile static RxBusNew mDefaultInstance;
    private final ReplayRelay<Object> mBusSticky;
    private final PublishSubject<Object> mBus;


    private RxBusNew() {
        mBusSticky = ReplayRelay.create();
        mBus = PublishSubject.create();
    }

    public static RxBusNew getInstance() {
        if (mDefaultInstance == null) {
            synchronized (RxBusNew.class) {
                if (mDefaultInstance == null) {
                    mDefaultInstance = new RxBusNew();
                }
            }
        }
        return mDefaultInstance;
    }

    /**
     * 发送事件
     */
    public void post(Object event) {
        mBus.onNext(event);
    }

    /**
     * 使用Rxlifecycle解决RxJava引起的内存泄漏
     */
    public <T> Flowable<T> toObservable(final Class<T> eventType) {
        return mBus.ofType(eventType)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER);
//                .as(AutoDispose.autoDisposable(AndroidLifecycleScopeProvider.from(owner,
//                Lifecycle.Event.ON_DESTROY)));
    }

    /**
     * 判断是否有订阅者
     */
    public boolean hasObservers() {
        return mBus.hasObservers();
    }

    public void reset() {
        mDefaultInstance = null;
    }


    /**
     * Stciky 相关
     */

    /**
     * 发送一个新Sticky事件
     */
    public void postSticky(Object event) {
        mBusSticky.accept(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 被观察者
     * 使用Rxlifecycle解决RxJava引起的内存泄漏
     */
    public <T> Flowable<T> toObservableSticky(final Class<T> eventType) {
        return mBusSticky.ofType(eventType)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toFlowable(BackpressureStrategy.BUFFER);
    }
}
