/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.authz.admin.permissions;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jakarta.ws.rs.BadRequestException;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;

public class AdminPermissionsTest extends AbstractTestRealmKeycloakTest {

    private final String CLIENT_ID = "fgap-client";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getClients().add(ClientBuilder.create()
          .clientId(CLIENT_ID)
          .serviceAccount()
          .authorizationServicesEnabled(true)
          .build());
    }

    @Test
    public void authorizationSchemaNotAvailableFeatureDisabled() {
        List<ClientRepresentation> clients = testRealm().clients().findByClientId(CLIENT_ID);
        assertThat(clients, hasSize(1));
        ResourceServerRepresentation authorizationSettings = testRealm().clients().get(clients.get(0).getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());
    }

    @Test
    @EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ)
    public void authorizationSchemaNotAvailableFeatureV1Enabled() throws Exception {
        reconnectAdminClient();
        List<ClientRepresentation> clients = testRealm().clients().findByClientId(CLIENT_ID);
        assertThat(clients, hasSize(1));
        ResourceServerRepresentation authorizationSettings = testRealm().clients().get(clients.get(0).getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());
    }

    @Test
    @EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)
    public void authorizationSchemaAvailableFeatureV2Enabled() throws Exception {
        reconnectAdminClient();
        List<ClientRepresentation> clients = testRealm().clients().findByClientId(CLIENT_ID);
        assertThat(clients, hasSize(1));
        ResourceServerRepresentation authorizationSettings = testRealm().clients().get(clients.get(0).getId()).authorization().getSettings();
        assertThat(authorizationSettings, notNullValue());

        //admin permissions not enabled for the realm
        assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());

        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm()).setAdminPermissionsEnabled(Boolean.TRUE).update()) {
            authorizationSettings = testRealm().clients().get(clients.get(0).getId()).authorization().getSettings();
            assertThat(authorizationSettings, notNullValue());

            //schema should be available only for admin-permissions client
            assertThat(authorizationSettings.getAuthorizationSchema(), nullValue());

            //get the admin-permissions client
            ClientRepresentation adminPermissionsClient = testRealm().toRepresentation().getAdminPermissionsClient();
            assertThat(adminPermissionsClient, notNullValue());

            authorizationSettings = testRealm().clients().get(adminPermissionsClient.getId()).authorization().getSettings();
            assertThat(authorizationSettings.getAuthorizationSchema(), notNullValue());
        }
    }

    @Test
    @EnableFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)
    public void adminPermissionRESTCheck() throws Exception {
        reconnectAdminClient();

        try (RealmAttributeUpdater rau = new RealmAttributeUpdater(testRealm()).setAdminPermissionsEnabled(Boolean.TRUE).update()) {
            //get the admin-permissions client
            ClientRepresentation adminPermissionsClient = testRealm().toRepresentation().getAdminPermissionsClient();
            AuthorizationResource authorization = testRealm().clients().get(adminPermissionsClient.getId()).authorization();

            ResourceServerRepresentation rep = new ResourceServerRepresentation();
            rep.setPolicyEnforcementMode(PolicyEnforcementMode.DISABLED);
            rep.setDecisionStrategy(DecisionStrategy.CONSENSUS);

            try {
                authorization.update(rep);
            } catch (Exception ex) {
                assertThat(ex, instanceOf(BadRequestException.class));
            }

            try {
                authorization.exportSettings();
            } catch (Exception ex) {
                assertThat(ex, instanceOf(BadRequestException.class));
            }

            try {
                authorization.importSettings(rep);
            } catch (Exception ex) {
                assertThat(ex, instanceOf(BadRequestException.class));
            }

            try {
                authorization.scopes();
            } catch (Exception ex) {
                assertThat(ex, instanceOf(BadRequestException.class));
            }

            ResourcesResource resources = authorization.resources();
            try {
                ResourceRepresentation resourceRep = new ResourceRepresentation("resource-1", "manage");
                resourceRep.setType("Users");
                //it is not allowed to create resources directly
                resources.create(resourceRep);
            } catch (Exception ex) {
                assertThat(ex, instanceOf(BadRequestException.class));
            }

            // test update of the resource according to Ad minPermissionsUtils.resourceRepresentationValidation
            // fisrt it's needed to be able to create permission in context of FGAP: https://github.com/keycloak/keycloak/pull/35880
//            try {
//                //todo: create permission with a resource here
//                ResourceRepresentation resourceRep = new ResourceRepresentation();//get the resourceRep from 
//                resourceRep.setType("nonExistentType");
//                resources.resource("idOfExistingResource").update(resourceRep);
//            } catch (Exception ex) {
//                assertThat(ex, instanceOf(NotFoundException.class));
//            }
//
//            // test owner
//            // test non-existent scope
//            // test empty attributes, displayName, iconUri, uris??
        }
    }
}
