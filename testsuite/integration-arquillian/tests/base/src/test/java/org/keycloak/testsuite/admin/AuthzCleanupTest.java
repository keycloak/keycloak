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

import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;

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
        testRealms.add(RealmBuilder.create().name(TEST)
                .client(ClientBuilder.create().clientId("myclient")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/myclient")
                        .defaultRoles("client-role-1", "client-role-2").build()).build());
    }

    public static void setup(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName(TEST);
        session.getContext().setRealm(realm);
        AuthorizationProvider authz = session.getProvider(AuthorizationProvider.class);
        ClientModel myclient = realm.getClientByClientId("myclient");
        ResourceServer resourceServer = authz.getStoreFactory().getResourceServerStore().findByClient(myclient.getId());
        createRolePolicy(authz, resourceServer, "client-role-1");
        createRolePolicy(authz, resourceServer, "client-role-2");
    }

    private static Policy createRolePolicy(AuthorizationProvider authz, ResourceServer resourceServer, String roleName) {
        RolePolicyRepresentation representation = new RolePolicyRepresentation();

        representation.setName(roleName);
        representation.setType("role");
        representation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        representation.setLogic(Logic.POSITIVE);
        representation.addRole(roleName, true);

        return authz.getStoreFactory().getPolicyStore().create(representation, resourceServer);
    }


    @Test
    public void testCreate() throws Exception {
        testingClient.server().run(AuthzCleanupTest::setup);
    }


}