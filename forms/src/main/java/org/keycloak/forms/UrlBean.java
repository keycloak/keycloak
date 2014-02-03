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
package org.keycloak.forms;

import org.keycloak.services.resources.flows.Urls;

import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UrlBean {

    private URI baseURI;

    private RealmBean realm;

    private String referrerURI;

    public UrlBean(RealmBean realm, URI baseURI, String referrerURI){
        this.realm = realm;
        this.baseURI = baseURI;
        this.referrerURI = referrerURI;
    }

    protected String getRealmIdentifier() {
        return realm.getName();
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

    public String getAccessUrl() {
        return Urls.accountAccessPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseURI, getRealmIdentifier()).toString();
    }

    URI getBaseURI() {
        return baseURI;
    }

    public String getLoginAction() {
        return Urls.realmLoginAction(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginUrl() {
        return Urls.realmLoginPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getRegistrationAction() {
        if (realm.isSaas()) {
            return Urls.saasRegisterAction(baseURI).toString();
        } else {
            return Urls.realmRegisterAction(baseURI, getRealmIdentifier()).toString();
        }
    }

    public String getRegistrationUrl() {
        return Urls.realmRegisterPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginUpdatePasswordUrl() {
        return Urls.loginActionUpdatePassword(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginUpdateTotpUrl() {
        return Urls.loginActionUpdateTotp(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginUpdateProfileUrl() {
        return Urls.loginActionUpdateProfile(baseURI, getRealmIdentifier()).toString();
    }

    public String getSocialUrl() {
        return Urls.accountSocialPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getTotpUrl() {
        return Urls.accountTotpPage(baseURI, getRealmIdentifier()).toString();
    }

    public String getTotpRemoveUrl() {
        return Urls.accountTotpRemove(baseURI, getRealmIdentifier()).toString();
    }

    public String getLogoutUrl() {
        return Urls.accountLogout(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginPasswordResetUrl() {
        return Urls.loginPasswordReset(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginUsernameReminderUrl() {
        return Urls.loginUsernameReminder(baseURI, getRealmIdentifier()).toString();
    }

    public String getLoginEmailVerificationUrl() {
        return Urls.loginActionEmailVerification(baseURI, getRealmIdentifier()).toString();
    }

    public String getReferrerURI() {
        return referrerURI;
    }

}
