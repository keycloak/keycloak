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
package org.keycloak.authorization.admin.permissions;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.common.UserModelIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.RealmAuth;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MgmtPermissions {
    public static final String MANAGE_SCOPE = "manage";

    protected RealmModel realm;
    protected KeycloakSession session;
    protected AuthorizationProvider authz;
    protected AdminAuth auth;
    protected Identity identity;

    public MgmtPermissions(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        AuthorizationProviderFactory factory = (AuthorizationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
        this.authz = factory.create(session, realm);
    }
    public MgmtPermissions(KeycloakSession session, RealmModel realm, AdminAuth auth) {
        this(session, realm);
        this.auth = auth;

    }

    public boolean isAdminSameRealm() {
        return realm.getId().equals(auth.getRealm().getId());
    }

    public RealmAuth getRealmAuth() {
        RealmManager realmManager = new RealmManager(session);
        if (auth.getRealm().equals(realmManager.getKeycloakAdminstrationRealm())) {
            return new RealmAuth(auth, realm.getMasterAdminClient());
        } else {
            return new RealmAuth(auth, realm.getClientByClientId(realmManager.getRealmAdminClientId(auth.getRealm())));
        }
    }

    public Identity identity() {
        if (identity != null) return identity;
        if (auth.getClient().getClientId().equals(Constants.REALM_MANAGEMENT_CLIENT_ID)) {
            this.identity = new UserModelIdentity(realm, auth.getUser());

        } else {
            this.identity = new KeycloakIdentity(auth.getToken(), session);
        }
        return this.identity;
    }


    public RoleMgmtPermissions roles() {
        return new RoleMgmtPermissions(session, realm, authz, this);
    }

    public ResourceServer findOrCreateResourceServer(ClientModel client) {
        ResourceServer server = authz.getStoreFactory().getResourceServerStore().findByClient(client.getId());
        if (server == null) {
            server = authz.getStoreFactory().getResourceServerStore().create(client.getId());
        }
        return server;
    }

}
