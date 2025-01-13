package org.keycloak.testframework.http;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

import java.io.IOException;

public class HttpClientSupplier implements Supplier<HttpClient, InjectHttpClient> {

    @Override
    public Class<InjectHttpClient> getAnnotationClass() {
        return InjectHttpClient.class;
    }

    @Override
    public Class<HttpClient> getValueType() {
        return HttpClient.class;
    }

    @Override
    public HttpClient getValue(InstanceContext<HttpClient, InjectHttpClient> instanceContext) {
        return HttpClientBuilder.create().build();
    }

    @Override
    public void close(InstanceContext<HttpClient, InjectHttpClient> instanceContext) {
        try {
            ((CloseableHttpClient) instanceContext.getValue()).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean compatible(InstanceContext<HttpClient, InjectHttpClient> a, RequestedInstance<HttpClient, InjectHttpClient> b) {
        return false;
    }
}
