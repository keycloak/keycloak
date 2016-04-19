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

import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.Assert;

import javax.ws.rs.NotFoundException;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class IdentityProviderTest extends AbstractAdminTest {

    @Test
    public void testFindAll() {
        realm.identityProviders().create(create("google", "google", "Google"));
        realm.identityProviders().create(create("facebook", "facebook", "Facebook"));

        Assert.assertNames(realm.identityProviders().findAll(), "google", "facebook");
    }

    @Test
    public void testCreate() {
        IdentityProviderRepresentation newIdentityProvider = create("new-identity-provider", "oidc", "New Identity Provider");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("new-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertNotNull(representation.getInternalId());
        assertEquals("new-identity-provider", representation.getAlias());
        assertEquals("oidc", representation.getProviderId());
        assertEquals("clientId", representation.getConfig().get("clientId"));
        assertEquals("clientSecret", representation.getConfig().get("clientSecret"));
        assertTrue(representation.isEnabled());
        assertFalse(representation.isStoreToken());
        assertFalse(representation.isTrustEmail());
    }

    @Test
    public void testUpdate() {
        IdentityProviderRepresentation newIdentityProvider = create("update-identity-provider", "oidc", "Update Identity Provider");

        newIdentityProvider.getConfig().put("clientId", "clientId");
        newIdentityProvider.getConfig().put("clientSecret", "clientSecret");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        assertEquals("update-identity-provider", representation.getAlias());

        representation.setAlias("changed-alias");
        representation.setEnabled(false);
        representation.setStoreToken(true);
        representation.getConfig().put("clientId", "changedClientId");

        identityProviderResource.update(representation);

        identityProviderResource = realm.identityProviders().get(representation.getInternalId());

        assertNotNull(identityProviderResource);

        representation = identityProviderResource.toRepresentation();

        assertFalse(representation.isEnabled());
        assertTrue(representation.isStoreToken());
        assertEquals("changedClientId", representation.getConfig().get("clientId"));
    }

    @Test(expected = NotFoundException.class)
    public void testRemove() {
        IdentityProviderRepresentation newIdentityProvider = create("remove-identity-provider", "saml", "Remove Identity Provider");

        realm.identityProviders().create(newIdentityProvider);
        IdentityProviderResource identityProviderResource = realm.identityProviders().get("update-identity-provider");

        assertNotNull(identityProviderResource);

        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

        assertNotNull(representation);

        identityProviderResource.remove();

        realm.identityProviders().get("update-identity-provider");
    }

    private IdentityProviderRepresentation create(String id, String providerId, String name) {
        IdentityProviderRepresentation identityProviderRepresentation = new IdentityProviderRepresentation();

        identityProviderRepresentation.setAlias(id);
        identityProviderRepresentation.setProviderId(providerId);
        identityProviderRepresentation.setEnabled(true);

        return identityProviderRepresentation;
    }

}
