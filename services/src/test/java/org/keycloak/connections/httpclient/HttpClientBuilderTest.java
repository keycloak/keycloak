package org.keycloak.connections.httpclient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class HttpClientBuilderTest {

    @Test
    public void testDefaultBuilder() throws NoSuchFieldException, IllegalAccessException {
        CloseableHttpClient httpClient = new HttpClientBuilder().build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Default socket timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getSocketTimeout());
        Assert.assertEquals("Default connect timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getConnectTimeout());
        Assert.assertEquals("Default connection request timeout is " + HttpClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS, HttpClientBuilder.DEFAULT_CONNECTION_REQUEST_TIMEOUT_MILLIS, requestConfig.getConnectionRequestTimeout());
    }

    @Test
    public void testRepeatedApacheBuilderUse() throws Exception {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder.getApacheHttpClientBuilder().setRetryHandler(new HttpRequestRetryHandler() {

            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                return true;
            }
        });
        var builder = httpClientBuilder.getApacheHttpClientBuilder(); // should not clear the retry handler

        Field retryHandler = builder.getClass().getDeclaredField("retryHandler");
        retryHandler.setAccessible(true);
        assertNotNull(retryHandler.get(builder));
    }

    @Test
    public void testTimeUnitSeconds() throws NoSuchFieldException, IllegalAccessException {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder
                .socketTimeout(2, TimeUnit.SECONDS)
                .establishConnectionTimeout(1, TimeUnit.SECONDS)
                .connectionRequestTimeout(3, TimeUnit.SECONDS);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Socket timeout is converted to milliseconds", 2000, requestConfig.getSocketTimeout());
        Assert.assertEquals("Connect timeout is converted to milliseconds", 1000, requestConfig.getConnectTimeout());
        Assert.assertEquals("Connection request timeout is converted to milliseconds", 3000, requestConfig.getConnectionRequestTimeout());
    }

    @Test
    public void testTimeUnitMilliSeconds() throws NoSuchFieldException, IllegalAccessException {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder
                .socketTimeout(2000, TimeUnit.MILLISECONDS)
                .establishConnectionTimeout(1000, TimeUnit.MILLISECONDS);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Socket timeout is still in milliseconds", 2000, requestConfig.getSocketTimeout());
        Assert.assertEquals("Connect timeout is still in milliseconds", 1000, requestConfig.getConnectTimeout());
    }

    private static RequestConfig getRequestConfig(CloseableHttpClient httpClient) throws NoSuchFieldException, IllegalAccessException {
        Field defaultConfig = httpClient.getClass().getDeclaredField("defaultConfig");
        defaultConfig.setAccessible(true);
        return (RequestConfig) defaultConfig.get(httpClient);
    }

    /**
     * Regression test: when {@code disable-trust-manager} is set, the custom client TLS key material for
     * tls_client_auth (mTLS) must still be presented. Previously the builder created a fresh SSLContext
     * with no key managers in that case, silently dropping the certificate.
     */
    @Test
    public void keyManagersSurviveDisableTrustManager() throws Exception {
        KeyManager[] keyManagers = { new NoopKeyManager() };

        HttpClientBuilder builder = new HttpClientBuilder()
                .keyManagers(keyManagers)
                .disableTrustManager();

        // The key material must not be dropped when trust management is disabled.
        assertSame("Client key managers must survive disable-trust-manager",
                keyManagers, builder.effectiveKeyManagers());

        // And the resulting context must be built successfully with that key material in place.
        SSLContext ctx = builder.resolveSslContext();
        assertNotNull("A usable SSLContext must be produced with disable-trust-manager and mTLS key material", ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    /**
     * Sanity check that key managers are also honored on the normal (trust-manager-enabled) path.
     */
    @Test
    public void keyManagersAreHonoredOnDefaultPath() throws Exception {
        KeyManager[] keyManagers = { new NoopKeyManager() };

        HttpClientBuilder builder = new HttpClientBuilder().keyManagers(keyManagers);

        assertSame(keyManagers, builder.effectiveKeyManagers());

        SSLContext ctx = builder.resolveSslContext();
        assertNotNull(ctx);
        assertNotNull(ctx.getSocketFactory());
    }

    /**
     * Minimal X509KeyManager placeholder used to verify the builder retains configured key material.
     */
    private static class NoopKeyManager implements X509KeyManager {
        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[0];
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[0];
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[0];
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return null;
        }
    }

}
