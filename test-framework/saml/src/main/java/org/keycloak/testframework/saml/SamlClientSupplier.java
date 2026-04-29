package org.keycloak.testframework.saml;

import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.saml.annotations.InjectSamlClient;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;

public class SamlClientSupplier implements Supplier<SamlClient, InjectSamlClient> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<SamlClient, InjectSamlClient> instanceContext) {
        return DependenciesBuilder.create(KeycloakUrls.class)
                .add(HttpClient.class)
                .add(ManagedWebDriver.class)
                .add(TestSamlApp.class)
                .add(ManagedRealm.class, instanceContext.getAnnotation().realmRef())
                .build();
    }

    @Override
    public SamlClient getValue(InstanceContext<SamlClient, InjectSamlClient> instanceContext) {
        KeycloakUrls keycloakUrls = instanceContext.getDependency(KeycloakUrls.class);
        CloseableHttpClient httpClient = (CloseableHttpClient) instanceContext.getDependency(HttpClient.class);
        ManagedWebDriver webDriver = instanceContext.getDependency(ManagedWebDriver.class);
        TestSamlApp testSamlApp = instanceContext.getDependency(TestSamlApp.class);
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, instanceContext.getAnnotation().realmRef());

        // Create SAML client configuration
        String acsUrl = testSamlApp.getAssertionConsumerServiceUrl();
        SamlClientConfig clientConfig = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        ClientRepresentation clientRep = clientConfig.configure(ClientConfigBuilder.create())
                .redirectUris(acsUrl + "/*")
                .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, acsUrl)
                .attribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, acsUrl)
                .build();

        String clientId = clientRep.getClientId();

        // Register the SAML client in the realm
        String id = ApiUtil.getCreatedId(realm.admin().clients().create(clientRep));
        ClientResource clientResource = realm.admin().clients().get(id);


        // Create SamlClient
        SamlClient samlClient = new SamlClient(keycloakUrls.getBase(), httpClient, webDriver, testSamlApp, clientResource);
        samlClient.realm(realm.getName())
                .client(clientId)
                .assertionConsumerServiceUrl(testSamlApp.getAssertionConsumerServiceUrl());

        return samlClient;
    }

    @Override
    public boolean compatible(InstanceContext<SamlClient, InjectSamlClient> a, RequestedInstance<SamlClient, InjectSamlClient> b) {
        return a.getAnnotation().ref().equals(b.getAnnotation().ref());
    }

    @Override
    public void close(InstanceContext<SamlClient, InjectSamlClient> instanceContext) {
        instanceContext.getValue().close();
    }
}
