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
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.forms.account.AccountPages;
import org.keycloak.forms.account.AccountProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.AbstractSecuredLocalService;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountFormService extends AbstractSecuredLocalService {

    private static final Logger logger = Logger.getLogger(AccountFormService.class);

    private static Set<String> VALID_PATHS = new HashSet<>();

    static {
        for (Method m : AccountFormService.class.getMethods()) {
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

    public AccountFormService(RealmModel realm, ClientModel client, EventBuilder event) {
        super(realm, client);
        this.event = event;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);

        account = session.getProvider(AccountProvider.class).setRealm(realm).setUriInfo(session.getContext().getUri()).setHttpHeaders(headers);

        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm);
        if (authResult != null) {
            stateChecker = (String) session.getAttribute("state_checker");
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), true);
            account.setStateChecker(stateChecker);
        }

        String requestOrigin = UriUtils.getOrigin(session.getContext().getUri().getBaseUri());

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
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
                if (clientSession == null) {
                    clientSession = session.sessions().createClientSession(userSession.getRealm(), client, userSession);
                }
                auth.setClientSession(clientSession);
            }

            account.setUser(auth.getUser());
        }

        account.setFeatures(realm.isIdentityFederationEnabled(), eventStore != null && realm.isEventsEnabled(), true, true);
    }

    public static UriBuilder accountServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
        return base;
    }

    public static UriBuilder accountServiceApplicationPage(UriInfo uriInfo) {
        return accountServiceBaseUrl(uriInfo).path(AccountFormService.class, "applicationsPage");
    }

    protected Set<String> getValidPaths() {
        return AccountFormService.VALID_PATHS;
    }

    private Response forwardToPage(String path, AccountPages page) {
        if (auth != null) {
            try {
                auth.require(AccountRoles.MANAGE_ACCOUNT);
            } catch (ForbiddenException e) {
                return session.getProvider(LoginFormsProvider.class).setError(Messages.NO_ACCESS).createErrorPage(Response.Status.FORBIDDEN);
            }

            setReferrerOnPage();

            UserSessionModel userSession = auth.getSession();

            String tabId = session.getContext().getUri().getQueryParameters().getFirst(org.keycloak.models.Constants.TAB_ID);
            if (tabId != null) {
                AuthenticationSessionModel authSession = new AuthenticationSessionManager(session).getAuthenticationSessionByIdAndClient(realm, userSession.getId(), client, tabId);
                if (authSession != null) {
                    String forwardedError = authSession.getAuthNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
                    if (forwardedError != null) {
                        try {
                            FormMessage errorMessage = JsonSerialization.readValue(forwardedError, FormMessage.class);
                            account.setError(Response.Status.INTERNAL_SERVER_ERROR, errorMessage.getMessage(), errorMessage.getParameters());
                            authSession.removeAuthNote(ACCOUNT_MGMT_FORWARDED_ERROR_NOTE);
                        } catch (IOException ioe) {
                            throw new RuntimeException(ioe);
                        }
                    }
                }
            }

            String locale = session.getContext().getUri().getQueryParameters().getFirst(LocaleSelectorProvider.KC_LOCALE_PARAM);
            if (locale != null) {
                LocaleSelectorProvider localeSelectorProvider = session.getProvider(LocaleSelectorProvider.class);
                localeSelectorProvider.updateUsersLocale(auth.getUser(), locale);
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
        return RealmsResource.accountUrl(base).path(AccountFormService.class, "totpPage");
    }

    @Path("totp")
    @GET
    public Response totpPage() {
        account.setAttribute("mode", session.getContext().getUri().getQueryParameters().getFirst("mode"));
        return forwardToPage("totp", AccountPages.TOTP);
    }

    public static UriBuilder passwordUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(AccountFormService.class, "passwordPage");
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
        if (!realm.isEventsEnabled()) {
            throw new NotFoundException();
        }

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
     * <p>
     * Form params:
     * <p>
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

        List<FormMessage> errors = Validation.validateUpdateProfileForm(realm, formData);
        if (errors != null && !errors.isEmpty()) {
            setReferrerOnPage();
            return account.setErrors(Status.OK, errors).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }

        try {
            updateUsername(formData.getFirst("username"), user, session);
            updateEmail(formData.getFirst("email"), user, session, event);

            user.setFirstName(formData.getFirst("firstName"));
            user.setLastName(formData.getFirst("lastName"));

            AttributeFormDataProcessor.process(formData, realm, user);

            event.success();

            setReferrerOnPage();
            return account.setSuccess(Messages.ACCOUNT_UPDATED).createResponse(AccountPages.ACCOUNT);
        } catch (ReadOnlyException roe) {
            setReferrerOnPage();
            return account.setError(Response.Status.BAD_REQUEST, Messages.READ_ONLY_USER).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        } catch (ModelDuplicateException mde) {
            setReferrerOnPage();
            return account.setError(Response.Status.CONFLICT, mde.getMessage()).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }
    }

    @Path("sessions")
    @POST
    public Response processSessionsLogout(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("sessions");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(formData);

        UserModel user = auth.getUser();

        // Rather decrease time a bit. To avoid situation when user is immediatelly redirected to login screen, then automatically authenticated (eg. with Kerberos) and then seeing issues due the stale token
        // as time on the token will be same like notBefore
        session.users().setNotBeforeForUser(realm, user, Time.currentTime() - 1);

        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel userSession : userSessions) {
            AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(), clientConnection, headers, true);
        }

        UriBuilder builder = Urls.accountBase(session.getContext().getUri().getBaseUri()).path(AccountFormService.class, "sessionsPage");
        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            builder.queryParam("referrer", referrer);

        }
        URI location = builder.build(realm.getName());
        return Response.seeOther(location).build();
    }

    @Path("applications")
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
            setReferrerOnPage();
            return account.setError(Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND).createResponse(AccountPages.APPLICATIONS);
        }
        ClientModel client = realm.getClientById(clientId);
        if (client == null) {
            setReferrerOnPage();
            return account.setError(Response.Status.BAD_REQUEST, Messages.CLIENT_NOT_FOUND).createResponse(AccountPages.APPLICATIONS);
        }

        // Revoke grant in UserModel
        UserModel user = auth.getUser();
        session.users().revokeConsentForClient(realm, user.getId(), client.getId());
        new UserSessionManager(session).revokeOfflineToken(user, client);

        // Logout clientSessions for this user and client
        AuthenticationManager.backchannelLogoutUserFromClient(session, realm, user, client, session.getContext().getUri(), headers);

        event.event(EventType.REVOKE_GRANT).client(auth.getClient()).user(auth.getUser()).detail(Details.REVOKED_CLIENT, client.getClientId()).success();
        setReferrerOnPage();

        UriBuilder builder = Urls.accountBase(session.getContext().getUri().getBaseUri()).path(AccountFormService.class, "applicationsPage");
        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            builder.queryParam("referrer", referrer);

        }
        URI location = builder.build(realm.getName());
        return Response.seeOther(location).build();
    }

    /**
     * Update the TOTP for this account.
     * <p>
     * form parameters:
     * <p>
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

        account.setAttribute("mode", session.getContext().getUri().getQueryParameters().getFirst("mode"));

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.TOTP);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        OTPCredentialProvider otpCredentialProvider = (OTPCredentialProvider) session.getProvider(CredentialProvider.class, "keycloak-otp");
        if (action != null && action.equals("Delete")) {
            String credentialId = formData.getFirst("credentialId");
            if (credentialId == null) {
                setReferrerOnPage();
                return account.setError(Status.OK, Messages.UNEXPECTED_ERROR_HANDLING_REQUEST).createResponse(AccountPages.TOTP);
            }
            otpCredentialProvider.deleteCredential(realm, user, credentialId);
            event.event(EventType.REMOVE_TOTP).client(auth.getClient()).user(auth.getUser()).success();
            setReferrerOnPage();
            return account.setSuccess(Messages.SUCCESS_TOTP_REMOVED).createResponse(AccountPages.TOTP);
        } else {
            String challengeResponse = formData.getFirst("totp");
            String totpSecret = formData.getFirst("totpSecret");
            String userLabel = formData.getFirst("userLabel");

            OTPPolicy policy = realm.getOTPPolicy();
            OTPCredentialModel credentialModel = OTPCredentialModel.createFromPolicy(realm, totpSecret, userLabel);
            if (Validation.isBlank(challengeResponse)) {
                setReferrerOnPage();
                return account.setError(Status.OK, Messages.MISSING_TOTP).createResponse(AccountPages.TOTP);
            } else if (!CredentialValidation.validOTP(challengeResponse, credentialModel, policy.getLookAheadWindow())) {
                setReferrerOnPage();
                return account.setError(Status.OK, Messages.INVALID_TOTP).createResponse(AccountPages.TOTP);
            }


            CredentialModel createdCredential = otpCredentialProvider.createCredential(realm, user, credentialModel);
            UserCredentialModel credential = new UserCredentialModel(createdCredential.getId(), otpCredentialProvider.getType(), challengeResponse);
            if (!otpCredentialProvider.isValid(realm, user, credential)) {
                setReferrerOnPage();
                return account.setError(Status.OK, Messages.INVALID_TOTP).createResponse(AccountPages.TOTP);
            }
            event.event(EventType.UPDATE_TOTP).client(auth.getClient()).user(auth.getUser()).success();

            setReferrerOnPage();
            return account.setSuccess(Messages.SUCCESS_TOTP).createResponse(AccountPages.TOTP);
        }
    }

    /**
     * Update account password
     * <p>
     * Form params:
     * <p>
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
                .user(auth.getSession().getUser());

        if (requireCurrent) {
            if (Validation.isBlank(password)) {
                setReferrerOnPage();
                errorEvent.error(Errors.PASSWORD_MISSING);
                return account.setError(Status.OK, Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
            }

            UserCredentialModel cred = UserCredentialModel.password(password);
            if (!session.userCredentialManager().isValid(realm, user, cred)) {
                setReferrerOnPage();
                errorEvent.error(Errors.INVALID_USER_CREDENTIALS);
                return account.setError(Status.OK, Messages.INVALID_PASSWORD_EXISTING).createResponse(AccountPages.PASSWORD);
            }
        }

        if (Validation.isBlank(passwordNew)) {
            setReferrerOnPage();
            errorEvent.error(Errors.PASSWORD_MISSING);
            return account.setError(Status.OK, Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
        }

        if (!passwordNew.equals(passwordConfirm)) {
            setReferrerOnPage();
            errorEvent.error(Errors.PASSWORD_CONFIRM_ERROR);
            return account.setError(Status.OK, Messages.INVALID_PASSWORD_CONFIRM).createResponse(AccountPages.PASSWORD);
        }

        try {
            session.userCredentialManager().updateCredential(realm, user, UserCredentialModel.password(passwordNew, false));
        } catch (ReadOnlyException mre) {
            setReferrerOnPage();
            errorEvent.error(Errors.NOT_ALLOWED);
            return account.setError(Response.Status.BAD_REQUEST, Messages.READ_ONLY_PASSWORD).createResponse(AccountPages.PASSWORD);
        } catch (ModelException me) {
            ServicesLogger.LOGGER.failedToUpdatePassword(me);
            setReferrerOnPage();
            errorEvent.detail(Details.REASON, me.getMessage()).error(Errors.PASSWORD_REJECTED);
            return account.setError(Response.Status.NOT_ACCEPTABLE, me.getMessage(), me.getParameters()).createResponse(AccountPages.PASSWORD);
        } catch (Exception ape) {
            ServicesLogger.LOGGER.failedToUpdatePassword(ape);
            setReferrerOnPage();
            errorEvent.detail(Details.REASON, ape.getMessage()).error(Errors.PASSWORD_REJECTED);
            return account.setError(Response.Status.INTERNAL_SERVER_ERROR, ape.getMessage()).createResponse(AccountPages.PASSWORD);
        }

        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel s : sessions) {
            if (!s.getId().equals(auth.getSession().getId())) {
                AuthenticationManager.backchannelLogout(session, realm, s, session.getContext().getUri(), clientConnection, headers, true);
            }
        }

        event.event(EventType.UPDATE_PASSWORD).client(auth.getClient()).user(auth.getUser()).success();

        setReferrerOnPage();
        return account.setPasswordSet(true).setSuccess(Messages.ACCOUNT_PASSWORD_UPDATED).createResponse(AccountPages.PASSWORD);
    }

    @Path("identity")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processFederatedIdentityUpdate(final MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("identity");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(formData);
        UserModel user = auth.getUser();

        String action = formData.getFirst("action");
        String providerId = formData.getFirst("providerId");

        if (Validation.isEmpty(providerId)) {
            setReferrerOnPage();
            return account.setError(Status.OK, Messages.MISSING_IDENTITY_PROVIDER).createResponse(AccountPages.FEDERATED_IDENTITY);
        }
        AccountSocialAction accountSocialAction = AccountSocialAction.getAction(action);
        if (accountSocialAction == null) {
            setReferrerOnPage();
            return account.setError(Status.OK, Messages.INVALID_FEDERATED_IDENTITY_ACTION).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        boolean hasProvider = false;

        for (IdentityProviderModel model : realm.getIdentityProviders()) {
            if (model.getAlias().equals(providerId)) {
                hasProvider = true;
            }
        }

        if (!hasProvider) {
            setReferrerOnPage();
            return account.setError(Status.OK, Messages.IDENTITY_PROVIDER_NOT_FOUND).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        if (!user.isEnabled()) {
            setReferrerOnPage();
            return account.setError(Status.OK, Messages.ACCOUNT_DISABLED).createResponse(AccountPages.FEDERATED_IDENTITY);
        }

        switch (accountSocialAction) {
            case ADD:
                String redirectUri = UriBuilder.fromUri(Urls.accountFederatedIdentityPage(session.getContext().getUri().getBaseUri(), realm.getName())).build().toString();

                try {
                    String nonce = UUID.randomUUID().toString();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    String input = nonce + auth.getSession().getId() + client.getClientId() + providerId;
                    byte[] check = md.digest(input.getBytes(StandardCharsets.UTF_8));
                    String hash = Base64Url.encode(check);
                    URI linkUrl = Urls.identityProviderLinkRequest(this.session.getContext().getUri().getBaseUri(), providerId, realm.getName());
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
                    return account.setError(Response.Status.INTERNAL_SERVER_ERROR, Messages.IDENTITY_PROVIDER_REDIRECT_ERROR).createResponse(AccountPages.FEDERATED_IDENTITY);
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
                        return account.setError(Status.OK, Messages.FEDERATED_IDENTITY_REMOVING_LAST_PROVIDER).createResponse(AccountPages.FEDERATED_IDENTITY);
                    }
                } else {
                    setReferrerOnPage();
                    return account.setError(Status.OK, Messages.FEDERATED_IDENTITY_NOT_ACTIVE).createResponse(AccountPages.FEDERATED_IDENTITY);
                }
            default:
                throw new IllegalArgumentException();
        }
    }

    @Path("resource")
    @GET
    public Response resourcesPage(@QueryParam("resource_id") String resourceId) {
        return forwardToPage("resource", AccountPages.RESOURCES);
    }

    @Path("resource/{resource_id}")
    @GET
    public Response resourceDetailPage(@PathParam("resource_id") String resourceId) {
        return forwardToPage("resource", AccountPages.RESOURCE_DETAIL);
    }

    @Path("resource/{resource_id}/grant")
    @GET
    public Response resourceDetailPageAfterGrant(@PathParam("resource_id") String resourceId) {
        return resourceDetailPage(resourceId);
    }

    @Path("resource/{resource_id}/grant")
    @POST
    public Response grantPermission(@PathParam("resource_id") String resourceId, @FormParam("action") String action, @FormParam("permission_id") String[] permissionId, @FormParam("requester") String requester, MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("resource");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        csrfCheck(formData);

        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        Resource resource = authorization.getStoreFactory().getResourceStore().findById(resourceId, null);

        if (resource == null) {
            return ErrorResponse.error("Invalid resource", Response.Status.BAD_REQUEST);
        }

        if (action == null) {
            return ErrorResponse.error("Invalid action", Response.Status.BAD_REQUEST);
        }

        boolean isGrant = "grant".equals(action);
        boolean isDeny = "deny".equals(action);
        boolean isRevoke = "revoke".equals(action);
        boolean isRevokePolicy = "revokePolicy".equals(action);
        boolean isRevokePolicyAll = "revokePolicyAll".equals(action);

        if (isRevokePolicy || isRevokePolicyAll) {
            List<String> ids = new ArrayList<>(Arrays.asList(permissionId));
            Iterator<String> iterator = ids.iterator();
            PolicyStore policyStore = authorization.getStoreFactory().getPolicyStore();
            Policy policy = null;

            while (iterator.hasNext()) {
                String id = iterator.next();

                if (!id.contains(":")) {
                    policy = policyStore.findById(id, client.getId());
                    iterator.remove();
                    break;
                }
            }

            Set<Scope> scopesToKeep = new HashSet<>();

            if (isRevokePolicyAll) {
                for (Scope scope : policy.getScopes()) {
                    policy.removeScope(scope);
                }
            } else {
                for (String id : ids) {
                    scopesToKeep.add(authorization.getStoreFactory().getScopeStore().findById(id.split(":")[1], client.getId()));
                }

                for (Scope scope : policy.getScopes()) {
                    if (!scopesToKeep.contains(scope)) {
                        policy.removeScope(scope);
                    }
                }
            }

            if (policy.getScopes().isEmpty()) {
                for (Policy associated : policy.getAssociatedPolicies()) {
                    policyStore.delete(associated.getId());
                }

                policyStore.delete(policy.getId());
            }
        } else {
            Map<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.RESOURCE, resource.getId());
            filters.put(PermissionTicket.REQUESTER, session.users().getUserByUsername(requester, realm).getId());

            if (isRevoke) {
                filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());
            } else {
                filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());
            }

            List<PermissionTicket> tickets = ticketStore.find(filters, resource.getResourceServer().getId(), -1, -1);
            Iterator<PermissionTicket> iterator = tickets.iterator();

            while (iterator.hasNext()) {
                PermissionTicket ticket = iterator.next();

                if (isGrant) {
                    if (permissionId != null && permissionId.length > 0 && !Arrays.asList(permissionId).contains(ticket.getId())) {
                        continue;
                    }
                }

                if (isGrant && !ticket.isGranted()) {
                    ticket.setGrantedTimestamp(System.currentTimeMillis());
                    iterator.remove();
                } else if (isDeny || isRevoke) {
                    if (permissionId != null && permissionId.length > 0 && Arrays.asList(permissionId).contains(ticket.getId())) {
                        iterator.remove();
                    }
                }
            }

            for (PermissionTicket ticket : tickets) {
                ticketStore.delete(ticket.getId());
            }
        }

        if (isRevoke || isRevokePolicy || isRevokePolicyAll) {
            return forwardToPage("resource", AccountPages.RESOURCE_DETAIL);
        }

        return forwardToPage("resource", AccountPages.RESOURCES);
    }

    @Path("resource/{resource_id}/share")
    @GET
    public Response resourceDetailPageAfterShare(@PathParam("resource_id") String resourceId) {
        return resourceDetailPage(resourceId);
    }

    @Path("resource/{resource_id}/share")
    @POST
    public Response shareResource(@PathParam("resource_id") String resourceId, @FormParam("user_id") String[] userIds, @FormParam("scope_id") String[] scopes, MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("resource");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        
        csrfCheck(formData);

        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();
        Resource resource = authorization.getStoreFactory().getResourceStore().findById(resourceId, null);

        if (resource == null) {
            return ErrorResponse.error("Invalid resource", Response.Status.BAD_REQUEST);
        }

        if (userIds == null || userIds.length == 0) {
            setReferrerOnPage();
            return account.setError(Status.BAD_REQUEST, Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
        }

        for (String id : userIds) {
            UserModel user = session.users().getUserById(id, realm);

            if (user == null) {
                user = session.users().getUserByUsername(id, realm);
            }

            if (user == null) {
                user = session.users().getUserByEmail(id, realm);
            }

            if (user == null) {
                setReferrerOnPage();
                return account.setError(Status.BAD_REQUEST, Messages.INVALID_USER).createResponse(AccountPages.RESOURCE_DETAIL);
            }

            Map<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.RESOURCE, resource.getId());
            filters.put(PermissionTicket.OWNER, auth.getUser().getId());
            filters.put(PermissionTicket.REQUESTER, user.getId());

            List<PermissionTicket> tickets = ticketStore.find(filters, resource.getResourceServer().getId(), -1, -1);

            if (tickets.isEmpty()) {
                if (scopes != null && scopes.length > 0) {
                    for (String scope : scopes) {
                        PermissionTicket ticket = ticketStore.create(resourceId, scope, user.getId(), resource.getResourceServer());
                        ticket.setGrantedTimestamp(System.currentTimeMillis());
                    }
                } else {
                    if (resource.getScopes().isEmpty()) {
                        PermissionTicket ticket = ticketStore.create(resourceId, null, user.getId(), resource.getResourceServer());
                        ticket.setGrantedTimestamp(System.currentTimeMillis());
                    } else {
                        for (Scope scope : resource.getScopes()) {
                            PermissionTicket ticket = ticketStore.create(resourceId, scope.getId(), user.getId(), resource.getResourceServer());
                            ticket.setGrantedTimestamp(System.currentTimeMillis());
                        }
                    }
                }
            } else if (scopes != null && scopes.length > 0) {
                List<String> grantScopes = new ArrayList<>(Arrays.asList(scopes));

                for (PermissionTicket ticket : tickets) {
                    Scope scope = ticket.getScope();

                    if (scope != null) {
                        grantScopes.remove(scope.getId());
                    }
                }

                for (String grantScope : grantScopes) {
                    PermissionTicket ticket = ticketStore.create(resourceId, grantScope, user.getId(), resource.getResourceServer());
                    ticket.setGrantedTimestamp(System.currentTimeMillis());
                }
            }
        }

        return forwardToPage("resource", AccountPages.RESOURCE_DETAIL);
    }

    @Path("resource")
    @POST
    public Response processResourceActions(@FormParam("resource_id") String[] resourceIds, @FormParam("action") String action, MultivaluedMap<String, String> formData) {
        if (auth == null) {
            return login("resource");
        }

        auth.require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(formData);

        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        PermissionTicketStore ticketStore = authorization.getStoreFactory().getPermissionTicketStore();

        if (action == null) {
            return ErrorResponse.error("Invalid action", Response.Status.BAD_REQUEST);
        }

        for (String resourceId : resourceIds) {
            Resource resource = authorization.getStoreFactory().getResourceStore().findById(resourceId, null);

            if (resource == null) {
                return ErrorResponse.error("Invalid resource", Response.Status.BAD_REQUEST);
            }

            HashMap<String, String> filters = new HashMap<>();

            filters.put(PermissionTicket.REQUESTER, auth.getUser().getId());
            filters.put(PermissionTicket.RESOURCE, resource.getId());

            if ("cancel".equals(action)) {
                filters.put(PermissionTicket.GRANTED, Boolean.TRUE.toString());
            } else if ("cancelRequest".equals(action)) {
                filters.put(PermissionTicket.GRANTED, Boolean.FALSE.toString());
            }

            for (PermissionTicket ticket : ticketStore.find(filters, resource.getResourceServer().getId(), -1, -1)) {
                ticketStore.delete(ticket.getId());
            }
        }

        return forwardToPage("authorization", AccountPages.RESOURCES);
    }

    public static UriBuilder loginRedirectUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(AccountFormService.class, "loginRedirect");
    }

    @Override
    protected URI getBaseRedirectUri() {
        return Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName());
    }

    public static boolean isPasswordSet(KeycloakSession session, RealmModel realm, UserModel user) {
        return session.userCredentialManager().isConfiguredFor(realm, user, PasswordCredentialModel.TYPE);
    }

    private String[] getReferrer() {
        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = session.getContext().getUri().getQueryParameters().getFirst("referrer_uri");

        ClientModel referrerClient = realm.getClientByClientId(referrer);
        if (referrerClient != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, referrerClient);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(session, referrerClient.getRootUrl(), referrerClient.getBaseUrl());
            }

            if (referrerUri != null) {
                String referrerName = referrerClient.getName();
                if (Validation.isBlank(referrerName)) {
                    referrerName = referrer;
                }
                return new String[]{referrerName, referrerUri};
            }
        } else if (referrerUri != null) {
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, client);

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


    private void updateUsername(String username, UserModel user, KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        boolean usernameChanged = username == null || !user.getUsername().equals(username);
        if (realm.isEditUsernameAllowed() && !realm.isRegistrationEmailAsUsername()) {
            if (usernameChanged) {
                UserModel existing = session.users().getUserByUsername(username, realm);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new ModelDuplicateException(Messages.USERNAME_EXISTS);
                }

                user.setUsername(username);
            }
        } else if (usernameChanged) {

        }
    }

    private void updateEmail(String email, UserModel user, KeycloakSession session, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();
        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;
        if (emailChanged && !realm.isDuplicateEmailsAllowed()) {
            UserModel existing = session.users().getUserByEmail(email, realm);
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new ModelDuplicateException(Messages.EMAIL_EXISTS);
            }
        }

        user.setEmail(email);

        if (emailChanged) {
            user.setEmailVerified(false);
            event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
        }

        if (realm.isRegistrationEmailAsUsername()) {
            if (!realm.isDuplicateEmailsAllowed()) {
                UserModel existing = session.users().getUserByEmail(email, realm);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new ModelDuplicateException(Messages.USERNAME_EXISTS);
                }
            }
            user.setUsername(email);
        }
    }

    private void csrfCheck(final MultivaluedMap<String, String> formData) {
        String formStateChecker = formData.getFirst("stateChecker");
        if (formStateChecker == null || !formStateChecker.equals(this.stateChecker)) {
            throw new ForbiddenException();
        }
    }


}
