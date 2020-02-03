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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    UserRepresentation toRepresentation();

    @PUT
    void update(UserRepresentation userRepresentation);

    @DELETE
    void remove();

    @Path("groups")
    @GET
    List<GroupRepresentation> groups();

    @Path("groups")
    @GET
    List<GroupRepresentation> groups(@QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults);

    @Path("groups")
    @GET
    List<GroupRepresentation> groups(@QueryParam("search") String search,
                                     @QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults);
    
    @Path("groups")
    @GET
    List<GroupRepresentation> groups(@QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults,
                                     @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);
    
    @Path("groups")
    @GET
    List<GroupRepresentation> groups(@QueryParam("search") String search,
                                     @QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults,
                                     @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);

    @Path("groups/{groupId}")
    @PUT
    void joinGroup(@PathParam("groupId") String groupId);

    @Path("groups/{groupId}")
    @DELETE
    void leaveGroup(@PathParam("groupId") String groupId);




    @POST
    @Path("logout")
    void logout();



    @GET
    @Path("credentials")
    @Produces(MediaType.APPLICATION_JSON)
    List<CredentialRepresentation> credentials();


    /**
     * Return credential types, which are provided by the user storage where user is stored. Returned values can contain for example "password", "otp" etc.
     * This will always return empty list for "local" users, which are not backed by any user storage
     *
     * @return
     */
    @GET
    @Path("configured-user-storage-credential-types")
    @Produces(MediaType.APPLICATION_JSON)
    List<String> getConfiguredUserStorageCredentialTypes();

    /**
     * Remove a credential for a user
     *
     */
    @DELETE
    @Path("credentials/{credentialId}")
    void removeCredential(@PathParam("credentialId")String credentialId);

    /**
     * Update a credential label for a user
     */
    @PUT
    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    @Path("credentials/{credentialId}/userLabel")
    void setCredentialUserLabel(final @PathParam("credentialId") String credentialId, String userLabel);

    /**
     * Move a credential to a first position in the credentials list of the user
     * @param credentialId The credential to move
     */
    @Path("credentials/{credentialId}/moveToFirst")
    @POST
    void moveCredentialToFirst(final @PathParam("credentialId") String credentialId);

    /**
     * Move a credential to a position behind another credential
     * @param credentialId The credential to move
     * @param newPreviousCredentialId The credential that will be the previous element in the list. If set to null, the moved credential will be the first element in the list.
     */
    @Path("credentials/{credentialId}/moveAfter/{newPreviousCredentialId}")
    @POST
    void moveCredentialAfter(final @PathParam("credentialId") String credentialId, final @PathParam("newPreviousCredentialId") String newPreviousCredentialId);


    /**
     * Disables or deletes all credentials for specific types.
     * Type examples "otp", "password"
     *
     * This endpoint is deprecated as it is not supported to disable credentials, just delete them
     *
     * @param credentialTypes
     */
    @Path("disable-credential-types")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Deprecated
    void disableCredentialType(List<String> credentialTypes);

    @PUT
    @Path("reset-password")
    void resetPassword(CredentialRepresentation credentialRepresentation);

    /**
     * Use executeActionsEmail and pass in the UPDATE_PASSWORD required action
     *
     */
    @PUT
    @Path("reset-password-email")
    @Deprecated
    void resetPasswordEmail();

    /**
     * Use executeActionsEmail and pass in the UPDATE_PASSWORD required action
     *
     */
    @PUT
    @Path("reset-password-email")
    @Deprecated
    void resetPasswordEmail(@QueryParam("client_id") String clientId);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. reset password, update profile, etc.
     *
     *
     * @param actions
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(List<String> actions);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. reset password, update profile, etc.
     *
     * The lifespan decides the number of seconds after which the generated token in the email link expires. The default
     * value is 12 hours.
     *
     * @param actions
     * @param lifespan
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(List<String> actions, @QueryParam("lifespan") Integer lifespan);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. reset password, update profile, etc.
     *
     * If redirectUri is not null, then you must specify a client id.  This will set the URI you want the flow to link
     * to after the email link is clicked and actions completed.  If both parameters are null, then no page is linked to
     * at the end of the flow.
     *
     * The lifespan decides the number of seconds after which the generated token in the email link expires. The default
     * value is 12 hours.
     *
     * @param clientId
     * @param redirectUri
     * @param lifespan
     * @param actions
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(@QueryParam("client_id") String clientId,
                             @QueryParam("redirect_uri") String redirectUri,
                             @QueryParam("lifespan") Integer lifespan,
                             List<String> actions);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. reset password, update profile, etc.
     *
     * If redirectUri is not null, then you must specify a client id.  This will set the URI you want the flow to link
     * to after the email link is clicked and actions completed.  If both parameters are null, then no page is linked to
     * at the end of the flow.
     *
     * @param clientId
     * @param redirectUri
     * @param actions
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(@QueryParam("client_id") String clientId, @QueryParam("redirect_uri") String redirectUri, List<String> actions);

    @PUT
    @Path("send-verify-email")
    void sendVerifyEmail();

    @PUT
    @Path("send-verify-email")
    void sendVerifyEmail(@QueryParam("client_id") String clientId);

    @GET
    @Path("sessions")
    List<UserSessionRepresentation> getUserSessions();

    @GET
    @Path("offline-sessions/{clientId}")
    List<UserSessionRepresentation> getOfflineSessions(@PathParam("clientId") String clientId);

    @GET
    @Path("federated-identity")
    List<FederatedIdentityRepresentation> getFederatedIdentity();

    @POST
    @Path("federated-identity/{provider}")
    Response addFederatedIdentity(@PathParam("provider") String provider, FederatedIdentityRepresentation rep);

    @Path("federated-identity/{provider}")
    @DELETE
    void removeFederatedIdentity(final @PathParam("provider") String provider);

    @Path("role-mappings")
    RoleMappingResource roles();


    @GET
    @Path("consents")
    List<Map<String, Object>> getConsents();

    @DELETE
    @Path("consents/{client}")
    void revokeConsent(@PathParam("client") String clientId);

    @POST
    @Path("impersonation")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> impersonate();
}
