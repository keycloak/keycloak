/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.forms.account.freemarker.model;

import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.theme.Theme;

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

    public UrlBean(RealmModel realm, Theme theme, URI baseURI, URI baseQueryURI, URI currentURI, String stateChecker) {
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
        this.baseQueryURI = baseQueryURI;
        this.currentURI = currentURI;
    }

    public String getApplicationsUrl() {
        return Urls.accountApplicationsPage(baseQueryURI, realm).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseQueryURI, realm).toString();
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseQueryURI, realm).toString();
    }

    public String getSocialUrl() {
        return Urls.accountFederatedIdentityPage(baseQueryURI, realm).toString();
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

    public String getLogoutUrl() {
        return Urls.accountLogout(baseQueryURI, currentURI, realm).toString();
    }

    public String getResourceUrl() {
        return Urls.accountResourcesPage(baseQueryURI, realm).toString();
    }

    public String getResourceDetailUrl(String id) {
        return Urls.accountResourceDetailPage(id, baseQueryURI, realm).toString();
    }

    public String getResourceGrant(String id) {
        return Urls.accountResourceGrant(id, baseQueryURI, realm).toString();
    }

    public String getResourceShare(String id) {
        return Urls.accountResourceShare(id, baseQueryURI, realm).toString();
    }

    public String getResourcesPath() {
        URI uri = Urls.themeRoot(baseURI);
        return uri.getPath() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }

}
