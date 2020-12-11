package com.sclbxx.libpdf

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * @title: PdfLib
 * @projectName MyApplication
 * @description: application初始化配置
 * @author cc
 * @date 2020/12/11 15:03
 */
object PdfLib {

    fun init(ctx: Context) {
        MMKV.initialize(ctx)
    }
}