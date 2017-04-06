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
package org.keycloak.testsuite.admin;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthzCleanupTest extends AbstractKeycloakTest {

    @Deployment
    public static WebArchive deploy() {
        return RunOnServerDeployment.create();
    }

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
        ClientModel client = session.realms().addClient(realm, "myclient");
        RoleModel role1 = client.addRole("client-role1");
        RoleModel role2 = client.addRole("client-role2");

        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().create(client.getId());
        createRolePolicy(authz, resourceServer, role1);
        createRolePolicy(authz, resourceServer, role2);


    }

    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, RoleModel role) {
        Policy policy = authz.getStoreFactory().getPolicyStore().create(role.getName(), "role", resourceServer);

        String roleValues = "[{\"id\":\"" + role.getId() + "\",\"required\": true}]";
        policy.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        policy.setLogic(Logic.POSITIVE);
        Map<String, String> config = new HashMap<>();
        config.put("roles", roleValues);
        policy.setConfig(config);
        return policy;
    }


    @Test
    public void testCreate() throws Exception {
        testingClient.server().run(AuthzCleanupTest::setup);
    }


}