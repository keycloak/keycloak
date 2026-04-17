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

package org.keycloak.testsuite.organization.admin;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.RealmBuilder;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

/**
 * Organization tests that still require legacy broker infrastructure (BrokerConfiguration,
 * cross-realm realmsResouce()). The bulk of OrganizationTest has moved to the new test framework
 * at tests/base/src/test/java/org/keycloak/tests/organization/admin/OrganizationTest.java.
 */
public class OrganizationTest extends AbstractOrganizationTest {

    @Test
    public void testDeleteRealm() {
        RealmRepresentation realmRep = RealmBuilder.create()
                .name(KeycloakModelUtils.generateId())
                .organizationEnabled(true)
                .build();
        RealmResource realmRes = realmsResouce().realm(realmRep.getRealm());

        try {
            realmRep.setEnabled(true);
            realmsResouce().create(realmRep);
            realmRes = realmsResouce().realm(realmRep.getRealm());
            realmRes.toRepresentation();

            createOrganization(realmRes, "test-org", "test.org");

            List<OrganizationRepresentation> orgs = realmRes.organizations().list(-1, -1);
            assertThat(orgs, hasSize(1));

            IdentityProviderRepresentation broker = bc.setUpIdentityProvider();
            broker.setAlias(KeycloakModelUtils.generateId());
            try (Response response = realmRes.identityProviders().create(broker)) {
                assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            }
            try (Response response = realmRes.organizations().get(orgs.get(0).getId()).identityProviders().addIdentityProvider(broker.getAlias())) {
                assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
            }
        } finally {
            realmRes.remove();
        }
    }
}
