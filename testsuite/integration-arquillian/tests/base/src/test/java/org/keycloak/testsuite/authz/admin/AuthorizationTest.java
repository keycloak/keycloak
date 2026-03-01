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

package org.keycloak.testsuite.authz.admin;

import java.util.List;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationTest extends AbstractAuthorizationTest {

    @Test
    public void testEnableAuthorizationServices() {
        ClientResource clientResource = getClientResource();
        ClientRepresentation resourceServer = getResourceServer();
        RealmResource realm = realmsResouce().realm(getRealmId());

        UserRepresentation serviceAccount = realm.users().search(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + resourceServer.getClientId()).get(0);
        Assert.assertNotNull(serviceAccount);
        List<RoleRepresentation> serviceAccountRoles = realm.users().get(serviceAccount.getId()).roles().clientLevel(resourceServer.getId()).listEffective();
        Assert.assertTrue(serviceAccountRoles.stream().anyMatch(roleRepresentation -> "uma_protection".equals(roleRepresentation.getName())));

        enableAuthorizationServices(false);
        enableAuthorizationServices(true);

        serviceAccount = clientResource.getServiceAccountUser();
        Assert.assertNotNull(serviceAccount);
        realm = realmsResouce().realm(getRealmId());
        serviceAccountRoles = realm.users().get(serviceAccount.getId()).roles().clientLevel(resourceServer.getId()).listEffective();
        Assert.assertTrue(serviceAccountRoles.stream().anyMatch(roleRepresentation -> "uma_protection".equals(roleRepresentation.getName())));

        RolePolicyRepresentation policy = new RolePolicyRepresentation();

        policy.setName("should be removed");
        policy.addRole("uma_authorization");

        clientResource.authorization().policies().role().create(policy);

        List<PolicyRepresentation> policies = clientResource.authorization().policies().policies();
        assertEquals(1, policies.size());

        enableAuthorizationServices(false);
        enableAuthorizationServices(true);

        ResourceServerRepresentation settings = clientResource.authorization().getSettings();

        assertEquals(PolicyEnforcerConfig.EnforcementMode.ENFORCING.name(), settings.getPolicyEnforcementMode().name());
        assertTrue(settings.isAllowRemoteResourceManagement());
        assertEquals(resourceServer.getId(), settings.getClientId());

        policies = clientResource.authorization().policies().policies();
        assertTrue(policies.isEmpty());

        serviceAccount = clientResource.getServiceAccountUser();
        Assert.assertNotNull(serviceAccount);
        serviceAccountRoles = realm.users().get(serviceAccount.getId()).roles().clientLevel(resourceServer.getId()).listEffective();
        Assert.assertTrue(serviceAccountRoles.stream().anyMatch(roleRepresentation -> "uma_protection".equals(roleRepresentation.getName())));
    }
}
