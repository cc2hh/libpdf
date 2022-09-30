package com.sclbxx.libpdf.util

import android.content.Context
import android.os.Environment
import java.io.*


/**
 *
 * 文件工具类
 * @Author cc
 * @Date 2020/5/21-16:14
 * @version 1.0
 */
object FileUtil {


    private const val FILE_EXTENSION_SEPARATOR = "."

    /**
     * 获取根路径
     */
    fun getDirPath(ctx: Context): String {
        return if (Environment.MEDIA_MOUNTED == Environment
                .getExternalStorageState() || !Environment.isExternalStorageRemovable()
        ) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/huidao/"
        } else {
            // 返回应用内部缓存路径
            ctx.cacheDir.path + "/huidao/"
        }
    }

    fun prefixLength(pathname: String): Int {
        if (pathname.isEmpty()) return 0
        return if (pathname[0] == '/') 1 else 0
    }

    /**
     *  无后缀名的文件名称
     *
     */
    fun fileNameWithoutExtension(filePath: String): String {

        val extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR)
        val filePosi = filePath.lastIndexOf(File.separator)
        if (filePosi == -1) {
            return if (extenPosi == -1) filePath else filePath.substring(0, extenPosi)
        }
        if (extenPosi == -1) {
            return filePath.substring(filePosi + 1)
        }
        return if (filePosi < extenPosi) filePath.substring(filePosi + 1, extenPosi) else
            filePath.substring(filePosi + 1)
    }

    /**
     * get folder name from path
     * <p>
     * <pre>
     *      getFolderName("abc")              =   ""
     *      getFolderName("/a.d/admin")      =   "/a.d"
     *      getFolderName("/home/admin")      =   "/home"
     *      getFolderName("/home/admin/a.txt/b.mp3")  =   "/home/admin/a.txt"
     * </pre>
     *
     * @param filePath
     * @return  是"/home" ，不是 "/home/"
     */
    fun parent(path: String): String {
        val index = path.lastIndexOf(File.separator)
        return if (index == -1) "" else path.substring(0, index + 1)
    }

    /**
     * get file name from path, include suffix
     * <p>
     * <pre>
     *      getFileName("a.mp3")            =   "a.mp3"
     * </pre>
     *
     * @param path
     * @return file name from path, include suffix
     */
    fun name(path: String): String {
        val index = path.lastIndexOf(File.separator)
        return if (index == -1) path else path.substring(index + 1)
    }

    /**
     * get suffix of file from path
     * <p>
     * <pre>
     *      getFileExtension("a.mp3")            =   "mp3"
     * </pre>
     *
     * @param path
     * @return
     */
    fun extension(path: String): String {

        val lastPoi = path.lastIndexOf(FILE_EXTENSION_SEPARATOR)
        val index = path.lastIndexOf(File.separator)
        if (index == -1) {
            return if (lastPoi == -1) path else path.substring(0, lastPoi)
        }
        return if (lastPoi == -1 || index > lastPoi) {
            path.substring(index + 1)
        } else path.substring(index + 1, lastPoi)
    }

    /**
     *  检测是否是能打开文件类型
     *
     */
    fun checkType(extension: String): Boolean {
        return when (extension.lowercase()) {
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
        for (f in file.listFiles()!!) {
            if (f.isFile) {
                f.delete()
            } else if (f.isDirectory) {
                deleteFile(f.absolutePath)
            }
        }
        return file.delete()
    }

    fun readTxt(txtPath: String): String {

        var inputStream: InputStream? = null
        val sb = StringBuffer("")
        try {
            inputStream = FileInputStream(txtPath)
            val inputStreamReader = InputStreamReader(inputStream, "UTF-8")
            val reader = BufferedReader(inputStreamReader)
            do {
                var line: String = reader.readLine() ?: break
                sb.append(line)
                sb.append("\n")
            } while (true)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()//关闭输入流
        }

        return sb.toString()
    }
}