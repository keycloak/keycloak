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
    private URI baseQueryURI;
    private URI currentURI;
    private String stateChecker;

    public UrlBean(RealmModel realm, Theme theme, URI baseURI, URI baseQueryURI, URI currentURI, String stateChecker) {
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
        this.baseQueryURI = baseQueryURI;
        this.currentURI = currentURI;
        this.stateChecker = stateChecker;
    }

    public String getAccessUrl() {
        return Urls.accountAccessPage(baseQueryURI, realm).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseQueryURI, realm).toString();
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseQueryURI, realm).toString();
    }

    public String getSocialUrl() {
        return Urls.accountSocialPage(baseQueryURI, realm).toString();
    }

    public String getTotpUrl() {
        return Urls.accountTotpPage(baseQueryURI, realm).toString();
    }

    public String getLogUrl() {
        return Urls.accountLogPage(baseQueryURI, realm).toString();
    }

    public String getSessionsUrl() {
        return Urls.accountSessionsPage(baseQueryURI, realm).toString();
    }

    public String getSessionsLogoutUrl() {
        return Urls.accountSessionsLogoutPage(baseQueryURI, realm, stateChecker).toString();
    }

    public String getTotpRemoveUrl() {
        return Urls.accountTotpRemove(baseQueryURI, realm, stateChecker).toString();
    }

    public String getLogoutUrl() {
        return Urls.accountLogout(baseQueryURI, currentURI, realm).toString();
    }

    public String getResourcesPath() {
        URI uri = Urls.themeRoot(baseURI);
        return uri.getPath() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }

}
