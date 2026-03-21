package com.smartbasketball.app.util

import java.io.StringReader
import java.io.StringWriter
import java.security.Key
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter

object RsaKeyTools {
    
    @Throws(java.io.IOException::class)
    fun getPemPrivateKey(privateKey: Key): String {
        val pkInfo = PrivateKeyInfo.getInstance(privateKey.encoded)
        val encodable = pkInfo.parsePrivateKey()
        val primitive = encodable.toASN1Primitive()
        val privateKeyPKCS1 = primitive.encoded

        val pemObject = PemObject("RSA PRIVATE KEY", privateKeyPKCS1)
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)
        pemWriter.writeObject(pemObject)
        pemWriter.close()
        return stringWriter.toString()
    }

    @Throws(java.io.IOException::class)
    fun getPemPublicKey(publicKey: Key): String {
        val spkInfo = SubjectPublicKeyInfo.getInstance(publicKey.encoded)
        val primitive = spkInfo.parsePublicKey()
        val publicKeyPKCS1 = primitive.encoded

        val pemObject = PemObject("CERTIFICATE", publicKeyPKCS1)
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)
        pemWriter.writeObject(pemObject)
        pemWriter.close()
        return stringWriter.toString()
    }

    @Throws(Exception::class)
    fun sign(data: String, privateKey: ByteArray): ByteArray {
        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(privateKey)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec)
        val signature = Signature.getInstance("SHA1WithRSA")
        signature.initSign(privateKey2)
        signature.update(data.toByteArray())
        return signature.sign()
    }

    fun bytes2String(bytes: ByteArray): String {
        val string = StringBuilder()
        for (b in bytes) {
            val hexString = Integer.toHexString(0x00FF and b.toInt())
            string.append(if (hexString.length == 1) "0$hexString" else hexString)
        }
        return string.toString()
    }

    fun verify(data: String, publicKey: ByteArray, signatureResult: ByteArray): Boolean {
        return try {
            val x509EncodedKeySpec = X509EncodedKeySpec(publicKey)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec)

            val signature = Signature.getInstance("SHA1WithRSA")
            signature.initVerify(publicKey2)
            signature.update(data.toByteArray())

            signature.verify(signatureResult)
        } catch (e: Exception) {
            false
        }
    }

    fun hexStringToByteArray(data: String): ByteArray {
        var k = 0
        val results = ByteArray(data.length / 2)
        var i = 0
        while (i + 1 < data.length) {
            results[k] = (Character.digit(data[i], 16) shl 4).toByte()
            results[k] = (results[k] + Character.digit(data[i + 1], 16)).toByte()
            i += 2
            k++
        }
        return results
    }

    fun sign2Str(data: String, privateKey: ByteArray): String {
        return try {
            val signData = sign(data, privateKey)
            bytes2String(signData)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
