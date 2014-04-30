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

    public boolean isSocial() {
        return realm.isSocial();
    }

    public boolean isRegistrationAllowed() {
        return realm.isRegistrationAllowed();
    }

    public boolean isResetPasswordAllowed() {
        return realm.isResetPasswordAllowed();
    }

    public boolean isRememberMe() {
        return realm.isRememberMe();
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
