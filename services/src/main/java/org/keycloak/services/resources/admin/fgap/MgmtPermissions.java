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
package org.keycloak.services.resources.admin.fgap;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.identity.UserModelIdentity;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DecisionPermissionCollector;
import org.keycloak.authorization.policy.evaluation.EvaluationContext;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.ws.rs.ForbiddenException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
class MgmtPermissions implements AdminPermissionEvaluator, AdminPermissionManagement, RealmsPermissionEvaluator {
    protected RealmModel realm;
    protected KeycloakSession session;
    protected AuthorizationProvider authz;
    protected AdminAuth auth;
    protected Identity identity;
    protected UserModel admin;
    protected RealmModel adminsRealm;
    protected ResourceServer realmResourceServer;
    protected UserPermissions users;
    protected GroupPermissions groups;
    protected RealmPermissions realmPermissions;
    protected ClientPermissions clientPermissions;
    protected IdentityProviderPermissions idpPermissions;
    protected RolePermissions rolePermissions;


    MgmtPermissions(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
        KeycloakSessionFactory keycloakSessionFactory = session.getKeycloakSessionFactory();
        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AuthorizationProviderFactory factory = (AuthorizationProviderFactory) keycloakSessionFactory.getProviderFactory(AuthorizationProvider.class);
            this.authz = factory.create(session, realm);
        }
    }

    MgmtPermissions(KeycloakSession session, RealmModel realm, AdminAuth auth) {
        this(session, realm);
        this.auth = auth;
        this.admin = auth.getUser();
        this.adminsRealm = auth.getRealm();
        if (!auth.getRealm().equals(realm)
                && !RealmManager.isAdministrationRealm(auth.getRealm())) {
            throw new ForbiddenException();
        }
        initIdentity(session, auth);
    }
    MgmtPermissions(KeycloakSession session, AdminAuth auth) {
        this.session = session;
        this.auth = auth;
        this.admin = auth.getUser();
        this.adminsRealm = auth.getRealm();
        initIdentity(session, auth);
    }

    private void initIdentity(KeycloakSession session, AdminAuth auth) {
        AccessToken accessToken = auth.getToken();
        AuthenticationManager.resolveLightweightAccessTokenRoles(session, accessToken, adminsRealm);
        this.identity = new KeycloakIdentity(accessToken, session, adminsRealm);
    }

    MgmtPermissions(KeycloakSession session, RealmModel adminsRealm, UserModel admin) {
        this.session = session;
        this.admin = admin;
        this.adminsRealm = adminsRealm;
        this.identity = new UserModelIdentity(adminsRealm, admin);
    }

    MgmtPermissions(KeycloakSession session, RealmModel realm, RealmModel adminsRealm, UserModel admin) {
        this(session, realm);
        this.admin = admin;
        this.adminsRealm = adminsRealm;
        this.identity = new UserModelIdentity(realm, admin);
    }

    @Override
    public ClientModel getRealmPermissionsClient() {
        if (realm.getName().equals(Config.getAdminRealm())) {
            return realm.getClientByClientId(Config.getAdminRealm() + "-realm");
        } else {
            return realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
        }
    }

    @Override
    public AuthorizationProvider authz() {
        return authz;
    }

    @Override
    public void requireAnyAdminRole() {
        if (!hasAnyAdminRole()) {
            throw new ForbiddenException();
        }
    }

    public boolean hasAnyAdminRole() {
        return hasOneAdminRole(AdminRoles.ALL_REALM_ROLES);
    }

    public boolean hasAnyAdminRole(RealmModel realm) {
        return hasOneAdminRole(realm, AdminRoles.ALL_REALM_ROLES);
    }

    @Override
    public boolean hasOneAdminRole(String... adminRoles) {
        return hasOneAdminRole(realm, adminRoles);
    }

    public boolean hasOneAdminRole(RealmModel realm, String... adminRoles) {
        String clientId;
        RealmManager realmManager = new RealmManager(session);
        if (RealmManager.isAdministrationRealm(adminsRealm)) {
            clientId = realm.getMasterAdminClient().getClientId();
        } else if (adminsRealm.equals(realm)) {
            clientId = realm.getClientByClientId(realmManager.getRealmAdminClientId(realm)).getClientId();
        } else {
            return false;
        }
        return identity.hasOneClientRole(clientId, adminRoles);
    }


    public boolean isAdminSameRealm() {
        return auth == null || realm.getId().equals(auth.getRealm().getId());
    }

    @Override
    public AdminAuth adminAuth() {
        return auth;
    }

    public Identity identity() {
        return identity;
    }

    public UserModel admin() {
        return admin;
    }

    public RealmModel adminsRealm() {
        return adminsRealm;
    }


    @Override
    public RolePermissions roles() {
        if (rolePermissions!=null) return rolePermissions;
        rolePermissions = new RolePermissions(session, realm, authz, this);
        return rolePermissions;
    }

    @Override
    public UserPermissions users() {
        if (users != null) return users;
        users = new UserPermissions(session, authz, this);
        return users;
    }

    @Override
    public RealmPermissions realm() {
        if (realmPermissions != null) return realmPermissions;
        realmPermissions = new RealmPermissions(this);
        return realmPermissions;
    }

    @Override
    public ClientPermissions clients() {
        if (clientPermissions != null) return clientPermissions;
        clientPermissions = new ClientPermissions(session, realm, authz, this);
        return clientPermissions;
    }

    @Override
    public IdentityProviderPermissions idps() {
        if (idpPermissions != null) return idpPermissions;
        idpPermissions = new IdentityProviderPermissions(session, realm, authz, this);
        return idpPermissions;
    }

    @Override
    public GroupPermissions groups() {
        if (groups != null) return groups;
        groups = new GroupPermissions(authz, this);
        return groups;
    }

    public ResourceServer findOrCreateResourceServer(ClientModel client) {
         return initializeRealmResourceServer();
    }

    public ResourceServer resourceServer(ClientModel client) {
        return realmResourceServer();
    }

    @Override
    public ResourceServer realmResourceServer() {
        if (authz == null) return null;
        if (realmResourceServer != null) return realmResourceServer;
        ClientModel client = getRealmPermissionsClient();
        if (client == null) return null;
        realmResourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(client);
        return realmResourceServer;

    }

    public ResourceServer initializeRealmResourceServer() {
        if (authz == null) return null;
        if (realmResourceServer != null) return realmResourceServer;
        ClientModel client = getRealmPermissionsClient();
        if (client == null) return null;
        realmResourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(client);
        if (realmResourceServer == null) {
            realmResourceServer = authz.getStoreFactory().getResourceServerStore().create(client);
        }
        return realmResourceServer;
    }

    protected Scope manageScope;
    protected Scope viewScope;

    public void initializeRealmDefaultScopes() {
        ResourceServer server = initializeRealmResourceServer();
        if (server == null) return;
        manageScope = initializeRealmScope(MgmtPermissions.MANAGE_SCOPE);
        viewScope = initializeRealmScope(MgmtPermissions.VIEW_SCOPE);
    }

    public Scope initializeRealmScope(String name) {
        ResourceServer server = initializeRealmResourceServer();
        if (server == null) return null;
        Scope scope  = authz.getStoreFactory().getScopeStore().findByName(server, name);
        if (scope == null) {
            scope = authz.getStoreFactory().getScopeStore().create(server, name);
        }
        return scope;
    }

    public Scope initializeScope(String name, ResourceServer server) {
        if (authz == null) return null;
        Scope scope  = authz.getStoreFactory().getScopeStore().findByName(server, name);
        if (scope == null) {
            scope = authz.getStoreFactory().getScopeStore().create(server, name);
        }
        return scope;
    }



    public Scope realmManageScope() {
        if (manageScope != null) return manageScope;
        manageScope = realmScope(MgmtPermissions.MANAGE_SCOPE);
        return manageScope;
    }


    public Scope realmViewScope() {
        if (viewScope != null) return viewScope;
        viewScope = realmScope(MgmtPermissions.VIEW_SCOPE);
        return viewScope;
    }

    public Scope realmScope(String scope) {
        ResourceServer server = realmResourceServer();
        if (server == null) return null;
        return authz.getStoreFactory().getScopeStore().findByName(server, scope);
    }

    public boolean evaluatePermission(Resource resource, ResourceServer resourceServer, Scope... scope) {
        Identity identity = identity();
        if (identity == null) {
            throw new RuntimeException("Identity of admin is not set for permission query");
        }
        return evaluatePermission(resource, resourceServer, identity, scope);
    }

    public Collection<Permission> evaluatePermission(ResourcePermission permission, ResourceServer resourceServer) {
        return evaluatePermission(permission, resourceServer, new DefaultEvaluationContext(identity, session));
    }

    public DecisionPermissionCollector getDecision(ResourcePermission permission, ResourceServer resourceServer) {
        return evaluatePermission(List.of(permission), resourceServer, new DefaultEvaluationContext(identity, session));
    }

    public Collection<Permission> evaluatePermission(ResourcePermission permission, ResourceServer resourceServer, EvaluationContext context) {
        return evaluatePermission(Arrays.asList(permission), resourceServer, context).results();
    }

    public DecisionPermissionCollector getDecision(ResourcePermission permission, ResourceServer resourceServer, EvaluationContext context) {
        return evaluatePermission(Arrays.asList(permission), resourceServer, context);
    }

    public boolean evaluatePermission(Resource resource, ResourceServer resourceServer, Identity identity, Scope... scope) {
        EvaluationContext context = new DefaultEvaluationContext(identity, session);
        return evaluatePermission(resource, resourceServer, context, scope);
    }

    public boolean evaluatePermission(Resource resource, ResourceServer resourceServer, EvaluationContext context, Scope... scope) {
        return !evaluatePermission(Arrays.asList(new ResourcePermission(resource, Arrays.asList(scope), resourceServer)), resourceServer, context).results().isEmpty();
    }

    public DecisionPermissionCollector evaluatePermission(List<ResourcePermission> permissions, ResourceServer resourceServer, EvaluationContext context) {
        RealmModel oldRealm = session.getContext().getRealm();
        try {
            session.getContext().setRealm(realm);
            return authz.evaluators().from(permissions, resourceServer, context).getDecision(resourceServer, null, DecisionPermissionCollector.class);
        } finally {
            session.getContext().setRealm(oldRealm);
        }
    }

    @Override
    public boolean canView(RealmModel realm) {
        return hasOneAdminRole(realm, AdminRoles.VIEW_REALM, AdminRoles.MANAGE_REALM);
    }

    @Override
    public boolean isAdmin(RealmModel realm) {
        return hasAnyAdminRole(realm);
    }

    @Override
    public boolean isAdmin() {
        if (RealmManager.isAdministrationRealm(adminsRealm)) {
            if (identity.hasRealmRole(AdminRoles.ADMIN) || identity.hasRealmRole(AdminRoles.CREATE_REALM)) {
                return true;
            }
            return session.realms().getRealmsStream().anyMatch(this::isAdmin);
        } else {
            return isAdmin(adminsRealm);
        }
    }

    @Override
    public boolean canCreateRealm() {
        if (!RealmManager.isAdministrationRealm(auth.getRealm())) {
           return false;
        }
        return identity.hasRealmRole(AdminRoles.CREATE_REALM);
    }

    @Override
    public void requireCreateRealm() {
        if (!canCreateRealm()) {
            throw new ForbiddenException();
        }
    }




}
