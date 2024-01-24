/*
 *
 *  * Copyright 2023  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.keycloak.testsuite.user.profile;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.BearerAuthFilter;
import org.keycloak.admin.ui.rest.model.UIRealmRepresentation;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPAttributeRequired;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

/**
 *
 * @author rmartinc
 */
public class UIRealmResourceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testNoUpdateUserProfile() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        updateRealmExt(toUIRealmRepresentation(rep, null));

        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        assertAdminEvents.assertEmpty();
    }

    @Test
    public void testSameUpdateUserProfile() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();

        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        assertAdminEvents.assertEmpty();
    }

    @Test
    public void testUpdateUserProfileModification() throws IOException {
        RealmRepresentation rep = testRealm().toRepresentation();
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.addOrReplaceAttribute(new UPAttribute("foo",
                new UPAttributePermissions(Set.of(), Set.of(UPConfigUtils.ROLE_USER, UPConfigUtils.ROLE_ADMIN))));

        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        AdminEventRepresentation adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        Assert.assertNotNull(adminEvent.getRepresentation());
        adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
        Assert.assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

        upConfig.getAttribute("foo").setDisplayName("Foo");
        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
        Assert.assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

        upConfig.getAttribute("foo").setPermissions(new UPAttributePermissions(Set.of(), Set.of(UPConfigUtils.ROLE_USER)));
        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
        Assert.assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

        upConfig.getAttribute("foo").setRequired(new UPAttributeRequired(Set.of(UPConfigUtils.ROLE_ADMIN, UPConfigUtils.ROLE_USER), Set.of()));
        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
        Assert.assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

        upConfig.getAttribute("foo").setValidations(Map.of("length", Map.of("min", "3", "max", "128")));
        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        adminEvent = assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, "ui-ext", ResourceType.USER_PROFILE);
        Assert.assertEquals(upConfig, toUpConfig(adminEvent.getRepresentation()));

        updateRealmExt(toUIRealmRepresentation(rep, upConfig));
        assertAdminEvents.assertEvent(TEST_REALM_NAME, OperationType.UPDATE, Matchers.nullValue(String.class), ResourceType.REALM);
        assertAdminEvents.assertEmpty();
    }

    private void updateRealmExt(UIRealmRepresentation rep) {
        try (Client client = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            Response response = client.target(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth")
                    .path("/admin/realms/" + rep.getRealm() + "/ui-ext")
                    .register(new BearerAuthFilter(adminClient.tokenManager()))
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.entity(rep, MediaType.APPLICATION_JSON));
            Assert.assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    private UIRealmRepresentation toUIRealmRepresentation(RealmRepresentation realm, UPConfig upConfig) throws IOException {
        UIRealmRepresentation uiRealm = JsonSerialization.readValue(JsonSerialization.writeValueAsString(realm), UIRealmRepresentation.class);
        uiRealm.setUpConfig(upConfig);
        return uiRealm;
    }

    private UPConfig toUpConfig(String representation) throws IOException {
        return JsonSerialization.readValue(representation, UPConfig.class);
    }
}
