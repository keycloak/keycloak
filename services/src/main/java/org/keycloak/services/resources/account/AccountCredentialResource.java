package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.authentication.CredentialRegistrator;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.PasswordCredentialProviderFactory;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.messages.Messages;
import org.keycloak.utils.MediaType;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AccountCredentialResource {

    private final KeycloakSession session;
    private final EventBuilder event;
    private final UserModel user;
    private final RealmModel realm;
    private Auth auth;

    public AccountCredentialResource(KeycloakSession session, EventBuilder event, UserModel user, Auth auth) {
        this.session = session;
        this.event = event;
        this.user = user;
        this.auth = auth;
        realm = session.getContext().getRealm();
    }

    // TODO: This is kept here for now and commented. The endpoints will be added by team cheetah during work on account console.
    // This is here just to show what logic will need to be called in the new endpoints. We may need to remove it and/or change it
//    @GET
//    @NoCache
//    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
//    public List<CredentialRepresentation> credentials(){
//        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
//        List<CredentialModel> models = session.userCredentialManager().getStoredCredentials(realm, user);
//        models.forEach(c -> c.setSecretData(null));
//        return models.stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
//    }
//
//
//    @GET
//    @Path("registrators")
//    @NoCache
//    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
//    public List<String> getCredentialRegistrators(){
//        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
//
//        return session.getContext().getRealm().getRequiredActionProviders().stream()
//                .map(RequiredActionProviderModel::getProviderId)
//                .filter(providerId ->  session.getProvider(RequiredActionProvider.class, providerId) instanceof CredentialRegistrator)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Remove a credential for a user
//     *
//     */
//    @Path("{credentialId}")
//    @DELETE
//    @NoCache
//    public void removeCredential(final @PathParam("credentialId") String credentialId) {
//        auth.require(AccountRoles.MANAGE_ACCOUNT);
//        session.userCredentialManager().removeStoredCredential(realm, user, credentialId);
//    }
//
//    /**
//     * Update a credential label for a user
//     */
//    @PUT
//    @Consumes(javax.ws.rs.core.MediaType.TEXT_PLAIN)
//    @Path("{credentialId}/label")
//    public void setLabel(final @PathParam("credentialId") String credentialId, String userLabel) {
//        auth.require(AccountRoles.MANAGE_ACCOUNT);
//        session.userCredentialManager().updateCredentialLabel(realm, user, credentialId, userLabel);
//    }
//
//    /**
//     * Move a credential to a position behind another credential
//     * @param credentialId The credential to move
//     */
//    @Path("{credentialId}/moveToFirst")
//    @POST
//    public void moveToFirst(final @PathParam("credentialId") String credentialId){
//        moveCredentialAfter(credentialId, null);
//    }
//
//    /**
//     * Move a credential to a position behind another credential
//     * @param credentialId The credential to move
//     * @param newPreviousCredentialId The credential that will be the previous element in the list. If set to null, the moved credential will be the first element in the list.
//     */
//    @Path("{credentialId}/moveAfter/{newPreviousCredentialId}")
//    @POST
//    public void moveCredentialAfter(final @PathParam("credentialId") String credentialId, final @PathParam("newPreviousCredentialId") String newPreviousCredentialId){
//        auth.require(AccountRoles.MANAGE_ACCOUNT);
//        session.userCredentialManager().moveCredentialTo(realm, user, credentialId, newPreviousCredentialId);
//    }

    @GET
    @Path("password")
    @Produces(MediaType.APPLICATION_JSON)
    public PasswordDetails passwordDetails() throws IOException {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);
        
        PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session.getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
        CredentialModel password = passwordProvider.getPassword(realm, user);

        PasswordDetails details = new PasswordDetails();
        if (password != null) {
            details.setRegistered(true);
            details.setLastUpdate(password.getCreatedDate());
        } else {
            details.setRegistered(false);
        }

        return details;
    }

    @POST
    @Path("password")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response passwordUpdate(PasswordUpdate update) {
        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        event.event(EventType.UPDATE_PASSWORD);

        UserCredentialModel cred = UserCredentialModel.password(update.getCurrentPassword());
        if (!session.userCredentialManager().isValid(realm, user, cred)) {
            event.error(org.keycloak.events.Errors.INVALID_USER_CREDENTIALS);
            return ErrorResponse.error(Messages.INVALID_PASSWORD_EXISTING, Response.Status.BAD_REQUEST);
        }
        
        if (update.getNewPassword() == null) {
            return ErrorResponse.error(Messages.INVALID_PASSWORD_EXISTING, Response.Status.BAD_REQUEST);
        }
        
        String confirmation = update.getConfirmation();
        if ((confirmation != null) && !update.getNewPassword().equals(confirmation)) {
            return ErrorResponse.error(Messages.NOTMATCH_PASSWORD, Response.Status.BAD_REQUEST);
        }

        try {
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(update.getNewPassword(), false));
        } catch (ModelException e) {
            return ErrorResponse.error(e.getMessage(), e.getParameters(), Response.Status.BAD_REQUEST);
        }

        return Response.ok().build();
    }

    public static class PasswordDetails {

        private boolean registered;
        private long lastUpdate;

        public boolean isRegistered() {
            return registered;
        }

        public void setRegistered(boolean registered) {
            this.registered = registered;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }

    }

    public static class PasswordUpdate {

        private String currentPassword;
        private String newPassword;
        private String confirmation;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
        
        public String getConfirmation() {
            return confirmation;
        }

        public void setConfirmation(String confirmation) {
            this.confirmation = confirmation;
        }

    }

}
