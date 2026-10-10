/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.client;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.UserSessionNoteStatementMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.utils.matchers.Matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;


@KeycloakIntegrationTest
public class UserSessionNoteMapperValidationTest {

    private static final List<String> BROKER_CREDENTIAL_NOTES = List.of(
            UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN,
            AbstractOAuth2IdentityProvider.FEDERATED_REFRESH_TOKEN,
            AbstractOAuth2IdentityProvider.FEDERATED_TOKEN_EXPIRATION,
            OIDCIdentityProvider.FEDERATED_ID_TOKEN);

    @InjectRealm
    ManagedRealm realm;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectClient(config = OidcClientConfig.class)
    ManagedClient client;

    @InjectClient(ref = "saml", config = SamlClientConfig.class)
    ManagedClient samlClient;

    private ProtocolMappersResource mappers;

    @BeforeEach
    void setup() {
        mappers = client.admin().getProtocolMappers();
    }

    static Stream<String> blockedNoteNames() {
        return BROKER_CREDENTIAL_NOTES.stream().flatMap(note -> Stream.of(
                note,
                note + ":google",
                note.toLowerCase(Locale.ROOT),
                note.charAt(0) + note.substring(1).toLowerCase(Locale.ROOT)));
    }

    @ParameterizedTest(name = "session note ''{0}'' must be rejected")
    @MethodSource("blockedNoteNames")
    public void shouldRejectBrokerCredentialNote(String noteName) {
        try (Response response = mappers.createMapper(noteMapper("blocked-note-mapper", noteName))) {
            assertThat("Broker credential note '" + noteName + "' must be rejected",
                    response, Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
            assertThat(response.readEntity(OAuth2ErrorRepresentation.class).getError(),
                    containsString("cannot be mapped"));
        }
    }

    @Test
    public void shouldRejectUpdateToBlockedNoteName() {
        // First create a valid mapper
        String createdId = createMapper("safe-note-mapper", "my_custom_note");

        // Then attempt to update it to reference a blocked note name
        ProtocolMapperRepresentation existing = mappers.getMapperById(createdId);
        existing.getConfig().put(ProtocolMapperUtils.USER_SESSION_NOTE, UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN);

        ClientErrorException ex = assertThrows(
                ClientErrorException.class,
                () -> mappers.update(createdId, existing),
                "Updating mapper to FEDERATED_ACCESS_TOKEN must be rejected"
        );
        assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        assertThat(ex.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError(),
                containsString("cannot be mapped"));

        // The stored mapper must keep its original note
        assertThat(mappers.getMapperById(createdId).getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE),
                is("my_custom_note"));
    }

    @Test
    public void shouldRejectBlockedNoteViaAddModels() {
        ProtocolMapperRepresentation mapper = noteMapper("bulk-blocker", UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN);

        ClientErrorException ex = assertThrows(
                ClientErrorException.class,
                () -> mappers.createMapper(List.of(mapper)),
                "Bulk add-models with FEDERATED_ACCESS_TOKEN must be rejected"
        );
        assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        assertThat(ex.getResponse().readEntity(OAuth2ErrorRepresentation.class).getError(),
                containsString("cannot be mapped"));
    }

    /**
     * The mapper can also be written as part of the client representation, which does not go through
     * {@link ProtocolMappersResource}. This is the path a delegated administrator holding only {@code manage} on a
     * single client can use.
     */
    @Test
    public void shouldRejectBlockedNoteOnClientUpdate() {
        ClientRepresentation rep = client.admin().toRepresentation();
        rep.setProtocolMappers(List.of(noteMapper("inline-update-blocker",
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN)));

        ClientErrorException ex = assertThrows(
                ClientErrorException.class,
                () -> client.admin().update(rep),
                "Client update with an inline FEDERATED_ACCESS_TOKEN mapper must be rejected"
        );
        assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));

        // The mapper must not have been persisted
        assertThat(mappers.getMappers().stream().map(ProtocolMapperRepresentation::getName).toList(),
                not(hasItem("inline-update-blocker")));
    }

    @Test
    public void shouldRejectBlockedNoteOnClientCreate() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("inline-create-blocker-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setProtocolMappers(List.of(noteMapper("inline-create-blocker",
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN)));

        try (Response response = realm.admin().clients().create(rep)) {
            assertThat("Client creation with an inline FEDERATED_ACCESS_TOKEN mapper must be rejected",
                    response, Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }
    }

    /**
     * Realm import must report the offending client rather than failing with an opaque 500.
     */
    @Test
    public void shouldRejectBlockedNoteOnRealmImport() {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId("imported-blocker-client");
        clientRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientRep.setProtocolMappers(List.of(noteMapper("imported-blocker",
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN)));

        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm("note-mapper-import-realm");
        realmRep.setClients(List.of(clientRep));

        ClientErrorException ex = assertThrows(
                ClientErrorException.class,
                () -> adminClient.realms().create(realmRep),
                "Realm import with a FEDERATED_ACCESS_TOKEN mapper must be rejected"
        );
        assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        assertThat(ex.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage(),
                containsString("imported-blocker-client"));
    }


    @Test
    public void shouldRejectBlockedNoteOnClientScopeCreate() {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setName("inline-create-blocker-scope");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setProtocolMappers(List.of(noteMapper("scope-create-blocker",
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN)));

        try (Response response = realm.admin().clientScopes().create(rep)) {
            assertThat("Client scope creation with an inline FEDERATED_ACCESS_TOKEN mapper must be rejected",
                    response, Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        }

        // The scope is written before its mappers are validated, so the rejection relies on the transaction being
        // rolled back. A 400 that still leaves the scope behind would be worse than no validation at all.
        assertThat(realm.admin().clientScopes().findAll().stream().map(ClientScopeRepresentation::getName).toList(),
                not(hasItem("inline-create-blocker-scope")));
    }

    /**
     * Realm import must report the offending client scope rather than failing with an opaque 500.
     */
    @Test
    public void shouldRejectBlockedNoteOnClientScopeRealmImport() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName("imported-blocker-scope");
        scopeRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scopeRep.setProtocolMappers(List.of(noteMapper("imported-scope-blocker",
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN)));

        RealmRepresentation realmRep = new RealmRepresentation();
        realmRep.setRealm("note-mapper-scope-import-realm");
        realmRep.setClientScopes(List.of(scopeRep));

        ClientErrorException ex = assertThrows(
                ClientErrorException.class,
                () -> adminClient.realms().create(realmRep),
                "Realm import with a FEDERATED_ACCESS_TOKEN mapper on a client scope must be rejected"
        );
        assertThat(ex.getResponse(), Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
        assertThat(ex.getResponse().readEntity(ErrorRepresentation.class).getErrorMessage(),
                containsString("imported-blocker-scope"));

        // The scopes are imported before the clients, so a realm left half-built would still carry the mapper.
        assertThat(adminClient.realms().findAll().stream().map(RealmRepresentation::getRealm).toList(),
                not(hasItem("note-mapper-scope-import-realm")));
    }

    /**
     * The SAML mapper reads the same session notes under its own configuration key.
     */
    @Test
    public void shouldRejectBlockedNoteOnSamlMapper() {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        rep.setName("saml-blocker");
        rep.setProtocolMapper(UserSessionNoteStatementMapper.PROVIDER_ID);
        rep.setConfig(Map.of(UserSessionNoteStatementMapper.NOTE_CONFIG_KEY,
                UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN));

        try (Response response = samlClient.admin().getProtocolMappers().createMapper(rep)) {
            assertThat("SAML mapper for FEDERATED_ACCESS_TOKEN must be rejected",
                    response, Matchers.statusCodeIs(Response.Status.BAD_REQUEST));
            assertThat(response.readEntity(OAuth2ErrorRepresentation.class).getError(),
                    containsString("cannot be mapped"));
        }
    }

    @Test
    public void shouldAllowOrdinaryApplicationNote() {
        String id = createMapper("app-note-mapper", "my_app_session_note");

        assertThat(mappers.getMapperById(id).getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE),
                is("my_app_session_note"));
    }

    @Test
    public void shouldAllowNoteNameThatContainsKeywordAsSubstring() {
        String id = createMapper("substring-note", "my_FEDERATED_ACCESS_TOKEN_copy");
        assertThat(mappers.getMapperById(id).getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE),
                is("my_FEDERATED_ACCESS_TOKEN_copy"));
    }

    @Test
    public void shouldAllowNoteNameWithBlockedNameAsPrefix() {
        String noteName = UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN + "_BACKUP";
        String id = createMapper("prefix-note", noteName);

        assertThat(mappers.getMapperById(id).getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE), is(noteName));
    }

    @Test
    public void shouldAllowMapperWithoutNoteName() {
        String id = createMapper("no-note-mapper", null);
        assertThat(mappers.getMapperById(id).getConfig().get(ProtocolMapperUtils.USER_SESSION_NOTE), nullValue());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String createMapper(String name, String noteName) {
        String id = ApiUtil.getCreatedId(mappers.createMapper(noteMapper(name, noteName)));
        client.cleanup().add(c -> c.getProtocolMappers().delete(id));
        return id;
    }

    private ProtocolMapperRepresentation noteMapper(String name, String noteName) {
        ProtocolMapperRepresentation rep = new ProtocolMapperRepresentation();
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setName(name);
        rep.setProtocolMapper(UserSessionNoteMapper.PROVIDER_ID);
        Map<String, String> config = new HashMap<>();
        if (noteName != null) {
            config.put(ProtocolMapperUtils.USER_SESSION_NOTE, noteName);
        }
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "leaked_token");
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, "String");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        rep.setConfig(config);
        return rep;
    }

    public static class OidcClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("note-mapper-validation-client")
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        }
    }

    public static class SamlClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("note-mapper-validation-saml-client")
                    .protocol(SamlProtocol.LOGIN_PROTOCOL);
        }
    }
}
