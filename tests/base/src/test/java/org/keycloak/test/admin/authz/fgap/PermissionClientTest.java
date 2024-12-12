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

package org.keycloak.test.admin.authz.fgap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.function.Supplier;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.authorization.AggregatePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ClientScopePolicyRepresentation;
import org.keycloak.representations.idm.authorization.GroupPolicyRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RegexPolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;

@KeycloakIntegrationTest(config = KeycloakAdminPermissionsServerConfig.class)
public class PermissionClientTest extends AbstractPermissionTest {

    @Test
    public void testUnsupportedPolicyTypes() {
        assertSupportForPolicyType("resource", () -> getPermissionsResource().resource().create(new ResourcePermissionRepresentation()), false);
    }

    @Test
    public void testSupportedPolicyTypes() {
        assertSupportForPolicyType("scope", () -> getPermissionsResource().scope().create(new ScopePermissionRepresentation()), true);
        assertSupportForPolicyType("user", () -> getPolicies().user().create(new UserPolicyRepresentation()), true);
        assertSupportForPolicyType("client", () -> getPolicies().client().create(new ClientPolicyRepresentation()), true);
        assertSupportForPolicyType("group", () -> getPolicies().group().create(new GroupPolicyRepresentation()), true);
        assertSupportForPolicyType("role", () -> getPolicies().role().create(new RolePolicyRepresentation()), true);
        assertSupportForPolicyType("aggregate", () -> getPolicies().aggregate().create(new AggregatePolicyRepresentation()), true);
        assertSupportForPolicyType("client-scope", () -> getPolicies().clientScope().create(new ClientScopePolicyRepresentation()), true);
        assertSupportForPolicyType("js", () -> getPolicies().js().create(new JSPolicyRepresentation()), true);
        assertSupportForPolicyType("regex", () -> getPolicies().regex().create(new RegexPolicyRepresentation()), true);
        assertSupportForPolicyType("time", () -> getPolicies().time().create(new TimePolicyRepresentation()), true);
    }

    private void assertSupportForPolicyType(String type, Supplier<Response> operation, boolean supported) {
        try (Response response = operation.get()) {
            assertPolicyEndpointResponse(type, supported, response);
        }

        PolicyRepresentation representation = new PolicyRepresentation();

        representation.setType(type);

        try (Response response = getPolicies().create(representation)) {
            assertPolicyEndpointResponse(type, supported, response);
        }
    }

    private void assertPolicyEndpointResponse(String type, boolean supported, Response response) {
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), Status.BAD_REQUEST.equals(Status.fromStatusCode(response.getStatus())), not(supported));
        assertThat("Policy type [" + type + "] should be " + (supported ? "supported" : "unsupported"), response.readEntity(String.class).contains("Policy type not supported by feature"), not(supported));
    }
}
