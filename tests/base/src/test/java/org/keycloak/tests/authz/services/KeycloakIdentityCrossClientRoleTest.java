/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.authz.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.common.TokenIdentityEnricher;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.permission.evaluator.PermissionEvaluator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates the cross-client role gap addressed by the
 * {@code TokenIdentityEnricher} contribution.
 *
 * <p>Scenario: a resource on {@code client-a} is protected by a role policy
 * referencing a client role defined in {@code client-b}. The user holds the
 * {@code client-b} role.
 *
 * <ul>
 *   <li>{@link #adminConsoleEvaluate_includesCrossClientRole_yieldsPermit()}
 *       captures the admin console reference: {@code PolicyEvaluationService}
 *       enriches the synthetic token with the user's role mappings, yielding
 *       PERMIT for the same scenario. This guards against regressions in the
 *       admin-console code path after the helper refactor.</li>
 *   <li>{@link #plainTokenIdentity_missesCrossClientRole_yieldsDeny()} pins
 *       the gap itself: an identity built from the raw client token is denied,
 *       because the {@code client-b} role never reaches the token.</li>
 *   <li>{@link #enrichedTokenIdentity_includesCrossClientRole_yieldsPermit()}
 *       proves the helper's value: after invoking
 *       {@link TokenIdentityEnricher#addAllUserRoles(AccessToken, UserModel)},
 *       a token-bound identity grants the same permission as the admin
 *       console.</li>
 * </ul>
 */
@KeycloakIntegrationTest
public class KeycloakIdentityCrossClientRoleTest {

    private static final String CLIENT_A = "resource-server-client-a";
    private static final String CLIENT_B = "role-container-client-b";
    private static final String CLIENT_B_ROLE = "special";
    private static final String USER_NAME = "cross-client-user";
    private static final String RESOURCE = "myresource";
    private static final String SCOPE = "myscope";
    private static final String PERMISSION = "mypermission";

    @InjectRealm(config = CrossClientRoleRealmConfig.class)
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void createAuthorizationConfig() {
        runOnServer.run(KeycloakIdentityCrossClientRoleTest::setup);
    }

    public static void setup(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ClientModel clientA = realm.getClientByClientId(CLIENT_A);

        // Idempotent: the realm outlives a single test, so later invocations
        // become no-ops.
        if (authz.getStoreFactory().getResourceServerStore().findByClient(clientA) != null) {
            return;
        }

        RoleModel role = realm.getClientByClientId(CLIENT_B).getRole(CLIENT_B_ROLE);

        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(clientA);
        Policy policy = createRolePolicy(authz, resourceServer, role);

        Scope scope = authz.getStoreFactory().getScopeStore().create(resourceServer, SCOPE);
        Resource resource = authz.getStoreFactory().getResourceStore()
                .create(resourceServer, RESOURCE, resourceServer.getClientId());
        resource.updateScopes(Set.of(scope));
        addScopePermission(authz, resourceServer, PERMISSION, resource, scope, policy);
    }

    public static void evaluateWithPlainTokenIdentity(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel clientA = realm.getClientByClientId(CLIENT_A);
        UserModel user = session.users().getUserByUsername(realm, USER_NAME);

        AccessToken token = synthesizeClientToken(session, realm, clientA, user);

        KeycloakIdentity identity = new KeycloakIdentity(token, session, realm);

        Collection<Permission> permissions = evaluateResourcePermission(session, clientA, identity);

        Assertions.assertTrue(
                permissions.isEmpty(),
                "Expected the un-enriched identity to be denied: the client-b role is outside "
                        + "client-a's scope, so it is absent from the token. If this grants access, "
                        + "the test no longer exercises the gap the enrichment closes.");
    }

    public static void evaluateWithEnrichedTokenIdentity(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel clientA = realm.getClientByClientId(CLIENT_A);
        UserModel user = session.users().getUserByUsername(realm, USER_NAME);

        AccessToken token = synthesizeClientToken(session, realm, clientA, user);
        TokenIdentityEnricher.addAllUserRoles(token, user);

        KeycloakIdentity identity = new KeycloakIdentity(token, session, realm);

        Collection<Permission> permissions = evaluateResourcePermission(session, clientA, identity);

        Assertions.assertFalse(
                permissions.isEmpty(),
                "Expected enriched identity to grant the cross-client role policy. "
                        + "If empty, the enrichment loop or evaluator wiring regressed.");
    }

    private static AccessToken synthesizeClientToken(KeycloakSession session, RealmModel realm,
                                                     ClientModel client, UserModel user) {
        AuthenticationSessionModel authSession = session.authenticationSessions()
                .createRootAuthenticationSession(realm)
                .createAuthenticationSession(client);
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAuthenticatedUser(user);

        UserSessionModel userSession = new UserSessionManager(session).createUserSession(
                authSession.getParentSession().getId(), realm, user,
                user.getUsername(), "127.0.0.1", "passwd", false, null, null,
                UserSessionModel.SessionPersistenceState.PERSISTENT);

        AuthenticationManager.setClientScopesInSession(session, authSession);
        ClientSessionContext ctx = TokenManager.attachAuthenticationSession(session, userSession, authSession);

        return new TokenManager().createClientAccessToken(session, realm, client, user, userSession, ctx,
                ctx.isOfflineTokenRequested());
    }

    private static Collection<Permission> evaluateResourcePermission(KeycloakSession session,
                                                                     ClientModel clientA,
                                                                     KeycloakIdentity identity) {
        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(clientA);
        Resource resource = authz.getStoreFactory().getResourceStore().findByName(resourceServer, RESOURCE);
        Scope scope = authz.getStoreFactory().getScopeStore().findByName(resourceServer, SCOPE);

        PermissionEvaluator evaluator = authz.evaluators().from(
                Arrays.asList(new ResourcePermission(resource, Arrays.asList(scope), resourceServer)),
                new DefaultEvaluationContext(identity, session));
        return evaluator.evaluate(resourceServer, null);
    }

    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        PolicyRepresentation representation = new PolicyRepresentation();
        representation.setName(role.getName() + "-policy");
        representation.setType("role");
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        String roleValues = "[{\"id\":\"" + role.getId() + "\",\"required\": true}]";
        Map<String, String> config = new HashMap<>();
        config.put("roles", roleValues);
        // fetchRoles is deliberately left off: with it enabled RolePolicyProvider
        // reloads the subject and calls UserModel#hasRole, bypassing the identity
        // altogether, so these tests would pass even without the enrichment. The
        // default resolves the role from the identity's token claims, which is the
        // path the helper feeds.
        representation.setConfig(config);

        return authz.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    private static Policy addScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name,
                                             Resource resource, Scope scope, Policy policy) {
        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();
        representation.setName(name);
        representation.setType("scope");
        representation.addResource(resource.getName());
        representation.addScope(scope.getName());
        representation.addPolicy(policy.getName());
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);

        return authz.getStoreFactory().getPolicyStore().create(resourceServer, representation);
    }

    @Test
    public void adminConsoleEvaluate_includesCrossClientRole_yieldsPermit() {
        String resourceServerId = realm.admin().clients().findByClientId(CLIENT_A).get(0).getId();
        UserRepresentation user = realm.admin().users().search(USER_NAME).get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource(RESOURCE, SCOPE);

        PolicyEvaluationResponse result = realm.admin().clients().get(resourceServerId)
                .authorization().policies().evaluate(request);
        Assertions.assertEquals(DecisionEffect.PERMIT, result.getStatus(),
                "Admin console must grant access via internal role enrichment.");
    }

    @Test
    public void plainTokenIdentity_missesCrossClientRole_yieldsDeny() {
        runOnServer.run(KeycloakIdentityCrossClientRoleTest::evaluateWithPlainTokenIdentity);
    }

    @Test
    public void enrichedTokenIdentity_includesCrossClientRole_yieldsPermit() {
        runOnServer.run(KeycloakIdentityCrossClientRoleTest::evaluateWithEnrichedTokenIdentity);
    }

    private static class CrossClientRoleRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            // client-b: role container, not a resource server and not in client-a's scope.
            realm.clients(ClientBuilder.create().clientId(CLIENT_B));
            realm.clientRoles(CLIENT_B, CLIENT_B_ROLE);

            // client-a: resource server holding the permission. fullScopeEnabled=false is
            // the production-realistic setting: only roles explicitly mapped into
            // client-a's scope appear in the tokens it issues. Without it the access token
            // would carry client-b roles via the full-scope shortcut and the cross-client
            // gap would not reproduce.
            realm.clients(ClientBuilder.create().clientId(CLIENT_A).fullScopeEnabled(false));

            realm.users(UserBuilder.create().username(USER_NAME).clientRoles(CLIENT_B, CLIENT_B_ROLE));

            return realm;
        }
    }
}
