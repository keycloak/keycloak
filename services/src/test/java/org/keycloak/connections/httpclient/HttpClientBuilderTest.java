package org.keycloak.connections.httpclient;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

public class HttpClientBuilderTest {

    @Test
    public void testDefaultBuilder() throws NoSuchFieldException, IllegalAccessException {
        CloseableHttpClient httpClient = new HttpClientBuilder().build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Default socket timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getSocketTimeout());
        Assert.assertEquals("Default connect timeout is -1 and can be converted by TimeUnit", -1, requestConfig.getConnectTimeout());
    }

    @Test
    public void testTimeUnitSeconds() throws NoSuchFieldException, IllegalAccessException {
        HttpClientBuilder httpClientBuilder = new HttpClientBuilder();
        httpClientBuilder
                .socketTimeout(2, TimeUnit.SECONDS)
                .establishConnectionTimeout(1, TimeUnit.SECONDS);
        CloseableHttpClient httpClient = httpClientBuilder.build();

        RequestConfig requestConfig = getRequestConfig(httpClient);

        Assert.assertEquals("Socket timeout is converted to milliseconds", 2000, requestConfig.getSocketTimeout());
        Assert.assertEquals("Connect timeout is converted to milliseconds", 1000, requestConfig.getConnectTimeout());
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
}
