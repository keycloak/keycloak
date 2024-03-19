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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractOrganizationTest extends AbstractAdminTest  {

    protected OrganizationRepresentation createOrganization() {
        return createOrganization("neworg");
    }

    protected OrganizationRepresentation createOrganization(String name) {
        OrganizationRepresentation org = new OrganizationRepresentation();

        org.setName(name);

        String id;

        try (Response response = testRealm().organizations().create(org)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            id = ApiUtil.getCreatedId(response);
        }

        org.setId(id);
        getCleanup().addCleanup(() -> testRealm().organizations().get(id).delete().close());

        return org;
    }

    protected UserRepresentation addMember(OrganizationResource organization) {
        return addMember(organization, "jdoe@neworg.org");
    }

    protected UserRepresentation addMember(OrganizationResource organization, String email) {
        UserRepresentation expected = new UserRepresentation();

        expected.setEmail(email);
        expected.setUsername(expected.getEmail());

        try (Response response = organization.members().addMember(expected)) {
            assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
            String id = ApiUtil.getCreatedId(response);
            UserRepresentation actual = organization.members().member(id).toRepresentation();

            assertNotNull(expected);
            assertEquals(id, actual.getId());
            assertEquals(expected.getUsername(), actual.getUsername());
            assertEquals(expected.getEmail(), actual.getEmail());

            return actual;
        }
    }
}
