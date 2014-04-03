package org.keycloak.account.freemarker.model;

import org.keycloak.freemarker.Theme;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.flows.Urls;

import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UrlBean {

    private String realm;
    private Theme theme;
    private URI baseURI;

    public UrlBean(RealmModel realm, Theme theme, URI baseURI) {
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
    }

    public String getAccessUrl() {
        return Urls.accountAccessPage(baseURI, realm).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseURI, realm).toString();
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseURI, realm).toString();
    }

    public String getSocialUrl() {
        return Urls.accountSocialPage(baseURI, realm).toString();
    }

    public String getTotpUrl() {
        return Urls.accountTotpPage(baseURI, realm).toString();
    }

    public String getLogUrl() {
        return Urls.accountLogPage(baseURI, realm).toString();
    }

    public String getTotpRemoveUrl() {
        return Urls.accountTotpRemove(baseURI, realm).toString();
    }

    public String getLogoutUrl() {
        return Urls.accountLogout(baseURI, realm).toString();
    }

    public String getResourcesPath() {
        URI uri = Urls.themeRoot(baseURI);
        return uri.getPath() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }

}
