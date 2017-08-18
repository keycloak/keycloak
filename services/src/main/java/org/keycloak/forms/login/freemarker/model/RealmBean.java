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
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmBean {

    private RealmModel realm;

    public RealmBean(RealmModel realmModel) {
        realm = realmModel;
    }

    public String getName() {
        return realm.getName();
    }

    public String getDisplayName() {
        String displayName = realm.getDisplayName();
        if (displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return getName();
        }
    }

    public String getDisplayNameHtml() {
        String displayNameHtml = realm.getDisplayNameHtml();
        if (displayNameHtml != null && displayNameHtml.length() > 0) {
            return displayNameHtml;
        } else {
            return getDisplayName();
        }
    }

    public boolean isIdentityFederationEnabled() {
        return realm.isIdentityFederationEnabled();
    }

    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    public boolean isRegistrationEmailAsUsername() {
        return realm.isRegistrationEmailAsUsername();
    }
    
    public boolean isLoginWithEmailAllowed() {
        return realm.isLoginWithEmailAllowed();
    }

    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    public boolean isRememberMe() {
        return realm.isRememberMe();
    }

    public boolean isInternationalizationEnabled() {
        return realm.isInternationalizationEnabled();
    }

    public boolean isEditUsernameAllowed() {
        return realm.isEditUsernameAllowed();
    }

    public boolean isPassword() {
        for (RequiredCredentialModel r : realm.getRequiredCredentials()) {
            if (r.getType().equals(CredentialRepresentation.PASSWORD)) {
                return true;
            }
        }
        return false;
    }

}
