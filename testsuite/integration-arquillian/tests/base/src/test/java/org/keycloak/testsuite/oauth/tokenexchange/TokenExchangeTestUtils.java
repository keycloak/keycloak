/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.oauth.tokenexchange;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.common.Profile;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TokenExchangeTestUtils {

    private TokenExchangeTestUtils() {
    }


    public static void setupRealm(KeycloakSession session) {
        addDirectExchanger(session);

        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.getRole("example");

        ClientModel target = realm.getClientByClientId("target");
        assertNotNull(target);

        ClientModel differentScopeClient = realm.addClient("different-scope-client");
        differentScopeClient.setClientId("different-scope-client");
        differentScopeClient.setPublicClient(false);
        differentScopeClient.setDirectAccessGrantsEnabled(true);
        differentScopeClient.setEnabled(true);
        differentScopeClient.setSecret("secret");
        differentScopeClient.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        differentScopeClient.setFullScopeAllowed(false);
        differentScopeClient.removeClientScope(realm.getClientScopesStream().filter(scope->"email".equals(scope.getName())).findAny().get());

        ClientModel clientExchanger = realm.addClient("client-exchanger");
        clientExchanger.setClientId("client-exchanger");
        clientExchanger.setPublicClient(false);
        clientExchanger.setDirectAccessGrantsEnabled(true);
        clientExchanger.setEnabled(true);
        clientExchanger.setSecret("secret");
        clientExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientExchanger.setFullScopeAllowed(false);

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            RoleModel impersonateRole = management.getRealmPermissionsClient().getRole(AdminRoles.IMPERSONATION);
            clientExchanger.addScopeMapping(impersonateRole);
        }

        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_ID));
        clientExchanger.addProtocolMapper(UserSessionNoteMapper.createUserSessionNoteMapper(IMPERSONATOR_USERNAME));
        clientExchanger.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("different-scope-client-audience", differentScopeClient.getClientId(), null, true, false, true));
        clientExchanger.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("allowed-exchanger1", null, "legal", true, false, true));
        clientExchanger.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("allowed-exchanger2", null, "no-refresh-token", true, false, true));
        clientExchanger.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("audience-to-itself", "client-exchanger", null, true, false, true));

        ClientModel illegal = realm.addClient("illegal");
        illegal.setClientId("illegal");
        illegal.setPublicClient(false);
        illegal.setDirectAccessGrantsEnabled(true);
        illegal.setEnabled(true);
        illegal.setSecret("secret");
        illegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        illegal.setFullScopeAllowed(false);

        ClientModel legal = realm.addClient("legal");
        legal.setClientId("legal");
        legal.setPublicClient(false);
        legal.setDirectAccessGrantsEnabled(true);
        legal.setEnabled(true);
        legal.setSecret("secret");
        legal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        legal.setFullScopeAllowed(false);

        ClientModel directLegal = realm.addClient("direct-legal");
        directLegal.setClientId("direct-legal");
        directLegal.setPublicClient(false);
        directLegal.setDirectAccessGrantsEnabled(true);
        directLegal.setEnabled(true);
        directLegal.setSecret("secret");
        directLegal.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directLegal.setFullScopeAllowed(false);
        directLegal.addRedirectUri(OAuthClient.APP_ROOT + "/auth");

        ClientModel directPublic = realm.addClient("direct-public");
        directPublic.setClientId("direct-public");
        directPublic.setPublicClient(true);
        directPublic.setDirectAccessGrantsEnabled(true);
        directPublic.setEnabled(true);
        directPublic.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directPublic.setFullScopeAllowed(false);
        directPublic.addRedirectUri("*");
        directPublic.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("client-exchanger-audience", clientExchanger.getClientId(), null, true, false, true));

        ClientModel directUntrustedPublic = realm.addClient("direct-public-untrusted");
        directUntrustedPublic.setClientId("direct-public-untrusted");
        directUntrustedPublic.setPublicClient(true);
        directUntrustedPublic.setDirectAccessGrantsEnabled(true);
        directUntrustedPublic.setEnabled(true);
        directUntrustedPublic.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directUntrustedPublic.setFullScopeAllowed(false);
        directUntrustedPublic.addRedirectUri("*");
        directUntrustedPublic.setAttribute(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, "+");
        directUntrustedPublic.addProtocolMapper(AudienceProtocolMapper.createClaimMapper("client-exchanger-audience", clientExchanger.getClientId(), null, true, false, true));

        ClientModel directNoSecret = realm.addClient("direct-no-secret");
        directNoSecret.setClientId("direct-no-secret");
        directNoSecret.setPublicClient(false);
        directNoSecret.setDirectAccessGrantsEnabled(true);
        directNoSecret.setEnabled(true);
        directNoSecret.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directNoSecret.setFullScopeAllowed(false);

        ClientModel noRefreshToken = realm.addClient("no-refresh-token");
        noRefreshToken.setClientId("no-refresh-token");
        noRefreshToken.setPublicClient(false);
        noRefreshToken.setDirectAccessGrantsEnabled(true);
        noRefreshToken.setEnabled(true);
        noRefreshToken.setSecret("secret");
        noRefreshToken.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        noRefreshToken.setFullScopeAllowed(false);
        noRefreshToken.getAttributes().put(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false");

        ClientModel serviceAccount = realm.addClient("my-service-account");
        serviceAccount.setClientId("my-service-account");
        serviceAccount.setPublicClient(false);
        serviceAccount.setServiceAccountsEnabled(true);
        serviceAccount.setEnabled(true);
        serviceAccount.setSecret("secret");
        serviceAccount.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        serviceAccount.setFullScopeAllowed(false);
        new ClientManager(new RealmManager(session)).enableServiceAccount(serviceAccount);

        // permission for client to client exchange to "target" client
        ClientPolicyRepresentation clientRep = new ClientPolicyRepresentation();
        clientRep.setName("to");
        clientRep.addClient(clientExchanger.getId());
        clientRep.addClient(legal.getId());
        clientRep.addClient(directLegal.getId());
        clientRep.addClient(noRefreshToken.getId());
        clientRep.addClient(serviceAccount.getId());
        clientRep.addClient(differentScopeClient.getId());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            ResourceServer server = management.realmResourceServer();
            Policy clientPolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientRep);
            management.clients().exchangeToPermission(target).addAssociatedPolicy(clientPolicy);
        }

        // permission for user impersonation for a client

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonators");
        clientImpersonateRep.addClient(directLegal.getId());
        clientImpersonateRep.addClient(directPublic.getId());
        clientImpersonateRep.addClient(directUntrustedPublic.getId());
        clientImpersonateRep.addClient(directNoSecret.getId());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            ResourceServer server = management.realmResourceServer();
            Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
            management.users().setPermissionsEnabled(true);
            management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
            management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }

        UserModel user = session.users().addUser(realm, "user");
        user.setEnabled(true);
        user.credentialManager().updateCredential(UserCredentialModel.password("password"));
        user.grantRole(exampleRole);

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            RoleModel impersonateRole = management.getRealmPermissionsClient().getRole(AdminRoles.IMPERSONATION);
            user.grantRole(impersonateRole);
        }

        UserModel bad = session.users().addUser(realm, "bad-impersonator");
        bad.setEnabled(true);
        bad.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }

    public static void setUpUserImpersonatePermissions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        AdminPermissionManagement management = AdminPermissions.management(session, realm);
        ResourceServer server = management.realmResourceServer();
        Policy userImpersonationPermission = management.users().userImpersonatedPermission();
        ClientPolicyRepresentation clientsAllowedToImpersonateRep = new ClientPolicyRepresentation();
        clientsAllowedToImpersonateRep.setName("clientsAllowedToImpersonateRep");
        clientsAllowedToImpersonateRep.addClient("direct-public");
        Policy clientsAllowedToImpersonate = management.authz().getStoreFactory().getPolicyStore().create(server, clientsAllowedToImpersonateRep);
        userImpersonationPermission.addAssociatedPolicy(clientsAllowedToImpersonate);
    }


    public static void addDirectExchanger(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        RoleModel exampleRole = realm.addRole("example");


        ClientModel target = realm.addClient("target");
        target.setName("target");
        target.setClientId("target");
        target.setDirectAccessGrantsEnabled(true);
        target.setEnabled(true);
        target.setSecret("secret");
        target.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        target.setFullScopeAllowed(false);
        target.addScopeMapping(exampleRole);

        ClientModel directExchanger = realm.addClient("direct-exchanger");
        directExchanger.setName("direct-exchanger");
        directExchanger.setClientId("direct-exchanger");
        directExchanger.setPublicClient(false);
        directExchanger.setDirectAccessGrantsEnabled(true);
        directExchanger.setEnabled(true);
        directExchanger.setSecret("secret");
        directExchanger.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        directExchanger.setFullScopeAllowed(false);

        ClientPolicyRepresentation clientImpersonateRep = new ClientPolicyRepresentation();
        clientImpersonateRep.setName("clientImpersonatorsDirect");
        clientImpersonateRep.addClient(directExchanger.getId());

        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ) || Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            AdminPermissionManagement management = AdminPermissions.management(session, realm);
            management.clients().setPermissionsEnabled(target, true);
            ResourceServer server = management.realmResourceServer();
            Policy clientImpersonatePolicy = management.authz().getStoreFactory().getPolicyStore().create(server, clientImpersonateRep);
            management.users().setPermissionsEnabled(true);
            management.users().adminImpersonatingPermission().addAssociatedPolicy(clientImpersonatePolicy);
            management.users().adminImpersonatingPermission().setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        }

        UserModel impersonatedUser = session.users().addUser(realm, "impersonated-user");
        impersonatedUser.setEnabled(true);
        impersonatedUser.credentialManager().updateCredential(UserCredentialModel.password("password"));
        impersonatedUser.grantRole(exampleRole);
    }

    public static void removeDirectExchanger(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        realm.removeClient(realm.getClientByClientId("direct-exchanger").getId());
        realm.removeClient(realm.getClientByClientId("target").getId());
        realm.removeRole(realm.getRole("example"));
        session.users().removeUser(realm, session.users().getUserByUsername(realm, "impersonated-user"));
    }
}
