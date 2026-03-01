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

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;

/**
 * @author rodrigo.sasaki@icarros.com.br
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserResource {

    @GET
    UserRepresentation toRepresentation();

    @GET
    UserRepresentation toRepresentation(@QueryParam("userProfileMetadata") boolean userProfileMetadata);

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
                                     @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);

    @Path("groups")
    @GET
    List<GroupRepresentation> groups(@QueryParam("search") String search,
                                     @QueryParam("first") Integer firstResult,
                                     @QueryParam("max") Integer maxResults,
                                     @QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation);

    @Path("groups/count")
    @GET
    Map<String, Long> groupsCount(@QueryParam("search") String search);

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
    @Consumes(jakarta.ws.rs.core.MediaType.TEXT_PLAIN)
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
     * This is typically supported just for the users backed by user storage providers. See {@link UserRepresentation#getDisableableCredentialTypes()}
     * to see what credential types can be disabled for the particular user
     *
     * @param credentialTypes
     */
    @Path("disable-credential-types")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
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
     * i.e. {@code VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS}, etc.
     *
     * @param actions a {@link List} of string representation of {@link org.keycloak.models.UserModel.RequiredAction}
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(List<String> actions);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. {@code VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS}, etc.
     *
     * The lifespan decides the number of seconds after which the generated token in the email link expires. The default
     * value is 12 hours.
     *
     * @param actions a {@link List} of string representation of {@link org.keycloak.models.UserModel.RequiredAction}
     * @param lifespan
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(List<String> actions, @QueryParam("lifespan") Integer lifespan);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. {@code VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS}, etc.
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
     * @param actions a {@link List} of string representation of {@link org.keycloak.models.UserModel.RequiredAction}
     */
    @PUT
    @Path("execute-actions-email")
    void executeActionsEmail(@QueryParam("client_id") String clientId,
                             @QueryParam("redirect_uri") String redirectUri,
                             @QueryParam("lifespan") Integer lifespan,
                             List<String> actions);

    /**
     * Sends an email to the user with a link within it.  If they click on the link they will be asked to perform some actions
     * i.e. {@code VERIFY_EMAIL, UPDATE_PROFILE, CONFIGURE_TOTP, UPDATE_PASSWORD, TERMS_AND_CONDITIONS}, etc.
     *
     * If redirectUri is not null, then you must specify a client id.  This will set the URI you want the flow to link
     * to after the email link is clicked and actions completed.  If both parameters are null, then no page is linked to
     * at the end of the flow.
     *
     * @param clientId
     * @param redirectUri
     * @param actions a {@link List} of string representation of {@link org.keycloak.models.UserModel.RequiredAction}
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

    @PUT
    @Path("send-verify-email")
    void sendVerifyEmail(@QueryParam("client_id") String clientId, @QueryParam("redirect_uri") String redirectUri);

    @PUT
    @Path("send-verify-email")
    void sendVerifyEmail(@QueryParam("lifespan") Integer lifespan);

    /**
     * Send an email-verification email to the user
     *
     * An email contains a link the user can click to verify their email address.
     * The redirectUri and clientId parameters are optional. The default for the
     * redirect is the account client. The default for the lifespan is 12 hours.
     *
     * @param redirectUri Redirect uri
     * @param clientId Client id
     * @param lifespan Number of seconds after which the generated token expires
     * @return
     */
    @PUT
    @Path("send-verify-email")
    void sendVerifyEmail(@QueryParam("client_id") String clientId, @QueryParam("redirect_uri") String redirectUri, @QueryParam("lifespan") Integer lifespan);

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

    /**
     * @since Keycloak server 24.0.6
     * @return unmanaged attributes of the user
     */
    @GET
    @Path("unmanagedAttributes")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, List<String>> getUnmanagedAttributes();
}
