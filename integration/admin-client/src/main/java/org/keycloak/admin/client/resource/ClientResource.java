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

package org.keycloak.admin.client.resource;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.ManagementPermissionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
public interface ClientResource {

    /**
     * Enables or disables the fine grain permissions feature.
     * Returns the updated status of the server in the
     * {@link ManagementPermissionReference}.
     *
     * @param status status request to apply
     * @return permission reference indicating the updated status
     */
    @PUT
    @Path("/management/permissions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    ManagementPermissionReference setPermissions(ManagementPermissionRepresentation status);

    /**
     * Returns indicator if the fine grain permissions are enabled or not.
     *
     * @return current representation of the permissions feature
     */
    @GET
    @Path("/management/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    ManagementPermissionReference getPermissions();

    @Path("protocol-mappers")
    ProtocolMappersResource getProtocolMappers();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClientRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(ClientRepresentation clientRepresentation);

    @DELETE
    void remove();

    @POST
    @Path("client-secret")
    @Produces(MediaType.APPLICATION_JSON)
    CredentialRepresentation generateNewSecret();

    @GET
    @Path("client-secret")
    @Produces(MediaType.APPLICATION_JSON)
    CredentialRepresentation getSecret();

    /**
     * Generate a new registration access token for the client
     *
     * @return
     */
    @Path("registration-access-token")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    ClientRepresentation regenerateRegistrationAccessToken();

    /**
     * Get representation of certificate resource
     *
     * @param attributePrefix
     * @return
     */
    @Path("certificates/{attr}")
    ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix);

    @GET
    @NoCache
    @Path("installation/providers/{providerId}")
    String getInstallationProvider(@PathParam("providerId") String providerId);

    @Path("session-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Integer> getApplicationSessionCount();

    @Path("user-sessions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserSessionRepresentation> getUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    @Path("offline-session-count")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Long> getOfflineSessionCount();

    @Path("offline-sessions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<UserSessionRepresentation> getOfflineUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    @POST
    @Path("push-revocation")
    @Produces(MediaType.APPLICATION_JSON)
    void pushRevocation();

    @Path("/scope-mappings")
    RoleMappingResource getScopeMappings();

    @Path("/roles")
    RolesResource roles();

    /**
     * Get default client scopes.  Only name and ids are returned.
     *
     * @return default client scopes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-client-scopes")
    List<ClientScopeRepresentation> getDefaultClientScopes();

    @PUT
    @Path("default-client-scopes/{clientScopeId}")
    void addDefaultClientScope(@PathParam("clientScopeId") String clientScopeId);

    @DELETE
    @Path("default-client-scopes/{clientScopeId}")
    void removeDefaultClientScope(@PathParam("clientScopeId") String clientScopeId);

    /**
     * Get optional client scopes.  Only name and ids are returned.
     *
     * @return optional client scopes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("optional-client-scopes")
    List<ClientScopeRepresentation> getOptionalClientScopes();

    @PUT
    @Path("optional-client-scopes/{clientScopeId}")
    void addOptionalClientScope(@PathParam("clientScopeId") String clientScopeId);

    @DELETE
    @Path("optional-client-scopes/{clientScopeId}")
    void removeOptionalClientScope(@PathParam("clientScopeId") String clientScopeId);

    @Path("/service-account-user")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    UserRepresentation getServiceAccountUser();

    @Path("nodes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void registerNode(Map<String, String> formParams);

    @Path("nodes/{node}")
    @DELETE
    void unregisterNode(final @PathParam("node") String node);

    @Path("test-nodes-available")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    GlobalRequestResult testNodesAvailable();

    @Path("/authz/resource-server")
    AuthorizationResource authorization();
}