package org.keycloak.testframework.http;

import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;

import org.apache.http.client.HttpClient;

public class SimpleHttpSupplier implements Supplier<SimpleHttp, InjectSimpleHttp> {

    @Override
    public SimpleHttp getValue(InstanceContext<SimpleHttp, InjectSimpleHttp> instanceContext) {
        HttpClient httpClient = instanceContext.getDependency(HttpClient.class);
        return SimpleHttp.create(httpClient);
    }

    @Override
    public boolean compatible(InstanceContext<SimpleHttp, InjectSimpleHttp> a, RequestedInstance<SimpleHttp, InjectSimpleHttp> b) {
        return true;
    }

}
