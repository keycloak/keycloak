/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class UmaPermissionTicketPushedClaimsTest extends AbstractResourceServerTest {

    @Test
    public void testEvaluatePermissionsWithPushedClaims() throws Exception {
        ResourceRepresentation resource = addResource("Bank Account", "withdraw");
        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName("Withdraw Limit Policy");

        StringBuilder code = new StringBuilder();

        code.append("var context = $evaluation.getContext();");
        code.append("var attributes = context.getAttributes();");
        code.append("var withdrawValue = attributes.getValue('my.bank.account.withdraw.value');");
        code.append("if (withdrawValue && withdrawValue.asDouble(0) <= 100) {");
        code.append("   $evaluation.grant();");
        code.append("}");

        policy.setCode(code.toString());

        AuthorizationResource authorization = getClient(getRealm()).authorization();

        authorization.policies().js().create(policy);

        ScopePermissionRepresentation representation = new ScopePermissionRepresentation();

        representation.setName("Withdraw Permission");
        representation.addScope("withdraw");
        representation.addPolicy(policy.getName());

        authorization.permissions().scope().create(representation);

        AuthzClient authzClient = getAuthzClient();
        PermissionRequest permissionRequest = new PermissionRequest(resource.getId());

        permissionRequest.addScope("withdraw");
        permissionRequest.setClaim("my.bank.account.withdraw.value", "50.5");

        PermissionResponse response = authzClient.protection("marta", "password").permission().create(permissionRequest);
        AuthorizationRequest request = new AuthorizationRequest();

        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        AuthorizationResponse authorizationResponse = authzClient.authorization().authorize(request);

        assertNotNull(authorizationResponse);
        assertNotNull(authorizationResponse.getToken());

        permissionRequest.setClaim("my.bank.account.withdraw.value", "100.5");

        response = authzClient.protection("marta", "password").permission().create(permissionRequest);
        request = new AuthorizationRequest();

        request.setTicket(response.getTicket());
        request.setClaimToken(authzClient.obtainAccessToken("marta", "password").getToken());

        try {
            authorizationResponse = authzClient.authorization().authorize(request);
            fail("Access should be denied");
        } catch (Exception ignore) {

        }
    }
}
