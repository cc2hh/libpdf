package com.sclbxx.libpdf.util

import android.os.Environment
import java.io.File

/**
 *
 * 文件工具类
 * @Author cc
 * @Date 2020/5/21-16:14
 * @version 1.0
 */
class FileUtil {

    companion object {

        /**
         * 获取根路径
         */
        fun getDirPath(): String {
            return if (Environment.MEDIA_MOUNTED == Environment
                            .getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                Environment.getExternalStorageDirectory().path
            } else {
                ""
            }
        }

        /**
         *  检测是否是能打开文件类型
         *
         */
        fun checkType(extension: String): Boolean {
            return when (extension) {
                "pdf", "txt", "doc", "docx", "xls", "xlsx", "xml", "ppt", "pptx", "log" -> true
                else -> false
            }
        }

        fun deleteFile(path: String): Boolean {
            if (path.trim { it <= ' ' }.isEmpty()) {
                return true
            }

            val file = File(path)
            if (!file.exists()) {
                return true
            }
            if (file.isFile) {
                return file.delete()
            }

            if (file.isDirectory) {
                return false
            }
            for (f in file.listFiles()) {
                if (f.isFile) {
                    f.delete()
                } else if (f.isDirectory) {
                    deleteFile(f.absolutePath)
                }
            }
            return file.delete()
        }
    }
}