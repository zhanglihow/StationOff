package com.example.lenovo.stationoff.util

import java.io.File
import java.io.FileInputStream
import java.io.IOException





class FileUtils{
    companion object {

        /**
         * 读取文件的字节数组.
         *
         * @param file 文件
         * @return 字节数组
         */
        fun readFile4Bytes(file: File): ByteArray? {
            // 如果文件不存在,返回空
            if (!file.exists()) {
                return null
            }
            var fis: FileInputStream? = null
            try {
                // 读取文件内容.
                fis = FileInputStream(file)
                val arrData = ByteArray((file.length()).toInt())
                fis!!.read(arrData)
                // 返回
                return arrData
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            } finally {
                if (fis != null) {
                    try {
                        fis!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        /**
         *
         * @param path  路径
         * @return  是否删除成功
         */
        fun deleteFile(path: String): Boolean {
            if (path.isEmpty()) {
                return true
            }
            val file = File(path)
            if (!file.exists()) {
                return true
            }
            if (file.isFile) {
                return file.delete()
            }
            if (!file.isDirectory) {
                return false
            }
            for (f in file.listFiles()!!) {
                if (f.isFile) {
                    f.delete()
                } else if (f.isDirectory) {
                    deleteFile(f.absolutePath)
                }
            }
            return true
        }

    }
}