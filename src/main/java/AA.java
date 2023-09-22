import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class AA {
    public static void main(String...args) {
        // request("www.baidu.com", "/", 443);
        request("mzj.cq12349.cn", "/oss/sass/2023-09-12/image/f8576038f00a410286532b9265ee729f.png", 443);
    }

    /**
     *
     * @param host 域名
     * @param path 路径
     * @param port 端口，https：443
     */
    public static void request(String host, String path, int port) {
        try {
            //Https请求用SSLSocketFactory，如果是Http使用Socket
            Socket socket = SSLSocketFactory.getDefault().createSocket(host, port);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //注意\r\n代表换行
            String head = "GET "+path+" HTTP/1.1\r\n" +
                    "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7\r\n" +
                    "Accept-Encoding: gzip, deflate, br\r\n" +
                    "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Host: "+host+"\r\n"+
                    "\r\n";
            //这里第二个\r\n代表空行，空行是http协议里要求的，不能省略
            bw.write(head);
            bw.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String readLine = null;
                if ((readLine = br.readLine()) != null) {
                    System.out.println(readLine);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static SSLConnectionSocketFactory createSSLClientDefault() {
        try {
            //使用 loadTrustMaterial() 方法实现一个信任策略，信任所有证书
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            //NoopHostnameVerifier类:  作为主机名验证工具，实质上关闭了主机名验证，它接受任何
            //有效的SSL会话并匹配到目标主机。

            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            return sslsf;
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;

    }
}

