/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.resources.account;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.util.ResolveRelative;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

public class AccountIssuedVerifiableCredentialResource {

    private static final Logger logger = Logger.getLogger(AccountIssuedVerifiableCredentialResource.class);

    private final KeycloakSession session;
    private final Auth auth;
    private final UserModel user;
    private final RealmModel realm;

    public AccountIssuedVerifiableCredentialResource(KeycloakSession session, Auth auth, UserModel user) {
        this.session = session;
        this.auth = auth;
        this.user = user;
        this.realm = auth.getRealm();
    }

    /**
     * Get list of issued credentials for the authenticated user
     *
     * @return list of user's issued credentials
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getIssuedCredentials() {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_VERIFIABLE_CREDENTIALS, AccountRoles.MANAGE_VERIFIABLE_CREDENTIALS);
        checkOid4VCIEnabled();

        List<IssuedVerifiableCredentialRepresentation> credentials = session.users()
                .getIssuedVerifiableCredentialsStreamByUser(user.getId())
                .map(model -> ModelToRepresentation.toRepresentation(model, session, realm))
                .map(rep -> enrichWithClientInfo(rep, session, realm))
                .toList();

        return Cors.builder()
                .auth()
                .checkAllowedOrigins(auth.getToken())
                .add(Response.ok(credentials));
    }

    /**
     * Revoke a specific issued credential for the authenticated user
     *
     * @param credentialId the issued credential ID to revoke
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{credentialId}")
    @NoCache
    public Response revokeIssuedCredential(@PathParam("credentialId") String credentialId) {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_VERIFIABLE_CREDENTIALS);
        checkOid4VCIEnabled();

        boolean removed = session.users().removeIssuedVerifiableCredential(credentialId);
        if (!removed) {
            logger.warn(String.format("Issued credential with ID '%s' not found for user '%s' in the realm '%s'.", credentialId, user.getUsername(), realm.getName()));
            throw new NotFoundException("Issued credential not found");
        }
        return Cors.builder().auth().checkAllowedOrigins(auth.getToken()).add(Response.noContent());
    }

    private void checkOid4VCIEnabled() {
        if (!Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI)) {
            throw ErrorResponse.error("Feature " + Profile.Feature.OID4VC_VCI.getKey() + " not enabled", Response.Status.BAD_REQUEST);
        }
        if (!realm.isVerifiableCredentialsEnabled()) {
            throw ErrorResponse.error("Verifiable credentials not enabled for the realm", Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Enriches the issued credential representation with client name and base URL
     */
    private IssuedVerifiableCredentialRepresentation enrichWithClientInfo(IssuedVerifiableCredentialRepresentation rep, KeycloakSession session, RealmModel realm) {
        if (rep.getClientId() == null) {
            return rep;
        }

        ClientModel client = realm.getClientById(rep.getClientId());
        if (client == null) {
            return rep;
        }

        String clientName = client.getName();
        if (clientName == null || clientName.isEmpty()) {
            clientName = client.getClientId();
        }
        rep.setClientName(clientName);

        String effectiveUrl = ResolveRelative.resolveRelativeUri(session,client.getRootUrl(),client.getBaseUrl());
        rep.setClientBaseUrl(effectiveUrl);

        return rep;
    }

}
