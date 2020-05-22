package com.sclbxx.libpdf.pojo

/**
 *
 * rxbus事件
 * @Author cc
 * @Date 2019/10/21-17:06
 * @version 1.0
 */
data class Event(val code: Int,val boolean: Boolean) {
    companion object {
        // 管家服务通知
        val CODE_MDM = 0
    }

}