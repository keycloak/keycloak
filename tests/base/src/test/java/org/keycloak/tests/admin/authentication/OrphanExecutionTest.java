/*
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

package org.keycloak.tests.admin.authentication;

import java.util.HashMap;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verifies the admin API keeps working when an execution references a provider id
 * that has no registered factory (e.g. a custom Authenticator SPI was uninstalled
 * or renamed). Covers https://github.com/keycloak/keycloak/issues/15535.
 */
@KeycloakIntegrationTest
public class OrphanExecutionTest extends AbstractAuthenticationTest {

    private static final String MISSING_PROVIDER_ID = "orphan-authenticator-missing";

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void orphanExecutionDoesNotBreakFlowListing() {
        String flowAlias = "orphan-listing-flow";
        copyBrowserFlow(flowAlias);

        List<AuthenticationExecutionInfoRepresentation> before = authMgmtResource.getExecutions(flowAlias);
        String executionId = findExecutionByProvider("auth-cookie", before).getId();
        makeExecutionOrphan(executionId);

        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions(flowAlias);
        Assertions.assertEquals(before.size(), executions.size(), "Every execution of the flow must still be listed");
        for (int i = 0; i < before.size(); i++) {
            AuthenticationExecutionInfoRepresentation expected = before.get(i);
            AuthenticationExecutionInfoRepresentation actual = executions.get(i);
            Assertions.assertEquals(expected.getId(), actual.getId(), "Execution order must be preserved");
            if (!executionId.equals(expected.getId())) {
                compareExecution(expected, actual);
                Assertions.assertNull(actual.getProviderUnavailable(), "Healthy executions must not be flagged unavailable");
            }
        }

        AuthenticationExecutionInfoRepresentation orphan = findById(executionId, executions);
        Assertions.assertNotNull(orphan, "Orphan execution must still be returned");
        Assertions.assertEquals(Boolean.TRUE, orphan.getProviderUnavailable(), "providerUnavailable must be true");
        Assertions.assertEquals(Boolean.FALSE, orphan.getConfigurable(), "Orphan execution must be reported as non-configurable");
        Assertions.assertEquals(MISSING_PROVIDER_ID, orphan.getProviderId(), "providerId must be preserved");
        Assertions.assertEquals(MISSING_PROVIDER_ID, orphan.getDisplayName(), "displayName falls back to providerId when factory is missing");
        Assertions.assertEquals(List.of(orphan.getRequirement(), AuthenticationExecutionModel.Requirement.DISABLED.name()),
                orphan.getRequirementChoices(), "Orphan offers its current requirement plus DISABLED");
    }

    @Test
    public void orphanExecutionCanBeDisabled() {
        String flowAlias = "orphan-disable-flow";
        copyBrowserFlow(flowAlias);

        String executionId = findExecutionByProvider("auth-cookie", authMgmtResource.getExecutions(flowAlias)).getId();
        makeExecutionOrphan(executionId);

        AuthenticationExecutionInfoRepresentation orphan = findById(executionId, authMgmtResource.getExecutions(flowAlias));
        orphan.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.name());
        authMgmtResource.updateExecutions(flowAlias, orphan);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE,
                AdminEventPaths.authUpdateExecutionPath(flowAlias), orphan, ResourceType.AUTH_EXECUTION);

        AuthenticationExecutionInfoRepresentation updated = findById(executionId, authMgmtResource.getExecutions(flowAlias));
        Assertions.assertEquals(AuthenticationExecutionModel.Requirement.DISABLED.name(), updated.getRequirement(),
                "Disabling an orphan is the non-destructive way to unblock authentication");
        Assertions.assertEquals(List.of(AuthenticationExecutionModel.Requirement.DISABLED.name()), updated.getRequirementChoices(),
                "A disabled orphan offers no further choices");
    }

    @Test
    public void orphanExecutionCanBeRemoved() {
        String flowAlias = "orphan-removal-flow";
        copyBrowserFlow(flowAlias);

        String executionId = findExecutionByProvider("auth-cookie", authMgmtResource.getExecutions(flowAlias)).getId();
        makeExecutionOrphan(executionId);

        authMgmtResource.removeExecution(executionId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE,
                AdminEventPaths.authExecutionPath(executionId), ResourceType.AUTH_EXECUTION);

        List<AuthenticationExecutionInfoRepresentation> executions = authMgmtResource.getExecutions(flowAlias);
        Assertions.assertNull(findById(executionId, executions), "Orphan execution must be removable");
    }

    private void copyBrowserFlow(String newAlias) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("newName", newAlias);
        try (Response response = authMgmtResource.copy("browser", params)) {
            Assertions.assertEquals(201, response.getStatus(), "Copy flow");
        }
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE,
                AdminEventPaths.authCopyFlowPath("browser"), params, ResourceType.AUTH_FLOW);
    }

    private void makeExecutionOrphan(String executionId) {
        String realmName = managedRealm.getName();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            AuthenticationExecutionModel exec = realm.getAuthenticationExecutionById(executionId);
            exec.setAuthenticator(MISSING_PROVIDER_ID);
            realm.updateAuthenticatorExecution(exec);
        });
    }

    private static AuthenticationExecutionInfoRepresentation findById(String id, List<AuthenticationExecutionInfoRepresentation> executions) {
        return executions.stream().filter(e -> id.equals(e.getId())).findFirst().orElse(null);
    }
}
