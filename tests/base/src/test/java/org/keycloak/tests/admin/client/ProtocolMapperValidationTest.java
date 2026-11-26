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

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A protocol mapper used to be validated only when it was written through {@code ProtocolMappersResource}, so a
 * mapper that the per-mapper endpoint rejects was accepted when it was sent as part of a client or a client scope.
 * Every mapper of a container is now validated whenever the container is written as a whole.
 * <p>
 * The pairwise subject identifier mapper is used here because it rejects a client whose redirect URIs span several
 * hosts, which is a condition of the client rather than of the mapper, and because its validation initializes the
 * configuration it is given.
 */
@KeycloakIntegrationTest
public class ProtocolMapperValidationTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Test
    public void shouldRejectInvalidMapperOnClientCreate() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("pairwise-create-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://one.example.org/callback", "https://two.example.org/callback"));
        rep.setProtocolMappers(List.of(pairwiseMapperWithoutSalt()));

        try (Response response = realm.admin().clients().create(rep)) {
            assertThat("A client whose redirect URIs span several hosts cannot carry a pairwise mapper",
                    response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        }

        assertThat(realm.admin().clients().findByClientId("pairwise-create-client"), is(empty()));
    }

    /**
     * The mapper is valid when it is created and only the client changes afterwards, so the update can only be
     * rejected if the mappers already stored on the client are validated as well.
     */
    @Test
    public void shouldRejectClientUpdateThatInvalidatesAStoredMapper() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("pairwise-update-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://one.example.org/callback"));
        rep.setProtocolMappers(List.of(pairwiseMapperWithoutSalt()));
        String clientId;
        try (Response response = realm.admin().clients().create(rep)) {
            clientId = ApiUtil.getCreatedId(response);
        }

        var client = realm.admin().clients().get(clientId);
        ClientRepresentation update = client.toRepresentation();
        update.setRedirectUris(List.of("https://one.example.org/callback", "https://two.example.org/callback"));

        ClientErrorException ex = assertThrows(ClientErrorException.class, () -> client.update(update),
                "Adding a redirect URI on a second host must be rejected while the pairwise mapper is stored");
        assertThat(ex.getResponse().getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        assertThat(client.toRepresentation().getRedirectUris(), is(List.of("https://one.example.org/callback")));
    }

    /**
     * A mapper representation may omit its configuration entirely. Validation must report the mapper's own error, or
     * accept the mapper, rather than fail with a {@code NullPointerException} reported as a 500.
     */
    @Test
    public void shouldAcceptMapperWithoutConfigOnPerMapperEndpoint() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("no-config-mapper-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://one.example.org/callback"));
        String clientId;
        try (Response response = realm.admin().clients().create(rep)) {
            clientId = ApiUtil.getCreatedId(response);
        }

        ProtocolMapperRepresentation mapper = pairwiseMapperWithoutSalt();
        mapper.setConfig(null);

        var client = realm.admin().clients().get(clientId);
        try (Response response = client.getProtocolMappers().createMapper(mapper)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
        assertThat(saltOf(client.getProtocolMappers().getMappers().get(0)), not(emptyOrNullString()));
    }

    @Test
    public void shouldAcceptMapperWithoutConfigOnClientCreate() {
        ProtocolMapperRepresentation mapper = pairwiseMapperWithoutSalt();
        mapper.setConfig(null);

        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("inline-no-config-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://one.example.org/callback"));
        rep.setProtocolMappers(List.of(mapper));

        String clientId;
        try (Response response = realm.admin().clients().create(rep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            clientId = ApiUtil.getCreatedId(response);
        }

        assertThat(saltOf(realm.admin().clients().get(clientId).getProtocolMappers().getMappers().get(0)),
                not(emptyOrNullString()));
    }

    /**
     * A client may carry a mapper of a provider that is not installed, for example after a third-party provider JAR
     * was removed. Such a mapper cannot be validated and must not make the client unwritable.
     */
    @Test
    public void shouldAcceptMapperOfUnknownProvider() {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("unknown-provider-mapper");
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setProtocolMapper("mapper-of-a-provider-that-is-not-installed");
        mapper.setConfig(new HashMap<>());

        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("unknown-mapper-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://one.example.org/callback"));
        rep.setProtocolMappers(List.of(mapper));

        try (Response response = realm.admin().clients().create(rep)) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        }
    }

    @Test
    public void shouldPersistGeneratedSaltOnClientCreateAndUpdate() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("pairwise-client");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setRedirectUris(List.of("https://example.org/callback"));
        rep.setProtocolMappers(List.of(pairwiseMapperWithoutSalt()));
        String clientId;
        try (Response response = realm.admin().clients().create(rep)) {
            clientId = ApiUtil.getCreatedId(response);
        }
        var client = realm.admin().clients().get(clientId);
        String initialSalt = saltOf(client.getProtocolMappers().getMappers().get(0));
        assertThat(initialSalt, not(emptyOrNullString()));

        ClientRepresentation update = client.toRepresentation();
        update.setDescription("Unrelated client update");
        client.update(update);
        assertThat(saltOf(client.getProtocolMappers().getMappers().get(0)), is(initialSalt));

        ProtocolMapperRepresentation mapper = client.getProtocolMappers().getMappers().get(0);
        mapper.getConfig().remove(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT);
        update.setProtocolMappers(List.of(mapper));
        client.update(update);
        assertThat(saltOf(client.getProtocolMappers().getMappers().get(0)), not(emptyOrNullString()));
    }

    @Test
    public void shouldPersistGeneratedSaltOnClientScopeCreate() {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setName("pairwise-scope");
        rep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        rep.setProtocolMappers(List.of(pairwiseMapperWithoutSalt()));
        String scopeId;
        try (Response response = realm.admin().clientScopes().create(rep)) {
            scopeId = ApiUtil.getCreatedId(response);
        }
        var scope = realm.admin().clientScopes().get(scopeId);
        assertThat(saltOf(scope.getProtocolMappers().getMappers().get(0)), not(emptyOrNullString()));
    }

    private static ProtocolMapperRepresentation pairwiseMapperWithoutSalt() {
        ProtocolMapperRepresentation mapper = SHA256PairwiseSubMapper.createPairwiseMapper(null, null);
        mapper.getConfig().remove(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT);
        return mapper;
    }

    private static String saltOf(ProtocolMapperRepresentation mapper) {
        return mapper.getConfig().get(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT);
    }
}
