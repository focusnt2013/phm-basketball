import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.json.JSONObject;

public class ApiKit {
	public static final int CONNECTTIMEOUT = 7000;
	public static final int READTIMEOUT = 5000;

	/**
	 * @return
	 * @throws Exception
	 */
	public static String getReqUrl(String prefixUrl) throws Exception {
		final String privatekey = "-----BEGIN RSA PRIVATE KEY-----\n"+
			"MIIBOgIBAAJBAKdkYovmgHvxU0Vsz2ft7vVczspAk8jNpMxyQtNJ0ax6BzEZ+svY\n"+
			"R17QjOkGFAM1eZRfJq5rdny+hCYhse8I/9sCAwEAAQJAP0gsGUei+zhYir6ACoJg\n"+
			"/FGBu+R9+kQEMWZg7Q/TPKio1Hlfh50k2PZW5wFBa+PdeyVm9mrUWncx1ZILNw8u\n"+
			"QQIhANKVnsfCsmVrfV+4ojBRHEVauOBoGBdDP+181mjabTltAiEAy34dLD9TN5tC\n"+
			"JlsKnI3rzyQ16rrb0OkxwJqKWPCgWWcCIGxX2kdAXnRbpzd2UMu3D2qHUJL0O2DM\n"+
			"krlm/xEXQBbJAiEAiguc6MZwwrlNr811Lm1MujIbbYij1F5OBRYRonJipSMCIGHa\n"+
			"/YmAFOOo7HfaOui79Qa1ixzSNNaj+9tT3DK1PCf4\n"+
			"-----END RSA PRIVATE KEY-----";
		PEMParser parser = new PEMParser(new StringReader(privatekey));
		PEMKeyPair pari = (PEMKeyPair) parser.readObject();
		parser.close();
		byte[] pk1 = pari.getPrivateKeyInfo().getEncoded();
		String token = "hoops";
		long timestamp = System.currentTimeMillis();

		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String event_time = sdf.format(Calendar.getInstance().getTime());

		byte[] signautreResult = RsaKeyTools.sign(token + timestamp + event_time, pk1);
		String signature = RsaKeyTools.bytes2String(signautreResult);
		return String.format("%s/1.8/%s/%s/%s", prefixUrl, timestamp, event_time, signature);
	}

	/**
	 * 执行网络数据的提交
	 */
	public static JSONObject postJson(String url, byte[] writeBytes) throws Exception {
		return postJson(url, writeBytes, null);
	}

	static class miTM implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
			return true;
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
				throws java.security.cert.CertificateException {
			return;
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
				throws java.security.cert.CertificateException {
			return;
		}
	}

	public static JSONObject postJson(String url, byte[] writeBytes, String cookie) {
		HttpURLConnection con = null;
		JSONObject rsp;
		try {
			if (url.startsWith("https")) {
				javax.net.ssl.HostnameVerifier hv = new javax.net.ssl.HostnameVerifier() {
					public boolean verify(String urlHostName, javax.net.ssl.SSLSession session) {
						return true;
					}
				};
				javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
				javax.net.ssl.TrustManager tm = new miTM();
				trustAllCerts[0] = tm;
				javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, null);
				javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				HttpsURLConnection.setDefaultHostnameVerifier(hv);
			}
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setConnectTimeout(CONNECTTIMEOUT);
			con.setReadTimeout(READTIMEOUT);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Accept-Encoding", "gzip, deflate");
			con.setRequestProperty("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
			con.setRequestProperty("Content-Type", "application/json;charset=utf-8");
			con.setRequestProperty("Cache-Control", "max-age=0");
			con.setRequestProperty("Connection", "Keep-Alive");
			if (cookie != null) {
				con.setRequestProperty("Cookie", cookie);
			}
			con.connect();
			if (writeBytes != null) {
				con.getOutputStream().write(writeBytes);
			}
			if (con.getResponseCode() == 200) {
				InputStream is = con.getInputStream();
				boolean gzip = "gzip".equals(con.getHeaderField("Content-Encoding"));
				if (gzip)
					is = new GZIPInputStream(is);
				rsp = new JSONObject(new String(readAsByteArray(is), "UTF-8"));
				if (cookie == null) {
					Map<String, List<String>> fields = con.getHeaderFields();
					Iterator<String> iterator = fields.keySet().iterator();
					while (iterator.hasNext()) {
						String key = iterator.next();
						if (key == null)
							continue;
						List<String> list = fields.get(key);
						StringBuffer val = new StringBuffer();
						for (int i = 0; i < list.size(); i++) {
							if (i > 0) {
								val.append(",");
							}
							val.append(list.get(i));
						}
						rsp.put(key, val.toString());
					}
				}
			} else {
				rsp = new JSONObject();
				rsp.put("errcode", 0);
				byte[] rspbytes = readFullInputStream(con.getInputStream());
				rsp.put("errmsg", String.format("%s(%s)", new String(rspbytes, "UTF-8"), con.getResponseCode()));
				rsp.put("voice", "授权开放");
			}
		} catch (Exception e) {
			e.printStackTrace();
			rsp = new JSONObject();
			rsp.put("errcode", 0);
			rsp.put("errmsg", String.format("处理异常(%s)", e.toString()));
			rsp.put("voice", "授权开放");
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return rsp;
	}

	/**
	 * 读取输入流为二进制数组
	 *
	 * @param servletInputStream
	 * @return
	 */
	public static byte[] readFullInputStream(InputStream is) {
		List<byte[]> all = new ArrayList<byte[]>();
		int read = -1;
		int count = 0;
		byte[] catchs = new byte[1024];
		try {
			while ((read = is.read(catchs)) > -1) {
				count += read;
				byte[] data = new byte[read];
				System.arraycopy(catchs, 0, data, 0, read);
				all.add(data);
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] allDatas = new byte[count];
		int copyCounts = 0;
		for (byte[] b : all) {
			System.arraycopy(b, 0, allDatas, copyCounts, b.length);
			copyCounts += b.length;
		}

		return allDatas;
	}

	public static byte[] getByteArrayFromUrl(String urlStr) {
		URL url = null;
		HttpURLConnection connection = null;
		InputStream in = null;
		byte[] content = null;
		try {
			url = new URL(urlStr);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(CONNECTTIMEOUT);
			connection.setReadTimeout(READTIMEOUT);
			// 连接
			connection.connect();
			int code = connection.getResponseCode();

			if (code > 400) {
				throw new RuntimeException(String.valueOf(code));
			}
			// 如果返回状态码200
			if (200 == code) {
				// 读取返回数据
				in = connection.getInputStream();
				content = IOUtils.toByteArray(in);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException("不合法的http地址", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("读取数据失败", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("关闭流失败", e);
				}
			}
		}
		return content;
	}

	/**
	 * 得到
	 *
	 * @param path
	 * @param decrypte
	 * @return
	 */
	public static JSONObject getJSONObject(ZooKeeper zookeeper, String path) {
		try {
			byte[] payload = null;
			if (zookeeper != null) {
				Stat stat = zookeeper.exists(path, false);
				payload = zookeeper.getData(path, false, stat);
			} else {
				File file = new File(String.format("../data%s/json.txt", path));
				if (file.exists()) {
					payload = readAsByteArray(file);
				}
			}
			if (payload != null) {
				return new JSONObject(new String(payload, "UTF-8"));
			}
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * 上传二进制照片
	 * 
	 * @title
	 * @methodName
	 * @author wangyi
	 * @description
	 * @param string 通过getReqUrl升成的URL
	 * @param file  上传文件的数据
	 * @param filename 上传文件名（随便）
	 * @param hashMap
	 * @return
	 * @throws Exception
	 */
	public static JSONObject postImage(String serverUrl, byte[] file, String filename, Map<String, Object> params)
			throws Exception {
		String respStr = null;
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {

			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		} }, new java.security.SecureRandom());
		CloseableHttpClient httpclient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.setSSLContext(sslContext).setSSLHostnameVerifier(new NoopHostnameVerifier()).build();
		try {

			HttpPost httppost = new HttpPost(serverUrl);
			MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
			multipartEntityBuilder.addBinaryBody("file", new ByteArrayInputStream(file),
					ContentType.create("multipart/form-data"), filename);
			// 设置上传的其他参数
			setUploadParams(multipartEntityBuilder, params);
			HttpEntity reqEntity = multipartEntityBuilder.build();
			httppost.setEntity(reqEntity);
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				HttpEntity resEntity = response.getEntity();
				respStr = getRespString(resEntity);
				EntityUtils.consume(resEntity);
			} finally {
				response.close();
			}
			return new JSONObject(respStr);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 设置上传文件时所附带的其他参数
	 * 
	 * @param multipartEntityBuilder
	 * @param params
	 * @throws UnsupportedEncodingException
	 */
	public static void setUploadParams(MultipartEntityBuilder multipartEntityBuilder, Map<String, Object> params)
			throws UnsupportedEncodingException {
		if (params != null && params.size() > 0) {
			Set<String> keys = params.keySet();
			for (String key : keys) {
				multipartEntityBuilder.addPart(key, new StringBody(params.get(key).toString(), ContentType.TEXT_PLAIN));
			}
		}
	}
	/**
	 * 将返回结果转化为String
	 * 
	 * @param entity
	 * @return
	 * @throws Exception
	 */
	public static String getRespString(HttpEntity entity) throws Exception {
		if (entity == null) {
			return null;
		}
		InputStream is = entity.getContent();
		StringBuffer strBuf = new StringBuffer();
		byte[] buffer = new byte[4096];
		int r = 0;
		while ((r = is.read(buffer)) > 0) {
			strBuf.append(new String(buffer, 0, r, "UTF-8"));
		}
		return strBuf.toString();
	}

	/**
	 * 还原Unicode编码的字符串
	 * 
	 * @param src
	 * @return String
	 * @author Focus
	 */
	public static String unicode2Chr(String src) {
		if (src == null || src.isEmpty()) {
			return src;
		}
		String tempStr = "";
		String returnStr = "";

		// 将编码过的字符串进行重排
		for (int i = 0; i < src.length() / 4; i++) {
			if (0 == i) {
				tempStr = src.substring(4 * i + 2, 4 * i + 4);
				tempStr += src.substring(4 * i, 4 * i + 2);
			} else {
				tempStr += src.substring(4 * i + 2, 4 * i + 4);
				tempStr += src.substring(4 * i, 4 * i + 2);
			}
		}

		byte[] b = new byte[tempStr.length() / 2];

		try {
			// 将重排过的字符串放入byte数组，用于进行转码
			for (int j = 0; j < tempStr.length() / 2; j++) {
				String subStr = tempStr.substring(j * 2, j * 2 + 2);
				int b1 = Integer.decode("0x" + subStr).intValue();
				b[j] = (byte) b1;
			}
			// 转码
			returnStr = new String(b, "utf-16");
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		} catch (Exception e) {
			return src;
		}
		return returnStr;
	}

	/**
	 * 字符串准成unicode
	 * 
	 * @param src
	 * @return
	 */
	public static String chr2Unicode(String src) {
		StringBuffer s = new StringBuffer();
		if (src != null && !"".equals(src)) {
			String hex = "";

			for (int i = 0; i < src.length(); i++) {
				hex = Integer.toHexString((int) src.charAt(i));
				if (hex.length() < 4) {
					while (hex.length() < 4) {
						hex = "0".concat(hex);
					}
				}
				hex = hex.substring(2, 4).concat(hex.substring(0, 2));
				s.append(hex.toUpperCase());
			}
		}
		return s.toString();
	}

	public static final byte[] readAsByteArray(File file) {
		if (file != null && file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				byte[] buffer = new byte[fis.available()];
				fis.read(buffer);
				fis.close();
				return buffer;
			} catch (Exception e) {
				return new byte[0];
			}
		} else {
			return new byte[0];
		}
	}

	public static final byte[] readAsByteArray(InputStream is) throws Exception {
		List<byte[]> all = new ArrayList<byte[]>();
		int read = -1;
		int count = 0;
		byte[] catchs = new byte[65536];
		try {
			while ((read = is.read(catchs)) > -1) {
				count += read;
				byte[] data = new byte[read];
				System.arraycopy(catchs, 0, data, 0, read);
				all.add(data);
			}
		} catch (IOException e) {
		} finally {
			is.close();
		}

		byte[] allDatas = new byte[count];
		int copyCounts = 0;
		for (byte[] b : all) {
			System.arraycopy(b, 0, allDatas, copyCounts, b.length);
			copyCounts += b.length;
		}

		return allDatas;
	}
}
