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

package org.keycloak.tests.admin.realm;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.tests.utils.runonserver.RunHelpers;
import org.keycloak.util.JsonSerialization;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class RealmDefaultConfigTest extends AbstractRealmTest {

    @InjectRealm(ref = "realm-with-smtp")
    ManagedRealm smtpRealm;

    @InjectRunOnServer(ref = "smtp", realmRef = "realm-with-smtp")
    RunOnServerClient smtpRealmRunOnServer;

    @InjectAdminEvents(ref = "smtpEvents", realmRef = "realm-with-smtp")
    AdminEvents smtpRealmAdminEvents;

    @Test
    public void smtpPasswordSecret() {
        smtpRealm.updateWithCleanup(r -> r.smtp("localhost",3025, "smtp_realm@local"));

        RealmRepresentation rep = smtpRealm.admin().toRepresentation();
        rep.getSmtpServer().put("auth", "true");
        rep.getSmtpServer().put("user", "user");
        rep.getSmtpServer().put("password", "secret");

        smtpRealm.admin().update(rep);
        smtpRealmAdminEvents.clear();

        RealmRepresentation returned = smtpRealm.admin().toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, returned.getSmtpServer().get("password"));

        RealmRepresentation internalRep = smtpRealmRunOnServer.fetch(RunHelpers.internalRealm());
        assertEquals("secret", internalRep.getSmtpServer().get("password"));

        smtpRealm.admin().update(rep);

        AdminEventRepresentation event = smtpRealmAdminEvents.poll();
        assertFalse(event.getRepresentation().contains("some secret value!!"));
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        internalRep = smtpRealmRunOnServer.fetch(RunHelpers.internalRealm());
        assertEquals("secret", internalRep.getSmtpServer().get("password"));

        RealmRepresentation realm = adminClient.realms().findAll().stream()
                .filter(r -> r.getRealm().equals("realm-with-smtp")).findFirst().get();
        assertEquals(ComponentRepresentation.SECRET_VALUE, realm.getSmtpServer().get("password"));

        // updating setting the secret value with asterisks
        rep.getSmtpServer().put("password", ComponentRepresentation.SECRET_VALUE);
        smtpRealm.admin().update(rep);

        event = smtpRealmAdminEvents.poll();
        assertTrue(event.getRepresentation().contains(ComponentRepresentation.SECRET_VALUE));

        internalRep = smtpRealmRunOnServer.fetch(RunHelpers.internalRealm());
        assertEquals("secret", internalRep.getSmtpServer().get("password"));

        realm = smtpRealm.admin().toRepresentation();
        assertEquals(ComponentRepresentation.SECRET_VALUE, realm.getSmtpServer().get("password"));
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        RoleRepresentation role = new RoleRepresentation("test", "test", false);
        managedRealm.admin().roles().create(role);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourcePath("test"), role, ResourceType.REALM_ROLE);

        role = managedRealm.admin().roles().get("test").toRepresentation();
        assertNotNull(role);

        managedRealm.admin().roles().get(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName().toLowerCase()).addComposites(Collections.singletonList(role));

        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.roleResourceCompositesPath(Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + managedRealm.getName().toLowerCase()), Collections.singletonList(role), ResourceType.REALM_ROLE);

        managedRealm.admin().roles().deleteRole("test");
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.roleResourcePath("test"), ResourceType.REALM_ROLE);

        try {
            managedRealm.admin().roles().get("testsadfsadf").toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
            // Expected
        }
    }

    @Test
    public void convertKeycloakClientDescription() throws IOException {
        ClientRepresentation description = new ClientRepresentation();
        description.setClientId("client-id");
        description.setRedirectUris(Collections.singletonList("http://localhost"));

        ClientRepresentation converted = managedRealm.admin().convertClientDescription(JsonSerialization.writeValueAsString(description));
        assertEquals("client-id", converted.getClientId());
        assertEquals("http://localhost", converted.getRedirectUris().get(0));
    }

    @Test
    public void convertOIDCClientDescription() throws IOException {
        String description = IOUtils.toString(RealmDefaultConfigTest.class.getResourceAsStream("client-oidc.json"), Charset.defaultCharset());

        ClientRepresentation converted = managedRealm.admin().convertClientDescription(description);
        assertEquals(1, converted.getRedirectUris().size());
        assertEquals("http://localhost", converted.getRedirectUris().get(0));
    }

    @Test
    public void convertSAMLClientDescription() throws IOException {
        String description = IOUtils.toString(RealmDefaultConfigTest.class.getResourceAsStream("saml-entity-descriptor.xml"), Charset.defaultCharset());

        ClientRepresentation converted = managedRealm.admin().convertClientDescription(description);
        assertEquals("loadbalancer-9.siroe.com", converted.getClientId());
        assertEquals(2, converted.getRedirectUris().size());
        assertEquals("https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp", converted.getRedirectUris().get(0));
        assertEquals("https://LoadBalancer-9.siroe.com:3443/federation/Consumer/metaAlias/sp", converted.getRedirectUris().get(1));
    }

    @Test
    // KEYCLOAK-17342
    public void testDefaultSignatureAlgorithm() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        assertEquals(Constants.DEFAULT_SIGNATURE_ALGORITHM, adminClient.realm("master").toRepresentation().getDefaultSignatureAlgorithm());
        assertEquals(Constants.DEFAULT_SIGNATURE_ALGORITHM, adminClient.realm("new-realm").toRepresentation().getDefaultSignatureAlgorithm());

        adminClient.realms().realm("new-realm").remove();
    }

    @Test
    public void testSupportedOTPApplications() {
        RealmRepresentation rep = new RealmRepresentation();
        rep.setRealm("new-realm");

        adminClient.realms().create(rep);

        RealmResource realm = adminClient.realms().realm("new-realm");

        rep = realm.toRepresentation();

        List<String> supportedApplications = rep.getOtpSupportedApplications();
        assertThat(supportedApplications, hasSize(3));
        assertThat(supportedApplications, containsInAnyOrder("totpAppGoogleName", "totpAppFreeOTPName", "totpAppMicrosoftAuthenticatorName"));

        rep.setOtpPolicyDigits(8);
        realm.update(rep);

        rep = realm.toRepresentation();

        supportedApplications = rep.getOtpSupportedApplications();
        assertThat(supportedApplications, hasSize(2));
        assertThat(supportedApplications, containsInAnyOrder("totpAppFreeOTPName", "totpAppGoogleName"));

        rep.setOtpPolicyType("hotp");
        realm.update(rep);

        rep = realm.toRepresentation();

        supportedApplications = rep.getOtpSupportedApplications();
        assertThat(supportedApplications, hasSize(2));
        assertThat(supportedApplications, containsInAnyOrder("totpAppFreeOTPName", "totpAppGoogleName"));

        adminClient.realms().realm("new-realm").remove();
    }
}
