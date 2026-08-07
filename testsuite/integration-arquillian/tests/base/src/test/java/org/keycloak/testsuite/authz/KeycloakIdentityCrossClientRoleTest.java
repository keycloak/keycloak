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
package org.keycloak.testsuite.authz;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
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
import org.keycloak.representations.idm.RealmRepresentation;
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

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

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
 *       enriches the synthetic token with all user role mappings, yielding
 *       PERMIT for the same scenario. This guards against regressions in the
 *       admin-console code path after the helper refactor.</li>
 *   <li>{@link #enrichedTokenIdentity_includesCrossClientRole_yieldsPermit()}
 *       proves the helper's value: after invoking
 *       {@link TokenIdentityEnricher#addAllUserRoles(AccessToken, UserModel)},
 *       a token-bound identity grants the same permission as the admin
 *       console.</li>
 * </ul>
 */
public class KeycloakIdentityCrossClientRoleTest extends AbstractAuthzTest {

    private static final String CLIENT_A = "resource-server-client-a";
    private static final String CLIENT_B = "role-container-client-b";
    private static final String CLIENT_B_ROLE = "special";
    private static final String USER_NAME = "cross-client-user";
    private static final String RESOURCE = "myresource";
    private static final String SCOPE = "myscope";
    private static final String PERMISSION = "mypermission";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(TEST);
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    public static void setup(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        session.getContext().setRealm(realm);

        // Idempotent: each @Test invokes setup, but the realm persists across
        // the run, so subsequent calls become no-ops.
        if (realm.getClientByClientId(CLIENT_A) != null) {
            return;
        }

        // client-b: role container (not a resource server, not in client-a's scope)
        ClientModel clientB = session.clients().addClient(realm, CLIENT_B);
        RoleModel role = clientB.addRole(CLIENT_B_ROLE);

        // client-a: resource server with a role-policy referencing client-b's role.
        // fullScopeAllowed=false is the production-realistic setting: only roles
        // explicitly mapped into client-a's scope appear in tokens it issues.
        // Without this, the access token would carry client-b roles via the
        // full-scope shortcut and the cross-client gap would not reproduce.
        ClientModel clientA = session.clients().addClient(realm, CLIENT_A);
        clientA.setFullScopeAllowed(false);

        AuthorizationProviderFactory factory = (AuthorizationProviderFactory)
                session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authz = factory.create(session, realm);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(clientA);
        Policy policy = createRolePolicy(authz, resourceServer, role);

        Scope scope = authz.getStoreFactory().getScopeStore().create(resourceServer, SCOPE);
        Resource resource = authz.getStoreFactory().getResourceStore()
                .create(resourceServer, RESOURCE, resourceServer.getClientId());
        addScopePermission(authz, resourceServer, PERMISSION, resource, scope, policy);

        UserModel user = session.users().addUser(realm, USER_NAME);
        user.grantRole(role);
    }

    public static void evaluateWithEnrichedTokenIdentity(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        session.getContext().setRealm(realm);

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
        config.put("fetchRoles", Boolean.TRUE.toString());
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
        testingClient.server().run(KeycloakIdentityCrossClientRoleTest::setup);

        RealmResource realm = adminClient.realm(TEST);
        String resourceServerId = realm.clients().findByClientId(CLIENT_A).get(0).getId();
        UserRepresentation user = realm.users().search(USER_NAME).get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource(RESOURCE, SCOPE);

        PolicyEvaluationResponse result = realm.clients().get(resourceServerId)
                .authorization().policies().evaluate(request);
        Assertions.assertEquals(DecisionEffect.PERMIT, result.getStatus(),
                "Admin console must grant access via internal role enrichment.");
    }

    @Test
    public void enrichedTokenIdentity_includesCrossClientRole_yieldsPermit() {
        testingClient.server().run(KeycloakIdentityCrossClientRoleTest::setup);
        testingClient.server().run(KeycloakIdentityCrossClientRoleTest::evaluateWithEnrichedTokenIdentity);
    }
}
