package com.focusnt.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

/**
 * Created by focus lau on 2015/6/10.
 * 
 */
public class RsaKeyTools {
	/**
	 * Convert private key from PKCS8 to PKCS1: Convert private key in PKCS1 to PEM:
	 * 
	 * @param privateKey
	 * @return
	 * @throws IOException
	 */
	public static String getPemPrivateKey(Key privateKey) throws IOException {

		PrivateKeyInfo pkInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
		ASN1Encodable encodable = pkInfo.parsePrivateKey();
		ASN1Primitive primitive = encodable.toASN1Primitive();
		byte[] privateKeyPKCS1 = primitive.getEncoded();

		PemObject pemObject = new PemObject("RSA PRIVATE KEY", privateKeyPKCS1);
		StringWriter stringWriter = new StringWriter();
		PemWriter pemWriter = new PemWriter(stringWriter);
		pemWriter.writeObject(pemObject);
		pemWriter.close();
		return stringWriter.toString();
	}

	/**
	 * Convert public key from X.509 SubjectPublicKeyInfo to PKCS1: Convert public
	 * key in PKCS1 to PEM:
	 * 
	 * @param privateKey
	 * @return
	 * @throws IOException
	 */
	public static String getPemPublicKey(Key publicKey) throws IOException {

		SubjectPublicKeyInfo spkInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		ASN1Primitive primitive = spkInfo.parsePublicKey();
		byte[] publicKeyPKCS1 = primitive.getEncoded();

		PemObject pemObject = new PemObject("CERTIFICATE", publicKeyPKCS1);
		StringWriter stringWriter = new StringWriter();
		PemWriter pemWriter = new PemWriter(stringWriter);
		pemWriter.writeObject(pemObject);
		pemWriter.close();
		return stringWriter.toString();
	}

	public static byte[] sign(String data, byte[] privateKey) throws Exception {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey2 = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		Signature signature = Signature.getInstance("SHA1WithRSA");
		signature.initSign(privateKey2);
		signature.update(data.getBytes());
		return signature.sign();

	}

	// 后台测试签名的时候 要和前台保持一致，所以需要将结果转换
	public static String bytes2String(byte[] bytes) {
		StringBuilder string = new StringBuilder();
		for (byte b : bytes) {
			String hexString = Integer.toHexString(0x00FF & b);
			string.append(hexString.length() == 1 ? "0" + hexString : hexString);
		}
		return string.toString();
	}

	public static boolean verify(String data, byte[] publicKey, byte[] signatureResult) {
		try {
			X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey2 = keyFactory.generatePublic(x509EncodedKeySpec);

			Signature signature = Signature.getInstance("SHA1WithRSA");
			signature.initVerify(publicKey2);
			signature.update(data.getBytes());

			return signature.verify(signatureResult);
		} catch (Exception e) {
		}
		return false;
	}

	// 前台的签名结果是将byte 中的一些 负数转换成了正数，
	// 但是后台验证的方法需要的又必须是转换之前的
	public static byte[] hexStringToByteArray(String data) {
		int k = 0;
		byte[] results = new byte[data.length() / 2];
		for (int i = 0; i + 1 < data.length(); i += 2, k++) {
			results[k] = (byte) (Character.digit(data.charAt(i), 16) << 4);
			results[k] += (byte) (Character.digit(data.charAt(i + 1), 16));
		}
		return results;
	}

	public static void main(String[] args) {
		String str = "test2017-11-29 01:23:174945bd16003a5";

		try {
			KeyPairGenerator keyPair = KeyPairGenerator.getInstance("RSA");
			SecureRandom random = new SecureRandom();
			keyPair.initialize(512, random);
			KeyPair keyP = keyPair.generateKeyPair();
			Key publicKey = keyP.getPublic();
			Key privateKey = keyP.getPrivate();

			String privatekey = getPemPrivateKey(privateKey);
			String publickey = getPemPublicKey(publicKey);
			System.out.println(privatekey);
			System.out.println(publickey);

			// privatekey = privatekey.substring("-----BEGIN RSA PRIVATE
			// KEY-----\r\n".length());
			PEMParser parser = new PEMParser(new StringReader(privatekey));
			PEMKeyPair pari = (PEMKeyPair) parser.readObject();
			parser.close();
			byte[] pk1 = pari.getPrivateKeyInfo().getEncoded();
			com.focus.util.Tools.printb(pk1);
			byte[] pk0 = privateKey.getEncoded();
			com.focus.util.Tools.printb(pk0);

			byte[] signautreResult = sign(str, pk1);
			String signatureStr = bytes2String(signautreResult);
			System.err.println("signatureStr:" + signatureStr);
			byte[] signatureResult2 = hexStringToByteArray(signatureStr);
			boolean b = verify(str, publicKey.getEncoded(), signatureResult2);
			System.out.print("签名延签结果:   " + b);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String sign2Str(String data, byte[] privateKey) {
		try {
			byte[] signData = sign(data, privateKey);
			return bytes2String(signData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
