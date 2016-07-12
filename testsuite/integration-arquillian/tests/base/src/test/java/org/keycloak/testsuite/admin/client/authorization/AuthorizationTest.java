/*
  Copyright 2016 Red Hat, Inc. and/or its affiliates
  and other contributors as indicated by the @author tags.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package org.keycloak.testsuite.admin.client.authorization;

import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTest extends AbstractAuthorizationTest {

    @Test
    public void testEnableAuthorizationServices() {
        ClientResource clientResource = getClientResource();
        ClientRepresentation resourceServer = getResourceServer();

        enableAuthorizationServices();

        ResourceServerRepresentation settings = clientResource.authorization().getSettings();

        assertEquals(PolicyEnforcerConfig.EnforcementMode.ENFORCING.name(), settings.getPolicyEnforcementMode().name());
        assertEquals(resourceServer.getId(), settings.getClientId());
        List<ResourceRepresentation> defaultResources = clientResource.authorization().resources().resources();

        assertEquals(1, defaultResources.size());

        List<PolicyRepresentation> defaultPolicies = clientResource.authorization().policies().policies();

        assertEquals(2, defaultPolicies.size());
    }
}