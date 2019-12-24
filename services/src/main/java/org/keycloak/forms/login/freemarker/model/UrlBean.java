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
package org.keycloak.forms.login.freemarker.model;

import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.theme.Theme;

import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UrlBean {

    private final URI actionuri;
    private URI baseURI;
    private Theme theme;
    private String realm;

    public UrlBean(RealmModel realm, Theme theme, URI baseURI, URI actionUri) {
        this.realm = realm != null ? realm.getName() : null;
        this.theme = theme;
        this.baseURI = baseURI;
        this.actionuri = actionUri;
    }

    public String getLoginAction() {
        if (this.actionuri != null) {
            return this.actionuri.toString();
        }
        throw new RuntimeException("action URI not set");
    }

    public String getLoginUrl() {
        return Urls.realmLoginPage(baseURI, realm).toString();
    }

    public String getLoginRestartFlowUrl() {
        return Urls.realmLoginRestartPage(baseURI, realm).toString();
    }

    public boolean hasAction()  {
        return actionuri != null;
    }

    public String getRegistrationAction() {
        if (this.actionuri != null) {
            return this.actionuri.toString();
        }
        return Urls.realmRegisterAction(baseURI, realm).toString();
    }

    public String getRegistrationUrl() {
        return Urls.realmRegisterPage(baseURI, realm).toString();
    }

    public String getLoginUpdatePasswordUrl() {
        return Urls.loginActionUpdatePassword(baseURI, realm).toString();
    }

    public String getLoginUpdateTotpUrl() {
        return Urls.loginActionUpdateTotp(baseURI, realm).toString();
    }

    public String getLoginUpdateProfileUrl() {
        return Urls.loginActionUpdateProfile(baseURI, realm).toString();
    }

    public String getLoginResetCredentialsUrl() {
        return Urls.loginResetCredentials(baseURI, realm).toString();
    }

    public String getLoginUsernameReminderUrl() {
        return Urls.loginUsernameReminder(baseURI, realm).toString();
    }

    public String getFirstBrokerLoginUrl() {
        return Urls.firstBrokerLoginProcessor(baseURI, realm).toString();
    }

    public String getResourcesUrl() {
        return Urls.themeRoot(baseURI).toString() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }

    public String getOauthAction() {
        if (this.actionuri != null) {
            return this.actionuri.getPath();
        }

        return Urls.realmOauthAction(baseURI, realm).toString();
    }

    public String getResourcesPath() {
        URI uri = Urls.themeRoot(baseURI);
        return uri.getPath() + "/" + theme.getType().toString().toLowerCase() +"/" + theme.getName();
    }
}
