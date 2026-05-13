package org.keycloak.testframework.server;

import java.net.HttpURLConnection;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Polls the server's endpoint until it reports ready, supporting both HTTP and HTTPS connections.
 */
public final class ReadinessProbe {

    private static final int CONNECTION_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofSeconds(5).toMillis());
    private static final long POLL_INTERVAL_MILLIS = Duration.ofMillis(500).toMillis();

    private ReadinessProbe() {
    }

    public static void waitUntilReady(KeycloakServer server, long timeout) {
        waitUntilReady(index -> server.getBaseUrl(), 1, timeout);
    }

    public static void waitUntilReady(IntFunction<String> baseUrlFunction, int clusterSize, long timeout) {
        var sslContext = createTrustAllSslContext();
        for (int i = 0; i < clusterSize; i++) {
            // can't use /health/ready has it is not enabled in most tests
            var url = baseUrlFunction.apply(i) + "/realms/master";
            waitUntilReady(url, sslContext, timeout);
        }
    }

    private static void waitUntilReady(String url, SSLContext sslContext, long timeout) {
        var deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout);
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                if (connection instanceof HttpsURLConnection https) {
                    https.setSSLSocketFactory(sslContext.getSocketFactory());
                    https.setHostnameVerifier((hostname, session) -> true);
                }
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
                connection.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);
                connection.setRequestMethod("GET");

                try {
                    if (connection.getResponseCode() == 200) {
                        return;
                    }
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                // server not yet available, retry
            }

            try {
                //noinspection BusyWait
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for server readiness", e);
            }
        }
        throw new IllegalStateException("Server did not become ready within " + timeout + " seconds: " + url);
    }

    private static SSLContext createTrustAllSslContext() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, null);
            return ctx;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create trust-all SSLContext", e);
        }
    }
}
