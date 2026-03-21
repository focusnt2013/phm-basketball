package com.smartbasketball.app.util

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.zip.GZIPInputStream
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.json.JSONObject

object ApiKit {
    const val CONNECTTIMEOUT = 7000
    const val READTIMEOUT = 5000
    
    private val privateKey = """
-----BEGIN RSA PRIVATE KEY-----
MIIBOgIBAAJBAKdkYovmgHvxU0Vsz2ft7vVczspAk8jNpMxyQtNJ0ax6BzEZ+svY
R17QjOkGFAM1eZRfJq5rdny+hCYhse8I/9sCAwEAAQJAP0gsGUei+zhYir6ACoJg
/FGBu+R9+kQEMWZg7Q/TPKio1Hlfh50k2PZW5wFBa+PdeyVm9mrUWncx1ZILNw8u
QQIhANKVnsfCsmVrfV+4ojBRHEVauOBoGBdDP+181mjabTltAiEAy34dLD9TN5tC
JlsKnI3rzyQ16rrb0OkxwJqKWPCgWWcCIGxX2kdAXnRbpzd2UMu3D2qHUJL0O2DM
krlm/xEXQBbJAiEAiguc6MZwwrlNr811Lm1MujIbbYij1F5OBRYRonJipSMCIGHa
/YmAFOOo7HfaOui79Qa1ixzSNNaj+9tT3DK1PCf4
-----END RSA PRIVATE KEY-----
""".trimIndent()

    @Throws(Exception::class)
    fun getReqUrl(prefixUrl: String): String {
        val parser = PEMParser(StringReader(privateKey))
        val pari = parser.readObject() as PEMKeyPair
        parser.close()
        val pk1 = pari.privateKeyInfo.encoded
        val token = "hoops"
        val timestamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyyMMddHHmmss")
        val eventTime = sdf.format(Calendar.getInstance().time)

        val signautreResult = RsaKeyTools.sign(token + timestamp + eventTime, pk1)
        val signature = RsaKeyTools.bytes2String(signautreResult)
        val fullUrl = String.format("%s/1.8/%s/%s/%s", prefixUrl, timestamp, eventTime, signature)
        
        AppLogger.d("========== 生成请求URL ==========")
        AppLogger.d("接口URL: $fullUrl")
        
        return fullUrl
    }

    @Throws(Exception::class)
    fun postJson(url: String, writeBytes: ByteArray?): JSONObject {
        return postJson(url, writeBytes, null)
    }

    fun postJson(url: String, writeBytes: ByteArray?, cookie: String?): JSONObject {
        var con: HttpURLConnection? = null
        var rsp: JSONObject = JSONObject()
        
        AppLogger.d("========== 发送POST请求 ==========")
        AppLogger.d("请求URL: $url")
        if (writeBytes != null) {
            val requestBody = String(writeBytes, Charsets.UTF_8)
            AppLogger.d("请求Body: $requestBody")
        }
        
        try {
            if (url.startsWith("https")) {
                val hv = javax.net.ssl.HostnameVerifier { _, _ -> true }
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                    override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                })
                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, null)
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(hv)
            }
            con = URL(url).openConnection() as HttpURLConnection
            con.connectTimeout = CONNECTTIMEOUT
            con.readTimeout = READTIMEOUT
            con.requestMethod = "POST"
            con.doOutput = true
            con.setRequestProperty("Accept", "application/json")
            con.setRequestProperty("Accept-Encoding", "gzip, deflate")
            con.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3")
            con.setRequestProperty("Content-Type", "application/json;charset=utf-8")
            con.setRequestProperty("Cache-Control", "max-age=0")
            con.setRequestProperty("Connection", "Keep-Alive")
            if (cookie != null) {
                con.setRequestProperty("Cookie", cookie)
            }
            con.connect()
            if (writeBytes != null) {
                con.outputStream.write(writeBytes)
            }
            if (con.responseCode == 200) {
                var inputStream: InputStream = con.inputStream
                val gzip = "gzip" == con.getHeaderField("Content-Encoding")
                if (gzip) {
                    inputStream = GZIPInputStream(inputStream)
                }
                rsp = JSONObject(String(readAsByteArray(inputStream), Charsets.UTF_8))
            } else {
                rsp = JSONObject()
                rsp.put("errcode", 0)
                rsp.put("errmsg", "${String(readAsByteArray(con.errorStream), Charsets.UTF_8)}(${con.responseCode})")
                rsp.put("voice", "授权开放")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            rsp = JSONObject()
            rsp.put("errcode", 0)
            rsp.put("errmsg", "处理异常(${e.message})")
            rsp.put("voice", "授权开放")
        } finally {
            con?.disconnect()
        }
        return rsp
    }

    fun getByteArrayFromUrl(urlStr: String): ByteArray? {
        return try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTTIMEOUT
            connection.readTimeout = READTIMEOUT
            connection.connect()
            val code = connection.responseCode
            if (code == 200) {
                connection.inputStream.use { it.readBytes() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readAsByteArray(file: File): ByteArray {
        return if (file.exists()) {
            FileInputStream(file).use { it.readBytes() }
        } else {
            ByteArray(0)
        }
    }

    @Throws(Exception::class)
    fun readAsByteArray(inputStream: InputStream): ByteArray {
        return inputStream.use { it.readBytes() }
    }

    @Throws(Exception::class)
    fun postImage(serverUrl: String, file: ByteArray, filename: String, params: Map<String, Any>): JSONObject {
        AppLogger.d("========== postImage上传图片 ==========")
        AppLogger.d("URL: $serverUrl, 文件大小: ${file.size}, 文件名: $filename")
        
        val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
        val lineEnd = "\r\n"
        
        try {
            if (serverUrl.startsWith("https")) {
                val hv = javax.net.ssl.HostnameVerifier { _, _ -> true }
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                    override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                })
                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAllCerts, null)
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(hv)
            }
            
            val connection = URL(serverUrl).openConnection() as HttpsURLConnection
            connection.connectTimeout = CONNECTTIMEOUT
            connection.readTimeout = READTIMEOUT
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            
            val outputStream = java.io.DataOutputStream(connection.outputStream)
            
            // 添加params参数
            for ((key, value) in params) {
                outputStream.writeBytes("--$boundary$lineEnd")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd$lineEnd")
                outputStream.writeBytes("$value$lineEnd")
            }
            
            // 添加文件
            outputStream.writeBytes("--$boundary$lineEnd")
            outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"$lineEnd")
            outputStream.writeBytes("Content-Type: image/jpeg$lineEnd$lineEnd")
            outputStream.write(file)
            outputStream.writeBytes(lineEnd)
            
            // 结束
            outputStream.writeBytes("--$boundary--$lineEnd")
            outputStream.flush()
            outputStream.close()
            
            val responseCode = connection.responseCode
            val responseStr = if (responseCode == 200) {
                String(connection.inputStream.readBytes(), Charsets.UTF_8)
            } else {
                String(connection.errorStream?.readBytes() ?: ByteArray(0), Charsets.UTF_8)
            }
            
            AppLogger.d("postImage响应: $responseStr")
            connection.disconnect()
            
            return JSONObject(responseStr)
        } catch (e: Exception) {
            AppLogger.e("postImage异常: ${e.message}")
            val rsp = JSONObject()
            rsp.put("errcode", -1)
            rsp.put("errmsg", "上传异常(${e.message})")
            return rsp
        }
    }
}
