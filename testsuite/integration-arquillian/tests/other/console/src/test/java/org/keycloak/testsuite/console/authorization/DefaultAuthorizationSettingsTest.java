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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.console.page.clients.authorization.AuthorizationSettingsForm;
import org.keycloak.testsuite.console.page.clients.authorization.permission.Permissions;
import org.keycloak.testsuite.console.page.clients.authorization.policy.Policies;
import org.keycloak.testsuite.console.page.clients.authorization.resource.Resources;
import org.keycloak.testsuite.console.page.clients.authorization.scope.Scopes;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DefaultAuthorizationSettingsTest extends AbstractAuthorizationSettingsTest {

    @Test
    public void testDefaultSettings() {
        AuthorizationSettingsForm settings = authorizationPage.settings();

        assertEquals(PolicyEnforcerConfig.EnforcementMode.ENFORCING, settings.getEnforcementMode());
        assertEquals(false, settings.isAllowRemoteResourceManagement());

        Resources resources = authorizationPage.authorizationTabs().resources();
        ResourceRepresentation resource = resources.resources().findByName("Default Resource");

        assertNotNull(resource);
        assertEquals("urn:oidc-confidetial:resources:default", resource.getType());
        assertEquals("/*", resource.getUri());
        assertEquals(newClient.getClientId(), resource.getOwner().getName());

        Scopes scopes = authorizationPage.authorizationTabs().scopes();

        assertTrue(scopes.scopes().getTableRows().isEmpty());

        Permissions permissions = authorizationPage.authorizationTabs().permissions();
        PolicyRepresentation permission = permissions.permissions().findByName("Default Permission");

        assertNotNull(permission);
        assertEquals("resource", permission.getType());

        Policies policies = authorizationPage.authorizationTabs().policies();
        PolicyRepresentation policy = policies.policies().findByName("Default Policy");

        assertNotNull(policy);
        assertEquals("js", policy.getType());
    }
}
