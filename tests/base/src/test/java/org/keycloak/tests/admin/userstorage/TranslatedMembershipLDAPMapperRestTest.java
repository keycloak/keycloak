/*
 * Copyright 2026 Andrzej Świętek
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
package org.keycloak.tests.admin.userstorage;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipConfig;
import org.keycloak.storage.ldap.mappers.membership.translation.TranslatedMembershipLDAPStorageMapperFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@KeycloakIntegrationTest
public class TranslatedMembershipLDAPMapperRestTest extends AbstractUserStorageRestTest {

    private String ldapProviderId;

    @BeforeEach
    public void createLDAPProvider() {
        ldapProviderId = createComponent(createBasicLDAPProviderRep());
    }

    @AfterEach
    public void removeLDAPProvider() {
        removeComponent(ldapProviderId);
    }

    @Test
    public void acceptsPatternWithTemplate() {
        try (Response response = addMapper(Map.of(
                TranslatedMembershipConfig.TRANSLATION_PATTERN, "myapp-(.+)-users",
                TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "$1"))) {
            assertThat(response.getStatus(), equalTo(201));
        }
    }

    @Test
    public void rejectsMapperWithoutTranslation() {
        assertRejected(Map.of(), "Configure a translation table, a translation pattern with a template, or both.");
    }

    @Test
    public void rejectsTemplateReferringToMissingCaptureGroup() {
        assertRejected(Map.of(
                        TranslatedMembershipConfig.TRANSLATION_PATTERN, "(?<app>[A-Z]{2,5})_(.+)",
                        TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "${role}"),
                "Translation template refers to ${role}, but the pattern has no capture group named 'role'.");
    }

    @Test
    public void rejectsClientRolesOfUnknownClient() {
        assertRejected(Map.of(
                        TranslatedMembershipConfig.TRANSLATION_PATTERN, "(.+)",
                        TranslatedMembershipConfig.TRANSLATION_TEMPLATE, "$1",
                        TranslatedMembershipConfig.TARGET, "CLIENT_ROLE",
                        TranslatedMembershipConfig.CLIENT_ID, "no-such-client"),
                "There is no client 'no-such-client' in the realm.");
    }

    private void assertRejected(Map<String, String> config, String problem) {
        try (Response response = addMapper(config)) {
            assertThat(response.getStatus(), equalTo(400));
            assertThat(response.readEntity(ErrorRepresentation.class).getErrorMessage(), containsString(problem));
        }
    }

    private Response addMapper(Map<String, String> config) {
        ComponentRepresentation mapper = new ComponentRepresentation();
        mapper.setName("translated-membership");
        mapper.setProviderId(TranslatedMembershipLDAPStorageMapperFactory.PROVIDER_ID);
        mapper.setProviderType(LDAPStorageMapper.class.getName());
        mapper.setParentId(ldapProviderId);
        mapper.setConfig(new MultivaluedHashMap<>());
        config.forEach(mapper.getConfig()::putSingle);
        return managedRealm.admin().components().add(mapper);
    }
}
