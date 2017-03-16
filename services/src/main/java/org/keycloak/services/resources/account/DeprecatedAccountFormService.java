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
package org.keycloak.services.resources.account;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.account.AccountPages;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AbstractSecuredLocalService;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DeprecatedAccountFormService extends AbstractSecuredLocalService {

    private static final Logger logger = Logger.getLogger(DeprecatedAccountFormService.class);

    private static Set<String> VALID_PATHS = new HashSet<String>();
    static {
        for (Method m : DeprecatedAccountFormService.class.getMethods()) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
                VALID_PATHS.add(p.value());
            }
        }
    }

    // Used when some other context (ie. IdentityBrokerService) wants to forward error to account management and display it here
    public static final String ACCOUNT_MGMT_FORWARDED_ERROR_NOTE = "ACCOUNT_MGMT_FORWARDED_ERROR";

    private final AppAuthManager authManager;
    private EventBuilder event;
    private AccountProvider account;
    private EventStoreProvider eventStore;

    public DeprecatedAccountFormService(RealmModel realm, ClientModel client, EventBuilder event) {
        super(realm, client);
        this.event = event;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);

        account = session.getProvider(AccountProvider.class).setRealm(realm).setUriInfo(uriInfo).setHttpHeaders(headers);

        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm);
        if (authResult != null) {
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), true);
            updateCsrfChecks();
            account.setStateChecker(stateChecker);
        }

        String requestOrigin = UriUtils.getOrigin(uriInfo.getBaseUri());

        String origin = headers.getRequestHeaders().getFirst("Origin");
        if (origin != null && !requestOrigin.equals(origin)) {
            throw new ForbiddenException();
        }

        if (!request.getHttpMethod().equals("GET")) {
            String referrer = headers.getRequestHeaders().getFirst("Referer");
            if (referrer != null && !requestOrigin.equals(UriUtils.getOrigin(referrer))) {
                throw new ForbiddenException();
            }
        }

        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            if (userSession != null) {
                boolean associated = false;
                for (ClientSessionModel c : userSession.getClientSessions()) {
                    if (c.getClient().equals(client)) {
                        auth.setClientSession(c);
                        associated = true;
                        break;
                    }
                }
                if (!associated) {
                    ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
                    clientSession.setUserSession(userSession);
                    auth.setClientSession(clientSession);
                }
            }

            account.setUser(auth.getUser());
        }

        account.setFeatures(realm.isIdentityFederationEnabled(), eventStore != null && realm.isEventsEnabled(), true);
    }

    public static UriBuilder accountServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
        return base;
    }

    public static UriBuilder accountServiceApplicationPage(UriInfo uriInfo) {
        return accountServiceBaseUrl(uriInfo).path(DeprecatedAccountFormService.class, "applicationsPage");
    }

    protected Set<String> getValidPaths() {
        return DeprecatedAccountFormService.VALID_PATHS;
    }

    private Response forwardToPage(String path, AccountPages page) {
        if (auth != null) {
            try {
                auth.require(AccountRoles.MANAGE_ACCOUNT);
            } catch (ForbiddenException e) {
                return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage();
            }

            setReferrerOnPage();

            String forwardedError = auth.getClientSession().getNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
            if (forwardedError != null) {
                try {
                    FormMessage errorMessage = JsonSerialization.readValue(forwardedError, FormMessage.class);
                    account.setError(errorMessage.getMessage(), errorMessage.getParameters());
                    auth.getClientSession().removeNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            return account.createResponse(page);
        } else {
            return login(path);
        }
    }

    private void setReferrerOnPage() {
        String[] referrer = getReferrer();
        if (referrer != null) {
            account.setReferrer(referrer);
        }
    }

    /**
     * Get account information.
     *
     * @return
     */
    @Path("/")
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response accountPage() {
        return forwardToPage(null, AccountPages.ACCOUNT);
    }

    public static UriBuilder totpUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(DeprecatedAccountFormService.class, "totpPage");
    }
    @Path("totp")
    @GET
    public Response totpPage() {
        return forwardToPage("totp", AccountPages.TOTP);
    }

    public static UriBuilder passwordUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(DeprecatedAccountFormService.class, "passwordPage");
    }
    @Path("password")
    @GET
    public Response passwordPage() {
        if (auth != null) {
            account.setPasswordSet(isPasswordSet(session, realm, auth.getUser()));
        }

        return forwardToPage("password", AccountPages.PASSWORD);
    }

    @Path("identity")
    @GET
    public Response federatedIdentityPage() {
        return forwardToPage("identity", AccountPages.FEDERATED_IDENTITY);
    }

    @Path("log")
    @GET
    public Response logPage() {
        if (auth != null) {
            List<Event> events = eventStore.createQuery().type(Constants.EXPOSED_LOG_EVENTS).user(auth.getUser().getId()).maxResults(30).getResultList();
            for (Event e : events) {
                if (e.getDetails() != null) {
                    Iterator<Map.Entry<String, String>> itr = e.getDetails().entrySet().iterator();
                    while (itr.hasNext()) {
                        if (!Constants.EXPOSED_LOG_DETAILS.contains(itr.next().getKey())) {
                            itr.remove();
                        }
                    }
                }
            }
            account.setEvents(events);
        }
        return forwardToPage("log", AccountPages.LOG);
    }

    @Path("sessions")
    @GET
    public Response sessionsPage() {
        if (auth != null) {
            account.setSessions(session.sessions().getUserSessions(realm, auth.getUser()));
        }
        return forwardToPage("sessions", AccountPages.SESSIONS);
    }

    @Path("applications")
    @GET
    public Response applicationsPage() {
        return forwardToPage("applications", AccountPages.APPLICATIONS);
    }

    /**
     * Update account information.
     *
     * Form params:
     *
     * firstName
     * lastName
     * email
     *
     * @param formData
     * @return
     */
    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processAccountUpdate(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login(null);
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.ACCOUNT);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        event.event(EventType.UPDATE_PROFILE).client(auth.getClient()).user(auth.getUser());

        List<FormMessage> errors = Validation.validateUpdateProfileForm(realm.isEditUsernameAllowed(), formData);
        if (errors != null && !errors.isEmpty()) {
            setReferrerOnPage();
            return account.setErrors(errors).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }

        try {
            AccountUtils.updateUsername(formData.getFirst("username"), user, session);
            AccountUtils.updateEmail(formData.getFirst("email"), user, session, event);

            user.setFirstName(formData.getFirst("firstName"));
            user.setLastName(formData.getFirst("lastName"));

            AttributeFormDataProcessor.process(formData, realm, user);

            event.success();

            setReferrerOnPage();
            return account.setSuccess(Messages.ACCOUNT_UPDATED).createResponse(AccountPages.ACCOUNT);
        } catch (ReadOnlyException roe) {
            setReferrerOnPage();
            return account.setError(Messages.READ_ONLY_USER).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        } catch (ModelDuplicateException mde) {
            setReferrerOnPage();
            return account.setError(mde.getMessage()).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }
    }

    @Path("totp-remove")
    @GET
    public Response processTotpRemove(@QueryParam("stateChecker") String stateChecker) {
        if (auth == null) {
            return login("totp");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);

        csrfCheck(stateChecker);

        UserModel user = auth.getUser();
        session.userCredentialManager().disableCredentialType(realm, user, CredentialModel.OTP);

        event.event(EventType.REMOVE_TOTP).client(auth.getClient()).user(auth.getUser()).success();

        setReferrerOnPage();
        return account.setSuccess(Messages.SUCCESS_TOTP_REMOVED).createResponse(AccountPages.TOTP);
    }


    @Path("sessions-logout")
    @GET
    public Response processSessionsLogout(@QueryParam("stateChecker") String stateChecker) {
        if (auth == null) {
            return login("sessions");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(stateChecker);

        UserModel user = auth.getUser();
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, clientConnection, headers, true);
        }

        UriBuilder builder = Urls.accountBase(uriInfo.getBaseUri()).path(DeprecatedAccountFormService.class, "sessionsPage");
        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            builder.queryParam("referrer", referrer);

        }
        URI location = builder.build(realm.getName());
        return Response.seeOther(location).build();
    }

    @Path("revoke-grant")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processRevokeGrant(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("applications");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(formData);

        String clientId = formData.getFirst("clientId");
        if (clientId == null) {
            return account.setError(Messages.CLIENT_NOT_FOUND).createResponse(AccountPages.APPLICATIONS);
        }
        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            return account.setError(Messages.CLIENT_NOT_FOUND).createResponse(AccountPages.APPLICATIONS);
        }

        // Revoke grant in UserModel
        UserModel user = auth.getUser();
        session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        new UserSessionManager(session).revokeOfflineToken(user, client);

        // Logout clientSessions for this user and client
        AuthenticationManager.backchannelUserFromClient(session, realm, user, client, uriInfo, headers);

        event.event(EventType.REVOKE_GRANT).client(auth.getClient()).user(auth.getUser()).detail(Details.REVOKED_CLIENT, client.getClientId()).success();
        setReferrerOnPage();

        UriBuilder builder = Urls.accountBase(uriInfo.getBaseUri()).path(DeprecatedAccountFormService.class, "applicationsPage");
        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            builder.queryParam("referrer", referrer);

        }
        URI location = builder.build(realm.getName());
        return Response.seeOther(location).build();
    }

    /**
     * Update the TOTP for this account.
     *
     * form parameters:
     *
     * totp - otp generated by authenticator
     * totpSecret - totp secret to register
     *
     * @param formData
     * @return
     */
    @Path("totp")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processTotpUpdate(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("totp");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.TOTP);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        if (Validation.isBlank(totp)) {
            setReferrerOnPage();
            return account.setError(Messages.MISSING_TOTP).createResponse(AccountPages.TOTP);
        } else if (!CredentialValidation.validOTP(realm, totp, totpSecret)) {
            setReferrerOnPage();
            return account.setError(Messages.INVALID_TOTP).createResponse(AccountPages.TOTP);
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(realm.getOTPPolicy().getType());
        credentials.setValue(totpSecret);
        session.userCredentialManager().updateCredential(realm, user, credentials);

        // to update counter
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(realm.getOTPPolicy().getType());
        cred.setValue(totp);
        session.userCredentialManager().isValid(realm, user, cred);

        event.event(EventType.UPDATE_TOTP).client(auth.getClient()).user(auth.getUser()).success();

        setReferrerOnPage();
        return account.setSuccess(Messages.SUCCESS_TOTP).createResponse(AccountPages.TOTP);
    }

    /**
     * Update account password
     *
     * Form params:
     *
     * password - old password
     * password-new
     * pasword-confirm
     *
     * @param formData
     * @return
     */
    @Path("password")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processPasswordUpdate(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("password");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);

        csrfCheck(formData);
        UserModel user = auth.getUser();

        boolean requireCurrent = isPasswordSet(session, realm, user);
        account.setPasswordSet(requireCurrent);

        String password = formData.getFirst("password");
        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_PASSWORD_ERROR)
                .client(auth.getClient())
                .user(auth.getClientSession().getUserSession().getUser());

        if (requireCurrent) {
            if (Validation.isBlank(password)) {
                setReferrerOnPage();
                errorEvent.error(Errors.PASSWORD_MISSING);
                return account.setError(Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
            }

            UserCredentialModel cred = UserCredentialModel.password(password);
            if (!session.userCredentialManager().isValid(realm, user, cred)) {
                setReferrerOnPage();
                errorEvent.error(Errors.INVALID_USER_CREDENTIALS);
                return account.setError(Messages.INVALID_PASSWORD_EXISTING).createResponse(AccountPages.PASSWORD);
            }
        }

        if (Validation.isBlank(passwordNew)) {
            setReferrerOnPage();
            errorEvent.error(Errors.PASSWORD_MISSING);
            return account.setError(Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
        }

        if (!passwordNew.equals(passwordConfirm)) {
            setReferrerOnPage();
            errorEvent.error(Errors.PASSWORD_CONFIRM_ERROR);
            return account.setError(Messages.INVALID_PASSWORD_CONFIRM).createResponse(AccountPages.PASSWORD);
        }

        try {
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(passwordNew, false));
        } catch (ReadOnlyException mre) {
            setReferrerOnPage();
            errorEvent.error(Errors.NOT_ALLOWED);
            return account.setError(Messages.READ_ONLY_PASSWORD).createResponse(AccountPages.PASSWORD);
        } catch (ModelException me) {
            ServicesLogger.LOGGER.failedToUpdatePassword(me);
            setReferrerOnPage();
            errorEvent.detail(Details.REASON, me.getMessage()).error(Errors.PASSWORD_REJECTED);
            return account.setError(me.getMessage(), me.getParameters()).createResponse(AccountPages.PASSWORD);
        } catch (Exception ape) {
            ServicesLogger.LOGGER.failedToUpdatePassword(ape);
            setReferrerOnPage();
            errorEvent.detail(Details.REASON, ape.getMessage()).error(Errors.PASSWORD_REJECTED);
            return account.setError(ape.getMessage()).createResponse(AccountPages.PASSWORD);
        }

        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel s : sessions) {
            if (!s.getId().equals(auth.getSession().getId())) {
                AuthenticationManager.backchannelLogout(session, realm, s, uriInfo, clientConnection, headers, true);
            }
        }

        event.event(EventType.UPDATE_PASSWORD).client(auth.getClient()).user(auth.getUser()).success();

        setReferrerOnPage();
        return account.setPasswordSet(true).setSuccess(Messages.ACCOUNT_PASSWORD_UPDATED).createResponse(AccountPages.PASSWORD);
    }

    @Path("federated-identity-update")
    @GET
    public Response processFederatedIdentityUpdate(@QueryParam("action") String action,
                                                   @QueryParam("provider_id") String providerId,
                                                   @QueryParam("stateChecker") String stateChecker) {
        if (auth == null) {
            return login("identity");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(stateChecker);
        UserModel user = auth.getUser();

        if (Validation.isEmpty(providerId)) {
            setReferrerOnPage();
            return account.setError(Messages.MISSING_IDENTITY_PROVIDER).createResponse(AccountPages.FEDERATED_IDENTITY);
        }
        AccountSocialAction accountSocialAction = AccountSocialAction.getAction(action);
        if (accountSocialAction == null) {
            setReferrerOnPage();
            return account.setError(Messages.INVALID_FEDERATED_IDENTITY_ACTION).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        boolean hasProvider = false;

        for (IdentityProviderModel model : realm.getIdentityProviders()) {
            if (model.getAlias().equals(providerId)) {
                hasProvider = true;
            }
        }

        if (!hasProvider) {
            setReferrerOnPage();
            return account.setError(Messages.IDENTITY_PROVIDER_NOT_FOUND).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        if (!user.isEnabled()) {
            setReferrerOnPage();
            return account.setError(Messages.ACCOUNT_DISABLED).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        switch (accountSocialAction) {
            case ADD:
                String redirectUri = UriBuilder.fromUri(Urls.accountFederatedIdentityPage(uriInfo.getBaseUri(), realm.getName())).build().toString();

                try {
                    String nonce = UUID.randomUUID().toString();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    String input = nonce + auth.getSession().getId() +  auth.getClientSession().getId() + providerId;
                    byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
                    String hash = Base64Url.encode(check);
                    URI linkUrl = Urls.identityProviderLinkRequest(this.uriInfo.getBaseUri(), providerId, realm.getName());
                    linkUrl = UriBuilder.fromUri(linkUrl)
                            .queryParam("nonce", nonce)
                            .queryParam("hash", hash)
                            .queryParam("client_id", client.getClientId())
                            .queryParam("redirect_uri", redirectUri)
                            .build();
                    return Response.seeOther(linkUrl)
                            .build();
                } catch (Exception spe) {
                    setReferrerOnPage();
                    return account.setError(Messages.IDENTITY_PROVIDER_REDIRECT_ERROR).createResponse(AccountPages.FEDERATED_IDENTITY);
                }
            case REMOVE:
                FederatedIdentityModel link = session.users().getFederatedIdentity(user, providerId, realm);
                if (link != null) {

                    // Removing last social provider is not possible if you don't have other possibility to authenticate
                    if (session.users().getFederatedIdentities(user, realm).size() > 1 || user.getFederationLink() != null || isPasswordSet(session, realm, user)) {
                        session.users().removeFederatedIdentity(realm, user, providerId);

                        logger.debugv("Social provider {0} removed successfully from user {1}", providerId, user.getUsername());

                        event.event(EventType.REMOVE_FEDERATED_IDENTITY).client(auth.getClient()).user(auth.getUser())
                                .detail(Details.USERNAME, auth.getUser().getUsername())
                                .detail(Details.IDENTITY_PROVIDER, link.getIdentityProvider())
                                .detail(Details.IDENTITY_PROVIDER_USERNAME, link.getUserName())
                                .success();

                        setReferrerOnPage();
                        return account.setSuccess(Messages.IDENTITY_PROVIDER_REMOVED).createResponse(AccountPages.FEDERATED_IDENTITY);
                    } else {
                        setReferrerOnPage();
                        return account.setError(Messages.FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER).createResponse(AccountPages.FEDERATED_IDENTITY);
                    }
                } else {
                    setReferrerOnPage();
                    return account.setError(Messages.FEDERATED_IDENTITY_NOT_ACTIVE).createResponse(AccountPages.FEDERATED_IDENTITY);
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    public static UriBuilder loginRedirectUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(DeprecatedAccountFormService.class, "loginRedirect");
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName());
    }

    public static boolean isPasswordSet(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, CredentialModel.PASSWORD);
    }

    private String[] getReferrer() {
        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = uriInfo.getQueryParameters().getFirst("referrer_uri");

        ClientModel referrerClient = realm.getClientByClientId(referrer);
        if (referrerClient != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, referrerClient);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(uriInfo.getRequestUri(), client.getRootUrl(), referrerClient.getBaseUrl());
            }

            if (referrerUri != null) {
                String referrerName = referrerClient.getName();
                if (Validation.isBlank(referrerName)) {
                    referrerName = referrer;
                }
                return new String[]{referrerName, referrerUri};
            }
        } else if (referrerUri != null) {
            referrerClient = realm.getClientByClientId(referrer);
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, referrerClient);

                if (referrerUri != null) {
                    return new String[]{referrer, referrerUri};
                }
            }
        }

        return null;
    }

    private enum AccountSocialAction {
        ADD,
        REMOVE;

        public static AccountSocialAction getAction(String action) {
            if ("add".equalsIgnoreCase(action)) {
                return ADD;
            } else if ("remove".equalsIgnoreCase(action)) {
                return REMOVE;
            } else {
                return null;
            }
        }
    }
}
