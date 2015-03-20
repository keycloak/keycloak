/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.login.freemarker.model;

import org.keycloak.freemarker.Theme;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.flows.Urls;

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
        this.realm = realm.getName();
        this.theme = theme;
        this.baseURI = baseURI;
        this.actionuri = actionUri;
    }

    public String getLoginAction() {
        return Urls.realmLoginAction(baseURI, realm).toString();
    }

    public String getLoginUrl() {
        return Urls.realmLoginPage(baseURI, realm).toString();
    }

    public String getRegistrationAction() {
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

    public String getLoginPasswordResetUrl() {
        return Urls.loginPasswordReset(baseURI, realm).toString();
    }

    public String getLoginUsernameReminderUrl() {
        return Urls.loginUsernameReminder(baseURI, realm).toString();
    }

    public String getLoginEmailVerificationUrl() {
        return Urls.loginActionEmailVerification(baseURI, realm).toString();
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
