package org.keycloak.testframework.oauth;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.StringUtil;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.openqa.selenium.WebDriver;

public class OAuthClientSupplier implements Supplier<OAuthClient, InjectOAuthClient> {

    @Override
    public OAuthClient getValue(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        InjectOAuthClient annotation = instanceContext.getAnnotation();

        String clientId = StringUtil.convertEmptyToNull(annotation.clientId());
        String clientSecret = StringUtil.convertEmptyToNull(annotation.clientSecret());

        KeycloakUrls keycloakUrls = instanceContext.getDependency(KeycloakUrls.class);
        CloseableHttpClient httpClient = (CloseableHttpClient) instanceContext.getDependency(HttpClient.class);
        WebDriver webDriver = instanceContext.getDependency(WebDriver.class);

        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, annotation.realmRef());
//        ClientConfig clientConfig = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());

        if ("".equals(annotation.clientId())) {
            ManagedClient managedClient = instanceContext.getDependency(ManagedClient.class, annotation.clientRef());
            clientId = managedClient.getClientId();
            clientSecret = managedClient.getSecret();
        }

        OAuthClient oAuthClient = new OAuthClient(keycloakUrls.getBase(), httpClient, webDriver);
        oAuthClient.config().realm(realm.getName()).client(clientId, clientSecret);
        return oAuthClient;
    }

    @Override
    public boolean compatible(InstanceContext<OAuthClient, InjectOAuthClient> a, RequestedInstance<OAuthClient, InjectOAuthClient> b) {
        return true;
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public void close(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        instanceContext.getValue().close();
    }
}
