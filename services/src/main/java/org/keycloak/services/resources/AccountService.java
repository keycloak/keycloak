/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.account.AccountPages;
import org.keycloak.account.AccountProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthRedirect;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.UriUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountService {

    private static final Logger logger = Logger.getLogger(AccountService.class);

    private static Set<String> VALID_PATHS = new HashSet<String>();
    static {
        for (Method m : AccountService.class.getMethods()) {
            Path p = m.getAnnotation(Path.class);
            if (p != null) {
                VALID_PATHS.add(p.value());
            }
        }
    }

    private static final EventType[] LOG_EVENTS = {EventType.LOGIN, EventType.LOGOUT, EventType.REGISTER, EventType.REMOVE_FEDERATED_IDENTITY, EventType.REMOVE_TOTP, EventType.SEND_RESET_PASSWORD,
            EventType.SEND_VERIFY_EMAIL, EventType.SOCIAL_LINK, EventType.UPDATE_EMAIL, EventType.UPDATE_PASSWORD, EventType.UPDATE_PROFILE, EventType.UPDATE_TOTP, EventType.VERIFY_EMAIL};

    private static final Set<String> LOG_DETAILS = new HashSet<String>();
    static {
        LOG_DETAILS.add(Details.UPDATED_EMAIL);
        LOG_DETAILS.add(Details.EMAIL);
        LOG_DETAILS.add(Details.PREVIOUS_EMAIL);
        LOG_DETAILS.add(Details.USERNAME);
        LOG_DETAILS.add(Details.REMEMBER_ME);
        LOG_DETAILS.add(Details.REGISTER_METHOD);
        LOG_DETAILS.add(Details.AUTH_METHOD);
    }

    public static final String KEYCLOAK_STATE_CHECKER = "KEYCLOAK_STATE_CHECKER";

    private RealmModel realm;

    @Context
    private HttpRequest request;

    @Context
    protected HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    @Context
    private KeycloakSession session;

    private final AppAuthManager authManager;
    private final ApplicationModel application;
    private EventBuilder event;
    private AccountProvider account;
    private Auth auth;
    private EventStoreProvider eventStore;
    private String stateChecker;

    public AccountService(RealmModel realm, ApplicationModel application, EventBuilder event) {
        this.realm = realm;
        this.application = application;
        this.event = event;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        eventStore = session.getProvider(EventStoreProvider.class);

        account = session.getProvider(AccountProvider.class).setRealm(realm).setUriInfo(uriInfo).setHttpHeaders(headers);

        AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(session, realm, uriInfo, clientConnection, headers);
        if (authResult != null) {
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), application, authResult.getSession(), false);
        } else {
            authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers);
            if (authResult != null) {
                auth = new Auth(realm, authResult.getToken(), authResult.getUser(), application, authResult.getSession(), true);
                Cookie cookie = headers.getCookies().get(KEYCLOAK_STATE_CHECKER);
                if (cookie != null) {
                    stateChecker = cookie.getValue();
                } else {
                    stateChecker = UUID.randomUUID().toString();
                    String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);
                    boolean secureOnly = realm.getSslRequired().isRequired(clientConnection);
                    CookieHelper.addCookie(KEYCLOAK_STATE_CHECKER, stateChecker, cookiePath, null, null, -1, secureOnly, true);
                }
                account.setStateChecker(stateChecker);
            }
        }

        String requestOrigin = UriUtils.getOrigin(uriInfo.getBaseUri());

        // don't allow cors requests unless they were authenticated by an access token
        // This is to prevent CSRF attacks.
        if (auth != null && auth.isCookieAuthenticated()) {
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
        }

        if (authResult != null) {
            UserSessionModel userSession = authResult.getSession();
            if (userSession != null) {
                boolean associated = false;
                for (ClientSessionModel c : userSession.getClientSessions()) {
                    if (c.getClient().equals(application)) {
                        auth.setClientSession(c);
                        associated = true;
                        break;
                    }
                }
                if (!associated) {
                    ClientSessionModel clientSession = session.sessions().createClientSession(realm, application);
                    clientSession.setUserSession(userSession);
                    auth.setClientSession(clientSession);
                }
            }

            account.setUser(auth.getUser());

        }

        boolean eventsEnabled = eventStore != null && realm.isEventsEnabled();

        // todo find out from federation if password is updatable
        account.setFeatures(realm.isIdentityFederationEnabled(), eventsEnabled, true);
    }

    public static UriBuilder accountServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
        return base;
    }

    public static UriBuilder accountServiceBaseUrl(UriBuilder base) {
        return base.path(RealmsResource.class).path(RealmsResource.class, "getAccountService");
    }

    private Response forwardToPage(String path, AccountPages page) {
        if (auth != null) {
            try {
                require(AccountRoles.MANAGE_ACCOUNT);
            } catch (ForbiddenException e) {
                return Flows.forms(session, realm, null, uriInfo, headers).setError(Messages.NO_ACCESS).createErrorPage();
            }

            setReferrerOnPage();

            return account.createResponse(page);
        } else {
            return login(path);
        }
    }

    protected void setReferrerOnPage() {
        String[] referrer = getReferrer();
        if (referrer != null) {
            account.setReferrer(referrer);
        }
    }

    /**
     * CORS preflight
     *
     * @return
     */
    @Path("/")
    @OPTIONS
    public Response accountPreflight() {
        return Cors.add(request, Response.ok()).auth().preflight().build();
    }

    /**
     * Get account information.
     *
     * @return
     */
    @Path("/")
    @GET
    public Response accountPage() {
        List<MediaType> types = headers.getAcceptableMediaTypes();
        if (types.contains(MediaType.WILDCARD_TYPE) || (types.contains(MediaType.TEXT_HTML_TYPE))) {
            return forwardToPage(null, AccountPages.ACCOUNT);
        } else if (types.contains(MediaType.APPLICATION_JSON_TYPE)) {
            requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.VIEW_PROFILE);

            UserRepresentation rep = ModelToRepresentation.toRepresentation(auth.getUser());
            if (rep.getAttributes() != null) {
                Iterator<String> itr = rep.getAttributes().keySet().iterator();
                while (itr.hasNext()) {
                    if (itr.next().startsWith("keycloak.")) {
                        itr.remove();
                    }
                }
            }

            return Cors.add(request, Response.ok(rep)).auth().allowedOrigins(auth.getToken()).build();
        } else {
            return Response.notAcceptable(Variant.VariantListBuilder.newInstance().mediaTypes(MediaType.TEXT_HTML_TYPE, MediaType.APPLICATION_JSON_TYPE).build()).build();
        }
    }

    public static UriBuilder totpUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(AccountService.class, "totpPage");
    }
    @Path("totp")
    @GET
    public Response totpPage() {
        return forwardToPage("totp", AccountPages.TOTP);
    }

    public static UriBuilder passwordUrl(UriBuilder base) {
        return RealmsResource.accountUrl(base).path(AccountService.class, "passwordPage");
    }
    @Path("password")
    @GET
    public Response passwordPage() {
        if (auth != null) {
            account.setPasswordSet(isPasswordSet(auth.getUser()));
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
            List<Event> events = eventStore.createQuery().type(LOG_EVENTS).user(auth.getUser().getId()).maxResults(30).getResultList();
            for (Event e : events) {
                if (e.getDetails() != null) {
                    Iterator<Map.Entry<String, String>> itr = e.getDetails().entrySet().iterator();
                    while (itr.hasNext()) {
                        if (!LOG_DETAILS.contains(itr.next().getKey())) {
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

    /**
     * Check to see if form post has sessionId hidden field and match it against the session id.
     *
     * @param formData
     */
    protected void csrfCheck(final MultivaluedMap<String, String> formData) {
        if (!auth.isCookieAuthenticated()) return;
        String stateChecker = formData.getFirst("stateChecker");
        if (!this.stateChecker.equals(stateChecker)) {
            throw new ForbiddenException();
        }

    }

    /**
     * Check to see if form post has sessionId hidden field and match it against the session id.
     *
     */
    protected void csrfCheck(String stateChecker) {
        if (!auth.isCookieAuthenticated()) return;
        if (auth.getSession() == null) return;
        if (!this.stateChecker.equals(stateChecker)) {
            throw new ForbiddenException();
        }

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

        require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.ACCOUNT);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        String error = Validation.validateUpdateProfileForm(formData);
        if (error != null) {
            setReferrerOnPage();
            return account.setError(error).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }

        try {
            user.setFirstName(formData.getFirst("firstName"));
            user.setLastName(formData.getFirst("lastName"));

            String email = formData.getFirst("email");
            String oldEmail = user.getEmail();
            boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;

            user.setEmail(formData.getFirst("email"));

            AttributeFormDataProcessor.process(formData, realm, user);

            event.event(EventType.UPDATE_PROFILE).client(auth.getClient()).user(auth.getUser()).success();

            if (emailChanged) {
                user.setEmailVerified(false);
                event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
            }
            setReferrerOnPage();
            return account.setSuccess(Messages.ACCOUNT_UPDATED).createResponse(AccountPages.ACCOUNT);
        } catch (ModelReadOnlyException roe) {
            setReferrerOnPage();
            return account.setError(Messages.READ_ONLY_USER).setProfileFormData(formData).createResponse(AccountPages.ACCOUNT);
        }
    }

    @Path("totp-remove")
    @GET
    public Response processTotpRemove(@QueryParam("stateChecker") String stateChecker) {
        if (auth == null) {
            return login("totp");
        }

        require(AccountRoles.MANAGE_ACCOUNT);

        csrfCheck(stateChecker);

        UserModel user = auth.getUser();
        user.setTotp(false);

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

        require(AccountRoles.MANAGE_ACCOUNT);
        csrfCheck(stateChecker);

        UserModel user = auth.getUser();
        session.sessions().removeUserSessions(realm, user);

        UriBuilder builder = Urls.accountBase(uriInfo.getBaseUri()).path(AccountService.class, "sessionsPage");
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

        require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.TOTP);
        }

        csrfCheck(formData);

        UserModel user = auth.getUser();

        String totp = formData.getFirst("totp");
        String totpSecret = formData.getFirst("totpSecret");

        if (Validation.isEmpty(totp)) {
            setReferrerOnPage();
            return account.setError(Messages.MISSING_TOTP).createResponse(AccountPages.TOTP);
        } else if (!new TimeBasedOTP().validate(totp, totpSecret.getBytes())) {
            setReferrerOnPage();
            return account.setError(Messages.INVALID_TOTP).createResponse(AccountPages.TOTP);
        }

        UserCredentialModel credentials = new UserCredentialModel();
        credentials.setType(CredentialRepresentation.TOTP);
        credentials.setValue(totpSecret);
        session.users().updateCredential(realm, user, credentials);

        user.setTotp(true);

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

        require(AccountRoles.MANAGE_ACCOUNT);

        String action = formData.getFirst("submitAction");
        if (action != null && action.equals("Cancel")) {
            setReferrerOnPage();
            return account.createResponse(AccountPages.PASSWORD);
        }

        csrfCheck(formData);
        UserModel user = auth.getUser();

        boolean requireCurrent = isPasswordSet(user);
        account.setPasswordSet(requireCurrent);

        String password = formData.getFirst("password");
        String passwordNew = formData.getFirst("password-new");
        String passwordConfirm = formData.getFirst("password-confirm");

        if (requireCurrent) {
            if (Validation.isEmpty(password)) {
                setReferrerOnPage();
                return account.setError(Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
            }

            UserCredentialModel cred = UserCredentialModel.password(password);
            if (!session.users().validCredentials(realm, user, cred)) {
                setReferrerOnPage();
                return account.setError(Messages.INVALID_PASSWORD_EXISTING).createResponse(AccountPages.PASSWORD);
            }
        }

        if (Validation.isEmpty(passwordNew)) {
            setReferrerOnPage();
            return account.setError(Messages.MISSING_PASSWORD).createResponse(AccountPages.PASSWORD);
        }

        if (!passwordNew.equals(passwordConfirm)) {
            setReferrerOnPage();
            return account.setError(Messages.INVALID_PASSWORD_CONFIRM).createResponse(AccountPages.PASSWORD);
        }

        try {
            session.users().updateCredential(realm, user, UserCredentialModel.password(passwordNew));
        } catch (ModelReadOnlyException mre) {
            setReferrerOnPage();
            return account.setError(Messages.READ_ONLY_PASSWORD).createResponse(AccountPages.PASSWORD);
        }catch (ModelException me) {
            logger.error("Failed to update password", me);
            setReferrerOnPage();
            return account.setError(me.getMessage(), me.getParameters()).createResponse(AccountPages.PASSWORD);
        }catch (Exception ape) {
            logger.error("Failed to update password", ape);
            setReferrerOnPage();
            return account.setError(ape.getMessage()).createResponse(AccountPages.PASSWORD);
        }

        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, user);
        for (UserSessionModel s : sessions) {
            if (!s.getId().equals(auth.getSession().getId())) {
                new ResourceAdminManager().logoutSession(uriInfo.getRequestUri(), realm, s);
                session.sessions().removeUserSession(realm, s);
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
            return login("broker");
        }

        require(AccountRoles.MANAGE_ACCOUNT);
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
                    ClientSessionModel clientSession = auth.getClientSession();
                    ClientSessionCode clientSessionCode = new ClientSessionCode(realm, clientSession);
                    clientSessionCode.setAction(ClientSessionModel.Action.AUTHENTICATE);
                    clientSession.setRedirectUri(redirectUri);
                    clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, UUID.randomUUID().toString());

                    return Response.temporaryRedirect(
                            Urls.identityProviderAuthnRequest(this.uriInfo.getBaseUri(), providerId, realm.getName(), clientSessionCode.getCode()))
                            .build();
                } catch (Exception spe) {
                    setReferrerOnPage();
                    return account.setError(Messages.IDENTITY_PROVIDER_REDIRECT_ERROR).createResponse(AccountPages.FEDERATED_IDENTITY);
                }
            case REMOVE:
                FederatedIdentityModel link = session.users().getFederatedIdentity(user, providerId, realm);
                if (link != null) {

                    // Removing last social provider is not possible if you don't have other possibility to authenticate
                    if (session.users().getFederatedIdentities(user, realm).size() > 1 || user.getFederationLink() != null || isPasswordSet(user)) {
                        session.users().removeFederatedIdentity(realm, user, providerId);

                        logger.debugv("Social provider {0} removed successfully from user {1}", providerId, user.getUsername());

                        event.event(EventType.REMOVE_FEDERATED_IDENTITY).client(auth.getClient()).user(auth.getUser())
                                .detail(Details.USERNAME, link.getUserId() + "@" + link.getIdentityProvider())
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
        return RealmsResource.accountUrl(base).path(AccountService.class, "loginRedirect");
    }

    @Path("login-redirect")
    @GET
    public Response loginRedirect(@QueryParam("code") String code,
                                  @QueryParam("state") String state,
                                  @QueryParam("error") String error,
                                  @QueryParam("path") String path,
                                  @QueryParam("referrer") String referrer,
                                  @Context HttpHeaders headers) {
        try {
            if (error != null) {
                logger.debug("error from oauth");
                throw new ForbiddenException("error");
            }
            if (path != null && !VALID_PATHS.contains(path)) {
                throw new BadRequestException("Invalid path");
            }
            if (!realm.isEnabled()) {
                logger.debug("realm not enabled");
                throw new ForbiddenException();
            }
            if (!application.isEnabled()) {
                logger.debug("account management app not enabled");
                throw new ForbiddenException();
            }
            if (code == null) {
                logger.debug("code not specified");
                throw new BadRequestException("code not specified");
            }
            if (state == null) {
                logger.debug("state not specified");
                throw new BadRequestException("state not specified");
            }

            URI accountUri = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName());
            URI redirectUri = path != null ? accountUri.resolve(path) : accountUri;
            if (referrer != null) {
                redirectUri = redirectUri.resolve("?referrer=" + referrer);
            }

            return Response.status(302).location(redirectUri).build();
        } finally {
        }
    }

    private Response login(String path) {
        OAuthRedirect oauth = new OAuthRedirect();
        String authUrl = OIDCLoginProtocolService.authUrl(uriInfo).build(realm.getName()).toString();
        oauth.setAuthUrl(authUrl);

        oauth.setClientId(Constants.ACCOUNT_MANAGEMENT_APP);

        UriBuilder uriBuilder = Urls.accountPageBuilder(uriInfo.getBaseUri()).path(AccountService.class, "loginRedirect");

        if (path != null) {
            uriBuilder.queryParam("path", path);
        }

        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer != null) {
            uriBuilder.queryParam("referrer", referrer);
        }

        String referrerUri = uriInfo.getQueryParameters().getFirst("referrer_uri");
        if (referrerUri != null) {
            uriBuilder.queryParam("referrer_uri", referrerUri);
        }

        URI accountUri = uriBuilder.build(realm.getName());

        oauth.setStateCookiePath(accountUri.getRawPath());
        return oauth.redirect(uriInfo, accountUri.toString());
    }

    public static boolean isPasswordSet(UserModel user) {
        boolean passwordSet = false;

        if (user.getFederationLink() != null) {
            passwordSet = true;
        }

        for (UserCredentialValueModel c : user.getCredentialsDirectly()) {
            if (c.getType().equals(CredentialRepresentation.PASSWORD)) {
                passwordSet = true;
            }
        }
        return passwordSet;
    }

    private String[] getReferrer() {
        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = uriInfo.getQueryParameters().getFirst("referrer_uri");

        ApplicationModel application = realm.getApplicationByName(referrer);
        if (application != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, application);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(uriInfo.getRequestUri(), application.getBaseUrl());
            }

            if (referrerUri != null) {
                return new String[]{referrer, referrerUri};
            }
        } else if (referrerUri != null) {
            ClientModel client = realm.getOAuthClient(referrer);
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, application);

                if (referrerUri != null) {
                    return new String[]{referrer, referrerUri};
                }
            }
        }

        return null;
    }

    public void require(String role) {
        if (auth == null) {
            throw new ForbiddenException();
        }

        if (!auth.hasAppRole(application, role)) {
            throw new ForbiddenException();
        }
    }

    public void requireOneOf(String... roles) {
        if (auth == null) {
            throw new ForbiddenException();
        }

        if (!auth.hasOneOfAppRole(application, roles)) {
            throw new ForbiddenException();
        }
    }

    public enum AccountSocialAction {
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
