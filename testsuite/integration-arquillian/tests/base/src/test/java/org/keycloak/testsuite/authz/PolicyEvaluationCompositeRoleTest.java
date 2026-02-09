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
package org.keycloak.testsuite.authz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.AuthorizationProviderFactory;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyEvaluationCompositeRoleTest extends AbstractAuthzTest {

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

        ClientModel client = session.clients().addClient(realm, "myclient");
        RoleModel role1 = client.addRole("client-role1");


        AuthorizationProviderFactory factory = (AuthorizationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(AuthorizationProvider.class);
        AuthorizationProvider authz = factory.create(session, realm);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(client);
        Policy policy = createRolePolicy(authz, resourceServer, role1);

        Scope scope = authz.getStoreFactory().getScopeStore().create(resourceServer, "myscope");
        Resource resource = authz.getStoreFactory().getResourceStore().create(resourceServer, "myresource", resourceServer.getClientId());
        addScopePermission(authz, resourceServer, "mypermission", resource, scope, policy);

        RoleModel composite = realm.addRole("composite");
        composite.addCompositeRole(role1);

        UserModel user = session.users().addUser(realm, "user");
        user.grantRole(composite);
    }

    private static Policy addScopePermission(AuthorizationProvider authz, ResourceServer resourceServer, String name, Resource resource, Scope scope, Policy policy) {
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


    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setName(role.getName());
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


    @Test
    public void testCreate() throws Exception {
        testingClient.server().run(PolicyEvaluationCompositeRoleTest::setup);

        RealmResource realm = adminClient.realm(TEST);
        String resourceServerId = realm.clients().findByClientId("myclient").get(0).getId();
        UserRepresentation user = realm.users().search("user").get(0);

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();
        request.setUserId(user.getId());
        request.setClientId(resourceServerId);
        request.addResource("myresource", "myscope");
        PolicyEvaluationResponse result = realm.clients().get(resourceServerId).authorization().policies().evaluate(request);
        Assert.assertEquals(DecisionEffect.PERMIT, result.getStatus());
    }


}
