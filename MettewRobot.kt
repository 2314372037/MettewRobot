package com.zh

import java.io.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

object MettewRobot {
    private var isStop = false
    private var htmlIndex = 1
    private val companiesUrl = "http://mettew.com/companies/"
    private var cookie = ""//本次运行需要的cookie

    @JvmStatic
    fun main(a: Array<String>) {
        //获取本地的index
        htmlIndex = if (readFile("htmlIndex").isNotEmpty()) {
            readFile("htmlIndex").toInt()
        } else {
            1
        }
        println("last index is ${htmlIndex},continue")
        while (!isStop) {
            if (isStop) {
                break
            }
            val result = getResponse(companiesUrl + htmlIndex)
            if (result.isNotEmpty()) {//字符数量不为空则判定获取成功
                createNewFile(htmlIndex, result)
                writeFile("htmlIndex", htmlIndex.toString())
                htmlIndex++
            } else {
                htmlIndex++
            }
        }
        println("total ${htmlIndex},stop")
    }

    private fun getResponse(url: String): String {
        var result = ""
        try {
            //适当延迟一段时间，避免给服务器造成压力
            Thread.sleep(Random(System.currentTimeMillis()).nextInt(4, 8) * 1000L)
            val httpURLConnection = (URL(url).openConnection() as HttpURLConnection)
            httpURLConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36 Edg/90.0.818.49")
            httpURLConnection.addRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
            httpURLConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            if (cookie.isNotEmpty()) {
                httpURLConnection.addRequestProperty("cookie", cookie)
            }
            httpURLConnection.requestMethod = "GET"
            httpURLConnection.connectTimeout = 20 * 1000
            httpURLConnection.readTimeout = 20 * 1000
            if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                val header = httpURLConnection.headerFields
                if (header.containsKey("Set-Cookie")) {
                    val tempCookie = header.get("Set-Cookie")
                    if (!tempCookie.isNullOrEmpty()) {
                        println("reset Cookie...")
                        cookie = tempCookie[0]
                        Thread.sleep(Random(System.currentTimeMillis()).nextInt(4, 8) * 1000L)
                        println("continue")
                    }
                    result = ""
                } else {
                    result = stream2string(httpURLConnection.inputStream)
                }
            }else if (httpURLConnection.responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR){
                println("http response ${httpURLConnection.responseCode},skip $htmlIndex")
                htmlIndex++
            }else if (httpURLConnection.responseCode == HttpURLConnection.HTTP_NOT_FOUND){
                isStop = true
            }
            httpURLConnection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun stream2string(inputStream: InputStream?): String {
        val inputStreamReader = InputStreamReader(inputStream, "utf-8")
        val bufferedReader = BufferedReader(inputStreamReader)
        val temp = bufferedReader.readText()
        inputStreamReader.close()
        bufferedReader.close()
        return temp
    }

    private fun readFile(fileName: String): String {
        var result = ""
        try {
            val currentDir = System.getProperty("user.dir")//获取当前目录:例如D:\Project\MyApplication
            val file = File("${currentDir}/${fileName}.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            var fileInputStream: FileInputStream? = FileInputStream(file)
            val byteArray = fileInputStream?.readBytes()
            fileInputStream?.close()
            fileInputStream = null
            if (byteArray != null) {
                result = String(byteArray)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun writeFile(fileName: String, content: String) {
        val currentDir = System.getProperty("user.dir")//获取当前目录:例如D:\Project\MyApplication
        val file = File("${currentDir}/${fileName}.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        var fileOutputStream: FileOutputStream? = FileOutputStream(file)
        fileOutputStream?.write(content.toByteArray())
        fileOutputStream?.flush()
        fileOutputStream?.close()
        fileOutputStream = null
    }

    private fun createNewFile(index: Int, content: String) {
        val currentDir = System.getProperty("user.dir")//获取当前目录:例如D:\Project\MyApplication
        val fileDir = File("${currentDir}/html")
        if (!fileDir.exists()) {
            println("not exists ${fileDir.absolutePath} dir,create")
            fileDir.mkdirs()
        }
        val file = File("${fileDir.absolutePath}/company${index}.html")
        if (!file.exists()) {
            file.createNewFile()
        } else {
            println("exists ${file.absolutePath},skip")
            return
        }
        var fileOutputStream: FileOutputStream? = FileOutputStream(file)
        fileOutputStream?.write(content.toByteArray())
        fileOutputStream?.flush()
        fileOutputStream?.close()
        fileOutputStream = null
    }
}