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

import java.net.URI;

import org.keycloak.services.resources.flows.Urls;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UrlBean {

    private URI baseURI;

    private RealmBean realm;

    private boolean socialRegistration;

    public boolean isSocialRegistration() {
        return socialRegistration;
    }

    public void setSocialRegistration(boolean socialRegistration) {
        this.socialRegistration = socialRegistration;
    }

    public UrlBean(RealmBean realm, URI baseURI){
        this.realm = realm;
        this.baseURI = baseURI;
    }

    public RealmBean getRealm() {
        return realm;
    }

    public void setRealm(RealmBean realm) {
        this.realm = realm;
    }

    public String getAccessUrl() {
        return Urls.accountAccessPage(baseURI, realm.getId()).toString();
    }

    public String getAccountUrl() {
        return Urls.accountPage(baseURI, realm.getId()).toString();
    }

    URI getBaseURI() {
        return baseURI;
    }

    public String getLoginAction() {
        return Urls.realmLoginAction(baseURI, realm.getId()).toString();
    }

    public String getLoginUrl() {
        return Urls.realmLoginPage(baseURI, realm.getId()).toString();
    }

    public String getPasswordUrl() {
        return Urls.accountPasswordPage(baseURI, realm.getId()).toString();
    }

    public String getRegistrationAction() {
        if (realm.isSaas()) {
            return Urls.saasRegisterAction(baseURI).toString();
        } else if (socialRegistration){
            return Urls.socialRegisterAction(baseURI, realm.getId()).toString();
        } else {
            return Urls.realmRegisterAction(baseURI, realm.getId()).toString();
        }
    }

    public String getRegistrationUrl() {
        if (realm.isSaas()) {
            // TODO: saas social registration
            return Urls.saasRegisterPage(baseURI).toString();
        } else {
            return Urls.realmRegisterPage(baseURI, realm.getId()).toString();
        }
    }

    public String getLoginUpdatePasswordUrl() {
        return Urls.loginActionUpdatePassword(baseURI, realm.getId()).toString();
    }

    public String getLoginUpdateTotpUrl() {
        return Urls.loginActionUpdateTotp(baseURI, realm.getId()).toString();
    }

    public String getLoginUpdateProfileUrl() {
        return Urls.loginActionUpdateProfile(baseURI, realm.getId()).toString();
    }

    public String getSocialUrl() {
        return Urls.accountSocialPage(baseURI, realm.getId()).toString();
    }

    public String getTotpUrl() {
        return Urls.accountTotpPage(baseURI, realm.getId()).toString();
    }

    public String getTotpRemoveUrl() {
        return Urls.accountTotpRemove(baseURI, realm.getId()).toString();
    }

    public String getLoginPasswordResetUrl() {
        return Urls.loginPasswordReset(baseURI, realm.getId()).toString();
    }

    public String getLoginEmailVerificationUrl() {
        return Urls.loginActionEmailVerification(baseURI, realm.getId()).toString();
    }

}
