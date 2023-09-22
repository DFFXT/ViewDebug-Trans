package com.example.viewdebugtrans.socket.core

import java.io.ByteArrayOutputStream
import javax.net.ssl.SSLSocketFactory


/**
 * 短链接，使用后立即关闭
 * socket协议
 * 请求：
 * 4字节，路由id长度
 * 路由id内容
 * 4字节，body长度
 * body内容
 *
 * 返回：
 * 4字节 body长度
 * body内容
 */
object HttpSocket {

    @JvmStatic
    fun main(vararg args: String) {
         send("mzj.cq12349.cn", "/oss/sass/2023-09-12/image/f8576038f00a410286532b9265ee729f.png", 443)
    }

    fun send(host: String, path: String, port: Int): ByteArray {


        val socket = SSLSocketFactory.getDefault().createSocket(host, port)
        val out = socket.getOutputStream()
        val input = socket.getInputStream()
        val requestHead = "GET $path HTTP/1.1\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\r\n" +
                "Accept-Encoding: gzip, deflate, br\r\n" +
                "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Connection: keep-alive\r\n" +
                "Host: mzj.cq12349.cn\r\n\r\n"
               // "scheme: https\r\n\r\n"
              /*  "If-Modified-Since: Tue, 12 Sep 2023 02:10:59 GMT\r\n" +
                "If-None-Match: \"89A59A488DF2CE7C2AB329F0EEC75AAE\"\r\n" +*/
               /* "Sec-Fetch-Dest: document\r\n" +
                "Sec-Fetch-Mode: navigate\r\n" +
                "Sec-Fetch-Site: none\r\n" +
                "Sec-Fetch-User: ?1\r\n" +
                "Upgrade-Insecure-Requests: 1\r\n" +
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36 Edg/116.0.1938.76\r\n" +
                "sec-ch-ua: \"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Microsoft Edge\";v=\"116\"\r\n" +
                "sec-ch-ua-mobile: ?0\r\n" +
                "sec-ch-ua-platform: \"Windows\"\r\n\r\n"*/

        out.write(requestHead.toByteArray())
        out.flush()
        val b = ByteArray(10240)
        val builder = ByteArrayOutputStream()
        while (true) {
            val len = input.read(b)
            if (len != -1) {
                builder.write(b, 0, len)
                println(String((builder.toByteArray())))
            } else {
                break
            }
        }



        // 关闭来连接
        socket.shutdownOutput()
        socket.shutdownInput()
        socket.close()
        return "".toByteArray()
    }
}
