package org.keycloak.tests.forms;

import java.net.URI;

import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.theme.Theme;

import org.junit.jupiter.api.Assertions;

@KeycloakIntegrationTest
public class UrlBeanTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @TestOnServer
    public void resourcesCommonUrlReturnsFullUrl(KeycloakSession session) throws Exception {
        RealmModel realm = session.realms().getRealmByName("default");
        Theme theme = session.theme().getTheme(Theme.Type.EMAIL);
        URI baseUri = session.getContext().getUri().getBaseUri();

        UrlBean urlBean = new UrlBean(realm, theme, baseUri, null);
        String themeRoot = Urls.themeRoot(baseUri).toString();

        Assertions.assertEquals(themeRoot + "/common/keycloak", urlBean.getResourcesCommonUrl());
        Assertions.assertTrue(urlBean.getResourcesCommonUrl().startsWith("http"),
                "resourcesCommonUrl must be an absolute URL");
    }

    @TestOnServer
    public void resourcesCommonPathReturnsPathOnly(KeycloakSession session) throws Exception {
        RealmModel realm = session.realms().getRealmByName("default");
        Theme theme = session.theme().getTheme(Theme.Type.EMAIL);
        URI baseUri = session.getContext().getUri().getBaseUri();

        UrlBean urlBean = new UrlBean(realm, theme, baseUri, null);
        String themeRoot = Urls.themeRoot(baseUri).getPath();

        Assertions.assertEquals(themeRoot + "/common/keycloak", urlBean.getResourcesCommonPath());
        Assertions.assertTrue(urlBean.getResourcesCommonPath().startsWith("/"),
                "resourcesCommonPath must not contain scheme");
    }

    @TestOnServer
    public void resourcesCommonUrlAndPathShareSameSuffix(KeycloakSession session) throws Exception {
        RealmModel realm = session.realms().getRealmByName("default");
        Theme theme = session.theme().getTheme(Theme.Type.EMAIL);
        URI baseUri = session.getContext().getUri().getBaseUri();

        UrlBean urlBean = new UrlBean(realm, theme, baseUri, null);

        String url = urlBean.getResourcesCommonUrl();
        String path = urlBean.getResourcesCommonPath();
        Assertions.assertTrue(url.endsWith("/common/keycloak") && path.endsWith("/common/keycloak"),
                "URL and Path should end with the same common suffix");
    }
}
