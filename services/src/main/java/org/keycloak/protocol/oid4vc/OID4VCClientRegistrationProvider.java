/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProvider.VC_KEY;
import org.keycloak.protocol.oid4vc.model.OID4VCClient;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientregistration.AbstractClientRegistrationProvider;
import org.keycloak.services.clientregistration.DefaultClientRegistrationContext;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Provides the client-registration functionality for OID4VC-clients.
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCClientRegistrationProvider extends AbstractClientRegistrationProvider {

    private static final Logger LOGGER = Logger.getLogger(OID4VCClientRegistrationProvider.class);

    public OID4VCClientRegistrationProvider(KeycloakSession session) {
        super(session);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOID4VCClient(OID4VCClient client) {
        ClientRepresentation clientRepresentation = toClientRepresentation(client);
        validate(clientRepresentation);

        ClientRepresentation cr = create(
                new DefaultClientRegistrationContext(session, clientRepresentation, this));
        URI uri = session.getContext().getUri().getAbsolutePathBuilder().path(cr.getClientId()).build();
        return Response.created(uri).entity(cr).build();
    }

    @PUT
    @Path("{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOID4VCClient(@PathParam("clientId") String clientDid, OID4VCClient client) {
        client.setClientDid(clientDid);
        ClientRepresentation clientRepresentation = toClientRepresentation(client);
        validate(clientRepresentation);
        clientRepresentation = update(clientDid,
                new DefaultClientRegistrationContext(session, clientRepresentation, this));
        return Response.ok(clientRepresentation).build();
    }

    @DELETE
    @Path("{clientId}")
    public Response deleteOID4VCClient(@PathParam("clientId") String clientDid) {
        delete(clientDid);
        return Response.noContent().build();
    }

    /**
     * Validates the clientRepresentation to fulfill the requirement of an OID4VC client
     */
    public static void validate(ClientRepresentation client) {
        String did = client.getClientId();
        if (did == null) {
            throw new ErrorResponseException("no_did", "A client did needs to be configured for OID4VC clients",
                    Response.Status.BAD_REQUEST);
        }
        if (!did.startsWith("did:")) {
            throw new ErrorResponseException("invalid_did", "The client id is not a did.",
                    Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Translate an incoming {@link OID4VCClient} into a keycloak native {@link ClientRepresentation}.
     *
     * @param oid4VCClient pojo, containing the oid4vc client parameters
     * @return a clientRepresentation
     */
    protected static ClientRepresentation toClientRepresentation(OID4VCClient oid4VCClient) {
        ClientRepresentation clientRepresentation = new ClientRepresentation();
        clientRepresentation.setProtocol(OID4VCLoginProtocolFactory.PROTOCOL_ID);

        clientRepresentation.setId(Optional.ofNullable(oid4VCClient.getId()).orElse(UUID.randomUUID().toString()));
        clientRepresentation.setClientId(oid4VCClient.getClientDid());
        // only add non-null parameters
        Optional.ofNullable(oid4VCClient.getDescription()).ifPresent(clientRepresentation::setDescription);
        Optional.ofNullable(oid4VCClient.getName()).ifPresent(clientRepresentation::setName);


        Map<String, String> clientAttributes = oid4VCClient.getSupportedVCTypes()
                .stream()
                .map(SupportedCredentialConfiguration::toDotNotation)
                .flatMap(dotNotated -> dotNotated.entrySet().stream())
                .collect(Collectors.toMap(entry -> VC_KEY + "." + entry.getKey(), Map.Entry::getValue, (e1, e2) -> e1));

        if (!clientAttributes.isEmpty()) {
            clientRepresentation.setAttributes(clientAttributes);
        }


        LOGGER.debugf("Generated client representation {}.", clientRepresentation);
        return clientRepresentation;
    }

    public static OID4VCClient fromClientAttributes(String clientId, Map<String, String> clientAttributes) {

        OID4VCClient oid4VCClient = new OID4VCClient()
                .setClientDid(clientId);

        Set<String> supportedCredentialIds = new HashSet<>();
        Map<String, String> attributes = new HashMap<>();
        clientAttributes
                .entrySet()
                .forEach(entry -> {
                    if (!entry.getKey().startsWith(VC_KEY)) {
                        return;
                    }
                    String key = entry.getKey().substring((VC_KEY + ".").length());
                    supportedCredentialIds.add(key.split("\\.")[0]);
                    attributes.put(key, entry.getValue());
                });


        List<SupportedCredentialConfiguration> supportedCredentialConfigurations = supportedCredentialIds
                .stream()
                .map(id -> SupportedCredentialConfiguration.fromDotNotation(id, attributes))
                .toList();

        return oid4VCClient.setSupportedVCTypes(supportedCredentialConfigurations);
    }
}
