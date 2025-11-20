package org.keycloak.testframework.oauth;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openqa.selenium.WebDriver;

public class OAuthClientSupplier implements Supplier<OAuthClient, InjectOAuthClient> {

    @Override
    public OAuthClient getValue(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        InjectOAuthClient annotation = instanceContext.getAnnotation();

        KeycloakUrls keycloakUrls = instanceContext.getDependency(KeycloakUrls.class);
        CloseableHttpClient httpClient = (CloseableHttpClient) instanceContext.getDependency(HttpClient.class);
        WebDriver webDriver = instanceContext.getDependency(WebDriver.class);
        TestApp testApp = instanceContext.getDependency(TestApp.class);

        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, annotation.realmRef());

        String redirectUri = testApp.getRedirectionUri();

        ClientConfig clientConfig = SupplierHelpers.getInstance(annotation.config());
        ClientRepresentation testAppClient = clientConfig.configure(ClientConfigBuilder.create())
                .redirectUris(redirectUri)
                .build();

        if (annotation.kcAdmin()) {
            testAppClient.setAdminUrl(testApp.getAdminUri());
        }

        String clientId = testAppClient.getClientId();
        String clientSecret = testAppClient.getSecret();

        ApiUtil.getCreatedId(realm.admin().clients().create(testAppClient));

        OAuthClient oAuthClient = new OAuthClient(keycloakUrls.getBase(), httpClient, webDriver);
        oAuthClient.config().realm(realm.getName()).client(clientId, clientSecret).redirectUri(redirectUri);
        return oAuthClient;
    }

    @Override
    public boolean compatible(InstanceContext<OAuthClient, InjectOAuthClient> a, RequestedInstance<OAuthClient, InjectOAuthClient> b) {
        return a.getAnnotation().ref().equals(b.getAnnotation().ref());
    }

    @Override
    public void close(InstanceContext<OAuthClient, InjectOAuthClient> instanceContext) {
        instanceContext.getValue().close();
    }
}
