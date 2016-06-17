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

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserResource {

    @GET
    public UserRepresentation toRepresentation();

    @PUT
    public void update(UserRepresentation userRepresentation);

    @DELETE
    public void remove();

    @Path("groups")
    @GET
    List<GroupRepresentation> groups();

    @Path("groups/{groupId}")
    @PUT
    void joinGroup(@PathParam("groupId") String groupId);

    @Path("groups/{groupId}")
    @DELETE
    void leaveGroup(@PathParam("groupId") String groupId);




    @POST
    @Path("logout")
    public void logout();

    @PUT
    @Path("remove-totp")
    public void removeTotp();

    @PUT
    @Path("reset-password")
    public void resetPassword(CredentialRepresentation credentialRepresentation);

    /**
     * Use executeActionsEmail and pass in the UPDATE_PASSWORD required action
     *
     */
    @PUT
    @Path("reset-password-email")
    @Deprecated
    public void resetPasswordEmail();

    /**
     * Use executeActionsEmail and pass in the UPDATE_PASSWORD required action
     *
     */
    @PUT
    @Path("reset-password-email")
    @Deprecated
    public void resetPasswordEmail(@QueryParam("client_id") String clientId);

    @PUT
    @Path("execute-actions-email")
    public void executeActionsEmail(List<String> actions);

    @PUT
    @Path("execute-actions-email")
    public void executeActionsEmail(@QueryParam("client_id") String clientId, List<String> actions);

    @PUT
    @Path("send-verify-email")
    public void sendVerifyEmail();

    @PUT
    @Path("send-verify-email")
    public void sendVerifyEmail(@QueryParam("client_id") String clientId);

    @GET
    @Path("sessions")
    public List<UserSessionRepresentation> getUserSessions();

    @GET
    @Path("offline-sessions/{clientId}")
    List<UserSessionRepresentation> getOfflineSessions(@PathParam("clientId") String clientId);

    @GET
    @Path("federated-identity")
    public List<FederatedIdentityRepresentation> getFederatedIdentity();

    @POST
    @Path("federated-identity/{provider}")
    public Response addFederatedIdentity(@PathParam("provider") String provider, FederatedIdentityRepresentation rep);

    @Path("federated-identity/{provider}")
    @DELETE
    public void removeFederatedIdentity(final @PathParam("provider") String provider);

    @Path("role-mappings")
    public RoleMappingResource roles();


    @GET
    @Path("consents")
    public List<Map<String, Object>> getConsents();

    @DELETE
    @Path("consents/{client}")
    public void revokeConsent(@PathParam("client") String clientId);

    @POST
    @Path("impersonation")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> impersonate();
}
