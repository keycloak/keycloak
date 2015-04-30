package org.keycloak.connections.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.EnvUtil;
import org.keycloak.util.KeystoreUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultHttpClientFactory implements HttpClientFactory {

    private static final Logger logger = Logger.getLogger(DefaultHttpClientFactory.class);

    private volatile CloseableHttpClient httpClient;

    @Override
    public HttpClientProvider create(KeycloakSession session) {
        return new HttpClientProvider() {
            @Override
            public HttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public void close() {

            }

            @Override
            public int postText(String uri, String text) throws IOException {
                HttpPost request = new HttpPost(uri);
                request.setEntity(EntityBuilder.create().setText(text).setContentType(ContentType.TEXT_PLAIN).build());
                HttpResponse response = httpClient.execute(request);
                try {
                    return response.getStatusLine().getStatusCode();
                } finally {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        InputStream is = entity.getContent();
                        if (is != null) is.close();
                    }

                }
            }

            @Override
            public InputStream get(String uri) throws IOException {
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpClient.execute(request);
                HttpEntity entity = response.getEntity();
                if (entity == null) return null;
                return entity.getContent();

            }
        };
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {

        }
    }

    @Override
    public String getId() {
        return "default";
    }

    @Override
    public void init(Config.Scope config) {
        long socketTimeout = config.getLong("socketTimeoutMillis", -1L);
        long establishConnectionTimeout = config.getLong("establishConnectionTimeoutMillis", -1L);
        long connectionTTL = config.getLong("connectionTTLMillis", -1L);
        int maxPooledPerRoute = config.getInt("maxPooledPerRoute", 0);
        int connectionPoolSize = config.getInt("connectionPoolSize", 200);
        boolean disableTrustManager = config.getBoolean("disableTrustManager", true);
        boolean disableCookies = config.getBoolean("disableCookies", true);
        String hostnameVerificationPolicy = config.get("hostnameVerificationPolicy", "WILDCARD");
        HttpClientBuilder.HostnameVerificationPolicy hostnamePolicy = HttpClientBuilder.HostnameVerificationPolicy.valueOf(hostnameVerificationPolicy);
        String truststore = config.get("truststore");
        String truststorePassword = config.get("truststorePassword");
        String clientKeystore = config.get("clientKeyStore");
        String clientKeystorePassword = config.get("clientKeyStorePassword");
        String clientPrivateKeyPassword = config.get("clientPrivateKeyPassword");

        HttpClientBuilder builder = new HttpClientBuilder();
        builder.socketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
               .establishConnectionTimeout(establishConnectionTimeout, TimeUnit.MILLISECONDS)
               .connectionTTL(connectionTTL, TimeUnit.MILLISECONDS)
                .maxPooledPerRoute(maxPooledPerRoute)
                .connectionPoolSize(connectionPoolSize)
                .hostnameVerification(hostnamePolicy)
                .disableCookies(disableCookies);
        if (disableTrustManager) builder.disableTrustManager();
        if (truststore != null) {
            truststore = EnvUtil.replace(truststore);
            try {
                builder.trustStore(KeystoreUtil.loadKeyStore(truststore, truststorePassword));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
        }
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            try {
                KeyStore clientCertKeystore = KeystoreUtil.loadKeyStore(clientKeystore, clientKeystorePassword);
                builder.keyStore(clientCertKeystore, clientPrivateKeyPassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
        }
        httpClient = builder.build();
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }



}
