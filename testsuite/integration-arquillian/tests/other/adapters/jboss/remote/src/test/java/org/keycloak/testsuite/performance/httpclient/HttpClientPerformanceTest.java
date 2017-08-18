package org.keycloak.testsuite.performance.httpclient;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.After;
import org.junit.Before;
import org.keycloak.testsuite.performance.PerformanceTest;

import java.io.IOException;

import static org.keycloak.testsuite.performance.PerformanceTest.MAX_THREADS;

/**
 *
 * @author tkyjovsk
 */
public abstract class HttpClientPerformanceTest extends PerformanceTest {

    protected CloseableHttpClient client;

    public static final Integer HTTP_CLIENT_SOCKET_TIMEOUT = Integer.parseInt(System.getProperty("httpclient.socket.timeout", "10000"));
    public static final Integer HTTP_CLIENT_CONNECT_TIMEOUT = Integer.parseInt(System.getProperty("httpclient.connect.timeout", "10000"));
    public static final Integer HTTP_CLIENT_CONNECTION_REQUEST_TIMEOUT = Integer.parseInt(System.getProperty("httpclient.connection-request.timeout", "10000"));

    @Before
    public void initializeClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(Math.max(1, MAX_THREADS / 10));
        connectionManager.setDefaultMaxPerRoute(connectionManager.getMaxTotal());
        connectionManager.setValidateAfterInactivity(10000);
        connectionManager.setDefaultSocketConfig(getDefaultSocketConfig());

        client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(new BasicCookieStore())
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .setRedirectStrategy(new CustomRedirectStrategy())
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
                .build();
    }

    protected SocketConfig getDefaultSocketConfig() {
        return SocketConfig.copy(SocketConfig.DEFAULT)
                .setSoTimeout(HTTP_CLIENT_SOCKET_TIMEOUT).build();
    }

    protected RequestConfig getDefaultRequestConfig() {
        return RequestConfig.custom()
                .setSocketTimeout(HTTP_CLIENT_SOCKET_TIMEOUT)
                .setConnectTimeout(HTTP_CLIENT_CONNECT_TIMEOUT)
                .setConnectionRequestTimeout(HTTP_CLIENT_CONNECTION_REQUEST_TIMEOUT)
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setRedirectsEnabled(true)
                .setRelativeRedirectsAllowed(true)
                .setCircularRedirectsAllowed(false)
                .setMaxRedirects(2)
                .build();
    }

    public class CustomRedirectStrategy extends DefaultRedirectStrategy {

        private final String[] REDIRECT_METHODS;

        public CustomRedirectStrategy() {
            this.REDIRECT_METHODS = new String[]{
                HttpGet.METHOD_NAME,
                HttpPost.METHOD_NAME,
                HttpHead.METHOD_NAME,
                HttpDelete.METHOD_NAME,
                HttpOptions.METHOD_NAME
            };
        }

        @Override
        protected boolean isRedirectable(String method) {
            for (final String m : REDIRECT_METHODS) {
                if (m.equalsIgnoreCase(method)) {
                    return true;
                }
            }
            return false;
        }

    }

    @After
    public void closeClient() throws IOException {
        client.close();
    }


    public abstract class Runnable extends PerformanceTest.Runnable {

        protected HttpClientContext context;

        public Runnable() {
            this.context = HttpClientContext.create();
            this.context.setCookieStore(new BasicCookieStore());
        }

    }

}
