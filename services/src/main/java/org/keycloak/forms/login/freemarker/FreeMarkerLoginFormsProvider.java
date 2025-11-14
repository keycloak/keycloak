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
package org.keycloak.forms.login.freemarker;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.RecoveryAuthnCodesFormAuthenticator;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.authentication.requiredactions.util.UserUpdateProfileContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.MessageType;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.AuthenticationSessionBean;
import org.keycloak.forms.login.freemarker.model.ClientBean;
import org.keycloak.forms.login.freemarker.model.CodeBean;
import org.keycloak.forms.login.freemarker.model.EmailBean;
import org.keycloak.forms.login.freemarker.model.FrontChannelLogoutBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.forms.login.freemarker.model.IdpReviewProfileBean;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.forms.login.freemarker.model.LogoutConfirmBean;
import org.keycloak.forms.login.freemarker.model.OAuthGrantBean;
import org.keycloak.forms.login.freemarker.model.OrganizationBean;
import org.keycloak.forms.login.freemarker.model.PasswordPoliciesBean;
import org.keycloak.forms.login.freemarker.model.ProfileBean;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.forms.login.freemarker.model.RecoveryAuthnCodeInputLoginBean;
import org.keycloak.forms.login.freemarker.model.RecoveryAuthnCodesBean;
import org.keycloak.forms.login.freemarker.model.RegisterBean;
import org.keycloak.forms.login.freemarker.model.RequiredActionUrlFormatterMethod;
import org.keycloak.forms.login.freemarker.model.SAMLPostFormBean;
import org.keycloak.forms.login.freemarker.model.TotpBean;
import org.keycloak.forms.login.freemarker.model.TotpLoginBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.forms.login.freemarker.model.VerifyProfileBean;
import org.keycloak.forms.login.freemarker.model.X509ConfirmBean;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.organization.forms.login.freemarker.model.OrganizationAwareIdentityProviderBean;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.AdvancedMessageFormatterMethod;
import org.keycloak.theme.beans.LocaleBean;
import org.keycloak.theme.beans.MessageBean;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.beans.MessagesPerFieldBean;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.MediaTypeMatcher;

import org.jboss.logging.Logger;

import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PASSWORD;
import static org.keycloak.organization.utils.Organizations.resolveOrganization;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProvider implements LoginFormsProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerLoginFormsProvider.class);

    protected String accessCode;
    protected Response.Status status;
    protected List<AuthorizationDetails> clientScopesRequested;
    protected Map<String, String> httpResponseHeaders = new HashMap<>();
    protected URI actionUri;
    protected String execution;
    protected AuthenticationFlowContext context;

    protected List<FormMessage> messages = null;
    protected MessageType messageType = MessageType.ERROR;

    protected MultivaluedMap<String, String> formData;
    protected boolean detachedAuthSession = false;

    protected KeycloakSession session;
    /**
     * authenticationSession can be null for some renderings, mainly error pages
     */
    protected AuthenticationSessionModel authenticationSession;
    protected RealmModel realm;
    protected ClientModel client;
    protected UriInfo uriInfo;

    protected FreeMarkerProvider freeMarker;

    protected UserModel user;

    protected String lang;

    protected final Map<String, Object> attributes = new HashMap<>();
    private Function<Map<String, Object>, Map<String, Object>> attributeMapper;

    public FreeMarkerLoginFormsProvider(KeycloakSession session) {
        this.session = session;
        this.freeMarker = session.getProvider(FreeMarkerProvider.class);
        this.attributes.put("scripts", new LinkedList<>());
        this.realm = session.getContext().getRealm();
        this.client = session.getContext().getClient();
        this.uriInfo = session.getContext().getUri();
        this.lang = Locale.ENGLISH.toLanguageTag();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addScript(String scriptUrl) {
        List<String> scripts = (List<String>) this.attributes.get("scripts");
        scripts.add(scriptUrl);
    }

    @Override
    public Response createResponse(UserModel.RequiredAction action) {

        String actionMessage;
        LoginFormsPages page;

        switch (action) {
            case CONFIGURE_TOTP:
                actionMessage = Messages.CONFIGURE_TOTP;
                page = LoginFormsPages.LOGIN_CONFIG_TOTP;
                break;
            case CONFIGURE_RECOVERY_AUTHN_CODES:
                actionMessage = Messages.CONFIGURE_BACKUP_CODES;
                page = LoginFormsPages.LOGIN_RECOVERY_AUTHN_CODES_CONFIG;
                break;
            case UPDATE_PROFILE:
                this.attributes.put(UPDATE_PROFILE_CONTEXT_ATTR, new UserUpdateProfileContext(realm, user));
                actionMessage = Messages.UPDATE_PROFILE;
                page = LoginFormsPages.LOGIN_UPDATE_PROFILE;
                break;
            case UPDATE_EMAIL:
                UpdateProfileContext updateEmailContext = new UserUpdateProfileContext(realm, user);
                attributes.put("user", new ProfileBean(updateEmailContext, formData));
                actionMessage = Messages.UPDATE_EMAIL;
                page = LoginFormsPages.UPDATE_EMAIL;
                break;
            case UPDATE_PASSWORD:
                boolean isRequestedByAdmin = user.getRequiredActionsStream().filter(Objects::nonNull).anyMatch(UPDATE_PASSWORD.toString()::contains);
                actionMessage = isRequestedByAdmin ? Messages.UPDATE_PASSWORD : Messages.RESET_PASSWORD;
                page = LoginFormsPages.LOGIN_UPDATE_PASSWORD;
                break;
            case VERIFY_EMAIL:
                UpdateProfileContext userBasedContext1 = new UserUpdateProfileContext(realm, user);
                attributes.put("user", new ProfileBean(userBasedContext1, formData));
                if (authenticationSession.getAuthNote(Constants.VERIFY_EMAIL_KEY) != null) {
                    attributes.put("verifyEmail", authenticationSession.getAuthNote(Constants.VERIFY_EMAIL_KEY));
                }
                actionMessage = Messages.VERIFY_EMAIL;
                page = LoginFormsPages.LOGIN_VERIFY_EMAIL;
                break;
            case VERIFY_PROFILE:
                UpdateProfileContext verifyProfile = new UserUpdateProfileContext(realm, user);
                this.attributes.put(UPDATE_PROFILE_CONTEXT_ATTR, verifyProfile);
                actionMessage = Messages.UPDATE_PROFILE;
                page = LoginFormsPages.LOGIN_UPDATE_PROFILE;
                break;
            default:
                return Response.serverError().build();
        }

        if (messages == null) {
            setMessage(MessageType.WARNING, actionMessage);
        }

        return createResponse(page);
    }

    @SuppressWarnings("incomplete-switch")
    protected Response createResponse(LoginFormsPages page) {

        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            return Response.serverError().build();
        }

        Locale locale = session.getContext().resolveLocale(user);
        Properties messagesBundle = handleThemeResources(theme, locale);

        handleMessages(locale, messagesBundle);

        // for some reason Resteasy 2.3.7 doesn't like query params and form params with the same name and will null out the code form param
        UriBuilder uriBuilder = prepareBaseUriBuilder(page == LoginFormsPages.OAUTH_GRANT);
        createCommonAttributes(theme, locale, messagesBundle, uriBuilder, page);

        attributes.put("login", new LoginBean(formData));
        if (status != null) {
            attributes.put("statusCode", status.getStatusCode());
        }


        if (!isDetachedAuthenticationSession()) {
            if ((AuthenticationSessionModel.Action.AUTHENTICATE.name().equals(authenticationSession.getAction())) ||
                    (AuthenticationSessionModel.Action.REQUIRED_ACTIONS.name().equals(authenticationSession.getAction())) ||
                    (AuthenticationSessionModel.Action.OAUTH_GRANT.name().equals(authenticationSession.getAction()))) {
                setAttribute("authenticationSession", new AuthenticationSessionBean(authenticationSession.getParentSession().getId(), authenticationSession.getTabId()));
            }
        }

        switch (page) {
            case LOGIN_CONFIG_TOTP:
                TotpBean totpBean = new TotpBean(session, realm, user, getTotpUriBuilder(), authenticationSession.getAuthNote(Constants.TOTP_SECRET_KEY));
                authenticationSession.setAuthNote(Constants.TOTP_SECRET_KEY, totpBean.getTotpSecret());
                attributes.put("totp", totpBean);
                break;
            case LOGIN_RECOVERY_AUTHN_CODES_CONFIG:
                // generate the recovery codes and assign to the auth session
                RecoveryAuthnCodesBean recoveryAuthnCodesBean = new RecoveryAuthnCodesBean();
                attributes.put("recoveryAuthnCodesConfigBean", recoveryAuthnCodesBean);
                authenticationSession.setAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_RECOVERY_AUTHN_CODES_NOTE, recoveryAuthnCodesBean.getGeneratedRecoveryAuthnCodesAsString());
                authenticationSession.setAuthNote(RecoveryAuthnCodesFormAuthenticator.GENERATED_AT_NOTE, Long.toString(recoveryAuthnCodesBean.getGeneratedAt()));
                break;
            case LOGIN_RECOVERY_AUTHN_CODES_INPUT:
                attributes.put("recoveryAuthnCodesInputBean", new RecoveryAuthnCodeInputLoginBean(session, realm, user));
                break;
            case LOGIN_UPDATE_PROFILE:
                attributes.put("profile", new VerifyProfileBean(user, formData, session));
                UpdateProfileContext userCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);
                attributes.put("user", new ProfileBean(userCtx, formData));
                break;
            case UPDATE_EMAIL:
                EmailBean emailBean = new EmailBean(user, formData, session);
                attributes.put("profile", emailBean);
                // only for backward compatibility but should be removed once declarative user profile is supported
                attributes.put("email", emailBean);
                break;
            case LOGIN_IDP_LINK_CONFIRM:
            case LOGIN_IDP_LINK_CONFIRM_OVERRIDE:
            case LOGIN_IDP_LINK_EMAIL:
                BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
                String idpAlias = brokerContext.getIdpConfig().getAlias();
                idpAlias = ObjectUtil.capitalize(idpAlias);
                String displayName = idpAlias;
                if (!ObjectUtil.isBlank(brokerContext.getIdpConfig().getDisplayName())) {
                    displayName = brokerContext.getIdpConfig().getDisplayName();
                }

                attributes.put("brokerContext", brokerContext);
                attributes.put("idpAlias", idpAlias);
                attributes.put("idpDisplayName", displayName);
                break;
            case LOGIN_TOTP:
                attributes.put("otpLogin", new TotpLoginBean(session, realm, user, (String) this.attributes.get(OTPFormAuthenticator.SELECTED_OTP_CREDENTIAL_ID)));
                break;
            case LOGIN_RESET_OTP:
                attributes.put("configuredOtpCredentials", new TotpLoginBean(session, realm, user, (String) this.attributes.get(OTPFormAuthenticator.SELECTED_OTP_CREDENTIAL_ID)));
                break;
            case REGISTER:
                RegisterBean rb = new RegisterBean(formData, session);
                //legacy bean for static template
                attributes.put("register", rb);
                //bean for dynamic template
                attributes.put("profile", rb);
                break;
            case OAUTH_GRANT:
                attributes.put("oauth",
                        new OAuthGrantBean(accessCode, client, clientScopesRequested));
                break;
            case CODE:
                attributes.remove("message"); // No need to include "message" attribute as error is included in separate field anyway
                attributes.put(OAuth2Constants.CODE, new CodeBean(accessCode, messageType == MessageType.ERROR ? getFirstMessageUnformatted() : null));
                break;
            case X509_CONFIRM:
                attributes.put("x509", new X509ConfirmBean(formData));
                break;
            case SAML_POST_FORM:
                attributes.put("samlPost", new SAMLPostFormBean(formData));
                break;
            case IDP_REVIEW_USER_PROFILE:
                UpdateProfileContext idpCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);
                attributes.put("profile", new IdpReviewProfileBean(idpCtx, formData, session));
                attributes.put("user", new ProfileBean(idpCtx, formData));
                break;
            case FRONTCHANNEL_LOGOUT:
                attributes.put("logout", new FrontChannelLogoutBean(session));
                break;
            case LOGOUT_CONFIRM:
                attributes.put("logoutConfirm", new LogoutConfirmBean(accessCode, authenticationSession));
                break;
        }

        return processTemplate(theme, Templates.getTemplate(page), locale);
    }

    /**
     * Get sure that correct hostname and path is used for totp form.
     * Relevant when running in proxy mode.
     *
     * @return UriBuilder with configured hostname and path set
     */
    private UriBuilder getTotpUriBuilder() {
        return uriInfo.getBaseUriBuilder()
                .path(uriInfo.getPath())
                .replaceQuery(uriInfo.getRequestUri().getQuery());
    }

    @Override
    public Response createForm(String form) {
        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            return Response.serverError().build();
        }

        Locale locale = session.getContext().resolveLocale(user);
        Properties messagesBundle = handleThemeResources(theme, locale);

        handleMessages(locale, messagesBundle);

        UriBuilder uriBuilder = prepareBaseUriBuilder(false);
        createCommonAttributes(theme, locale, messagesBundle, uriBuilder, null);

        return processTemplate(theme, form, locale);
    }

    /**
     * Prepare base uri builder for later use
     *
     * @param resetRequestUriParams - for some reason Resteasy 2.3.7 doesn't like query params and form params with the same name and will null out the code form param, so we have to reset them for some pages
     * @return base uri builder
     */
    protected UriBuilder prepareBaseUriBuilder(boolean resetRequestUriParams) {
        String requestURI = uriInfo.getBaseUri().getPath();
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);
        if (resetRequestUriParams) {
            uriBuilder.replaceQuery(null);
        }

        if (client != null) {
            uriBuilder.queryParam(Constants.CLIENT_ID, client.getClientId());
        }
        if (authenticationSession != null) {
            uriBuilder.queryParam(Constants.TAB_ID, authenticationSession.getTabId());
            String authSessionAction = authenticationSession.getAction();
            if (!AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(authSessionAction) && !AuthenticationSessionModel.Action.LOGGED_OUT.name().equals(authSessionAction)) {
                uriBuilder.queryParam(Constants.CLIENT_DATA, AuthenticationProcessor.getClientData(session, authenticationSession));
            }
        }
        return uriBuilder;
    }

    /**
     * Get Theme used for page rendering.
     *
     * @return theme for page rendering, never null
     * @throws IOException in case of Theme loading problem
     */
    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.LOGIN);
    }

    /**
     * Load message bundle and place it into <code>msg</code> template attribute. Also load Theme properties and place them into <code>properties</code> template attribute.
     *
     * @param theme  actual Theme to load bundle from
     * @param locale to load bundle for
     * @return message bundle for other use
     */
    protected Properties handleThemeResources(Theme theme, Locale locale) {
        Properties messagesBundle;
        try {
            messagesBundle = theme.getEnhancedMessages(realm, locale);
            Map<Object, Object> msgParams = new HashMap<>(attributes);
            msgParams.putAll(messagesBundle);
            attributes.put("msg", new MessageFormatterMethod(locale, msgParams));
            attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }

        try {
            Properties properties = theme.getProperties();
            attributes.put("properties", properties);
            attributes.put("darkMode", "true".equals(properties.getProperty("darkMode"))
                    && realm.getAttribute("darkMode", true));
        } catch (IOException e) {
            logger.warn("Failed to load properties", e);
        }

        return messagesBundle;
    }

    /**
     * Handle messages to be shown on the page - set them to template attributes
     *
     * @param locale         to be used for message text loading
     * @param messagesBundle to be used for message text loading
     * @see #messageType
     * @see #messages
     */
    protected void handleMessages(Locale locale, Properties messagesBundle) {
        MessagesPerFieldBean messagesPerField = new MessagesPerFieldBean();
        if (messages != null) {
            MessageBean wholeMessage = new MessageBean(null, messageType);
            for (FormMessage message : this.messages) {
                String formattedMessageText = formatMessage(message, messagesBundle, locale);
                if (formattedMessageText != null) {
                    wholeMessage.appendSummaryLine(formattedMessageText);
                    messagesPerField.addMessage(message.getField(), formattedMessageText, messageType);
                }
            }
            attributes.put("message", wholeMessage);
        } else {
            attributes.remove("message");
        }
        attributes.put("messagesPerField", messagesPerField);
    }

    @Override
    public String getMessage(String message, Object... parameters) {
        return formatMessage(new FormMessage(null, message, parameters));
    }

    /**
     * Create common attributes used in all templates.
     *
     * @param theme          actual Theme used (provided by <code>getTheme()</code>)
     * @param locale         actual locale
     * @param messagesBundle actual message bundle (provided by <code>handleThemeResources()</code>)
     * @param baseUriBuilder actual base uri builder (provided by <code>prepareBaseUriBuilder()</code>)
     * @param page           in case if common page is rendered, is null if called from <code>createForm()</code>
     */
    protected void createCommonAttributes(Theme theme, Locale locale, Properties messagesBundle, UriBuilder baseUriBuilder, LoginFormsPages page) {
        URI baseUri = baseUriBuilder.build();
        if (accessCode != null) {
            baseUriBuilder.queryParam(LoginActionsService.SESSION_CODE, accessCode);
        }
        URI baseUriWithCodeAndClientId = baseUriBuilder.build();

        if (client != null) {
            attributes.put("client", new ClientBean(session, client));
        }

        if (realm != null) {
            attributes.put("realm", new RealmBean(realm));

            IdentityProviderBean idpBean = new IdentityProviderBean(session, realm, baseUriWithCodeAndClientId, context);

            if (Profile.isFeatureEnabled(Feature.ORGANIZATION) && realm.isOrganizationsEnabled()) {
                idpBean = new OrganizationAwareIdentityProviderBean(idpBean);
            }

            attributes.put("social", idpBean);
            attributes.put("url", new UrlBean(realm, theme, baseUri, this.actionUri));
            attributes.put("requiredActionUrl", new RequiredActionUrlFormatterMethod(realm, baseUri));
            attributes.put("auth", new AuthenticationContextBean(context, page));
            if (authenticationSession != null && Boolean.parseBoolean(authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.USERNAME_HIDDEN))) {
                attributes.put(LoginFormsProvider.USERNAME_HIDDEN, Boolean.TRUE.toString());
            }
            setAttribute(Constants.EXECUTION, execution);

            if (realm.isInternationalizationEnabled()) {
                UriBuilder b;
                if (page != null) {
                    switch (page) {
                        case LOGIN:
                        case LOGIN_USERNAME:
                        case LOGIN_WEBAUTHN:
                        case X509_CONFIRM:
                            b = UriBuilder.fromUri(Urls.realmLoginPage(baseUri, realm.getName()));
                            break;
                        case REGISTER:
                            b = UriBuilder.fromUri(Urls.realmRegisterPage(baseUri, realm.getName()));
                            break;
                        case LOGOUT_CONFIRM:
                            b = UriBuilder.fromUri(Urls.logoutConfirm(baseUri, realm.getName()));
                            break;
                        case LOGIN_PAGE_EXPIRED:
                            b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
                            break;
                        case INFO:
                        case ERROR:
                            if (isDetachedAuthenticationSession()) {
                                FormMessage formMessage = getFirstMessage();
                                if (formMessage == null) {
                                    throw new IllegalStateException("Not able to create info/error page with detached authentication session as no info/error message available");
                                }

                                DetachedInfoStateCookie cookie = new DetachedInfoStateChecker(session, realm).generateAndSetCookie(
                                        formMessage.getMessage(), messageType.toString(), status == null ? null : status.getStatusCode(),
                                        client == null ? null : client.getId(), formMessage.getParameters());

                                b = UriBuilder.fromUri(Urls.loginActionsDetachedInfo(baseUri, realm.getName()))
                                        .queryParam(DetachedInfoStateChecker.STATE_CHECKER_PARAM, cookie.getRenderedUrlState());
                                break;
                            }
                        default:
                            b = getDefaultPageUriForLocale(baseUri);
                            break;
                    }
                } else {
                    b = getDefaultPageUriForLocale(baseUri);
                }

                if (execution != null) {
                    b.queryParam(Constants.EXECUTION, execution);
                }

                if (authenticationSession != null && authenticationSession.getAuthNote(Constants.KEY) != null) {
                    b.queryParam(Constants.KEY, authenticationSession.getAuthNote(Constants.KEY));
                } else {
                    HttpRequest request = session.getContext().getHttpRequest();
                    MultivaluedMap<String, String> queryParameters = request.getUri().getQueryParameters();

                    if (queryParameters != null && queryParameters.getFirst(Constants.TOKEN) != null) {
                        // changing locale should forward the action token
                        b.queryParam(Constants.TOKEN, queryParameters.getFirst(Constants.TOKEN));
                    }
                }

                final var localeBean = new LocaleBean(realm, locale, b, messagesBundle);
                attributes.put("locale", localeBean);

                lang = localeBean.getCurrentLanguageTag();
            }

            if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
                OrganizationModel organization = resolveOrganization(session, user);

                if (organization != null) {
                    attributes.put("org", new OrganizationBean(organization, user));
                }
            }
            attributes.put("passwordPolicies", new PasswordPoliciesBean(realm.getPasswordPolicy()));
        }
        if (realm != null && user != null && session != null) {
            attributes.put("authenticatorConfigured", new AuthenticatorConfiguredMethod(realm, user, session));
        }

        if (authenticationSession != null && authenticationSession.getClientNote(Constants.KC_ACTION_EXECUTING) != null
                && !Boolean.TRUE.toString().equals(authenticationSession.getClientNote(Constants.KC_ACTION_ENFORCED))) {
            attributes.put("isAppInitiatedAction", true);
        }

        attributes.put("lang", lang);
    }

    private UriBuilder getDefaultPageUriForLocale(URI baseUri) {
        // Using "actionUri" by default in the language combobox when available
        return actionUri != null ? UriBuilder.fromUri(actionUri.getPath()).replaceQuery(baseUri.getQuery()) :
                UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
    }

    /**
     * Process FreeMarker template and prepare Response. Some fields are used for rendering also.
     *
     * @param theme        to be used (provided by <code>getTheme()</code>)
     * @param templateName name of the template to be rendered
     * @param locale       to be used
     * @return Response object to be returned to the browser, never null
     */
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        try {
            Map<String, Object> attributes = Optional.ofNullable(attributeMapper).orElse(Function.identity()).apply(this.attributes);
            if (!attributes.containsKey("templateName")) {
                attributes.put("templateName", templateName);
            }

            attributes.put("pageId", templateName.substring(0, templateName.length() - 4));

            String result = freeMarker.processTemplate(attributes, templateName, theme);
            Response.ResponseBuilder builder = Response.status(status == null ? Response.Status.OK : status).type(MediaType.TEXT_HTML_UTF_8_TYPE).language(locale).entity(result);
            for (Map.Entry<String, String> entry : httpResponseHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            return builder.build();
        } catch (FreeMarkerException e) {
            logger.error("Failed to process template", e);
            return Response.serverError().build();
        }
    }

    @Override
    public Response createLoginUsernamePassword() {
        return createResponse(LoginFormsPages.LOGIN);
    }

    public Response createLoginUsername() {
        return createResponse(LoginFormsPages.LOGIN_USERNAME);
    }

    public Response createLoginPassword() {
        return createResponse(LoginFormsPages.LOGIN_PASSWORD);
    }

    @Override
    public Response createPasswordReset() {
        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (loginHint != null && !loginHint.isEmpty()) {
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, loginHint);
        }
        return createResponse(LoginFormsPages.LOGIN_RESET_PASSWORD);
    }

    @Override
    public Response createOtpReset() {
        return createResponse(LoginFormsPages.LOGIN_RESET_OTP);
    }

    @Override
    public Response createLoginTotp() {
        return createResponse(LoginFormsPages.LOGIN_TOTP);
    }

    @Override
    public Response createLoginRecoveryAuthnCode() {
        return createResponse(LoginFormsPages.LOGIN_RECOVERY_AUTHN_CODES_INPUT);
    }

    @Override
    public Response createLoginWebAuthn() {
        return createResponse(LoginFormsPages.LOGIN_WEBAUTHN);
    }

    @Override
    public Response createRegistration() {
        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (loginHint != null && !loginHint.isEmpty()) {
            if (this.formData == null) {
                this.formData = new MultivaluedHashMap<>();
            }
            if (this.realm.isRegistrationEmailAsUsername()) {
                String value = this.formData.getFirst("email");
                if (value == null || value.trim().isEmpty()) {
                    this.formData.putSingle("email", loginHint);
                }
            } else {
                String value = this.formData.getFirst("username");
                if (value == null || value.trim().isEmpty()) {
                    this.formData.putSingle("username", loginHint);
                }
            }
        }

        if (Profile.isFeatureEnabled(Feature.ORGANIZATION)) {
            String email = (String) attributes.get(RegistrationPage.FIELD_EMAIL);
            if (this.formData == null) {
                this.formData = new MultivaluedHashMap<>();
            }
            String value = this.formData.getFirst(RegistrationPage.FIELD_EMAIL);
            if (value == null || value.trim().isEmpty()) {
                this.formData.putSingle(RegistrationPage.FIELD_EMAIL, email);
            }
        }

        return createResponse(LoginFormsPages.REGISTER);
    }

    @Override
    public Response createInfoPage() {
        return createResponse(LoginFormsPages.INFO);
    }

    @Override
    public Response createUpdateProfilePage() {
        // Don't display initial message if we already have some errors
        if (messageType != MessageType.ERROR) {
            setMessage(MessageType.WARNING, Messages.UPDATE_PROFILE);
        }

        UpdateProfileContext userCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);

        if (userCtx != null && userCtx.getUserProfileContext() == UserProfileContext.IDP_REVIEW) {
            return createResponse(LoginFormsPages.IDP_REVIEW_USER_PROFILE);
        }

        return createResponse(LoginFormsPages.LOGIN_UPDATE_PROFILE);
    }

    @Override
    public Response createIdpLinkConfirmLinkPage() {
        return createResponse(LoginFormsPages.LOGIN_IDP_LINK_CONFIRM);
    }

    @Override
    public Response createIdpLinkConfirmOverrideLinkPage() {
        return createResponse(LoginFormsPages.LOGIN_IDP_LINK_CONFIRM_OVERRIDE);
    }

    @Override
    public Response createLoginExpiredPage() {
        return createResponse(LoginFormsPages.LOGIN_PAGE_EXPIRED);
    }

    @Override
    public Response createIdpLinkEmailPage() {
        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        idpAlias = ObjectUtil.capitalize(idpAlias);
        String displayName = idpAlias;
        if (!ObjectUtil.isBlank(brokerContext.getIdpConfig().getDisplayName())) {
            displayName = brokerContext.getIdpConfig().getDisplayName();
        }

        setMessage(MessageType.WARNING, Messages.LINK_IDP, displayName);

        return createResponse(LoginFormsPages.LOGIN_IDP_LINK_EMAIL);
    }

    @Override
    public Response createErrorPage(Response.Status status) {
        if (MediaTypeMatcher.isJsonRequest(session.getContext().getRequestHeaders())) {
            OAuth2ErrorRepresentation errorRep = new OAuth2ErrorRepresentation();
            errorRep.setError(formatMessage(getFirstMessage()));

            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(errorRep)
                    .build();
        }

        this.status = status;
        return createResponse(LoginFormsPages.ERROR);
    }

    @Override
    public Response createWebAuthnErrorPage() {
        return createResponse(LoginFormsPages.ERROR_WEBAUTHN);
    }

    @Override
    public Response createOAuthGrant() {
        return createResponse(LoginFormsPages.OAUTH_GRANT);
    }

    public Response createSelectAuthenticator() {
        return createResponse(LoginFormsPages.LOGIN_SELECT_AUTHENTICATOR);
    }

    @Override
    public Response createOAuth2DeviceVerifyUserCodePage() {
        return createResponse(LoginFormsPages.LOGIN_OAUTH2_DEVICE_VERIFY_USER_CODE);
    }

    @Override
    public Response createCode() {
        return createResponse(LoginFormsPages.CODE);
    }

    @Override
    public Response createX509ConfirmPage() {
        return createResponse(LoginFormsPages.X509_CONFIRM);
    }

    @Override
    public Response createSamlPostForm() {
        return createResponse(LoginFormsPages.SAML_POST_FORM);
    }

    @Override
    public Response createFrontChannelLogoutPage() {
        return createResponse(LoginFormsPages.FRONTCHANNEL_LOGOUT);
    }

    @Override
    public Response createLogoutConfirmPage() {
        return createResponse(LoginFormsPages.LOGOUT_CONFIRM);
    }

    @Override
    public LoginFormsProvider setMessage(MessageType type, String message, Object... parameters) {
        messageType = type;
        messages = new ArrayList<>();
        messages.add(new FormMessage(null, message, parameters));
        return this;
    }

    private FormMessage getFirstMessage() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(0);
        }
        return null;
    }

    protected String getFirstMessageUnformatted() {
        FormMessage formMessage = getFirstMessage();
        return formMessage == null ? null : formMessage.getMessage();
    }

    protected String formatMessage(FormMessage message) {
        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            throw new RuntimeException("Failed to create theme");
        }

        Locale locale = session.getContext().resolveLocale(user);
        Properties messagesBundle = handleThemeResources(theme, locale);
        return formatMessage(message, messagesBundle, locale);
    }

    protected String formatMessage(FormMessage message, Properties messagesBundle, Locale locale) {
        if (message == null)
            return null;
        if (messagesBundle.containsKey(message.getMessage())) {
            return new MessageFormat(messagesBundle.getProperty(message.getMessage()), locale).format(message.getParameters());
        } else {
            return message.getMessage();
        }
    }

    @Override
    public FreeMarkerLoginFormsProvider setError(String message, Object... parameters) {
        setMessage(MessageType.ERROR, message, parameters);
        return this;
    }

    @Override
    public LoginFormsProvider setErrors(List<FormMessage> messages) {
        if (messages == null)
            return this;
        this.messageType = MessageType.ERROR;
        this.messages = new ArrayList<>(messages);
        return this;
    }

    @Override
    public LoginFormsProvider addError(FormMessage errorMessage) {
        if (this.messageType != MessageType.ERROR) {
            this.messageType = null;
            this.messages = null;
        }
        if (messages == null) {
            this.messageType = MessageType.ERROR;
            this.messages = new LinkedList<>();
        }
        this.messages.add(errorMessage);
        return this;

    }

    @Override
    public LoginFormsProvider addSuccess(FormMessage errorMessage) {
        if (this.messageType != MessageType.SUCCESS) {
            this.messageType = null;
            this.messages = null;
        }
        if (messages == null) {
            this.messageType = MessageType.SUCCESS;
            this.messages = new LinkedList<>();
        }
        this.messages.add(errorMessage);
        return this;

    }

    @Override
    public FreeMarkerLoginFormsProvider setSuccess(String message, Object... parameters) {
        setMessage(MessageType.SUCCESS, message, parameters);
        return this;
    }

    @Override
    public FreeMarkerLoginFormsProvider setInfo(String message, Object... parameters) {
        setMessage(MessageType.INFO, message, parameters);
        return this;
    }

    @Override
    public LoginFormsProvider setDetachedAuthSession() {
        detachedAuthSession = true;
        return this;
    }

    private boolean isDetachedAuthenticationSession() {
        return detachedAuthSession || authenticationSession == null;
    }

    @Override
    public LoginFormsProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    @Override
    public FreeMarkerLoginFormsProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public FreeMarkerLoginFormsProvider setFormData(MultivaluedMap<String, String> formData) {
        this.formData = formData;
        return this;
    }

    @Override
    public LoginFormsProvider setClientSessionCode(String accessCode) {
        this.accessCode = accessCode;
        return this;
    }

    @Override
    public LoginFormsProvider setAccessRequest(List<AuthorizationDetails> clientScopesRequested) {
        this.clientScopesRequested = clientScopesRequested;
        return this;
    }

    @Override
    public LoginFormsProvider setAttribute(String name, Object value) {
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name, value);
        }
        return this;
    }

    @Override
    public LoginFormsProvider setStatus(Response.Status status) {
        this.status = status;
        return this;
    }

    @Override
    public LoginFormsProvider setActionUri(URI actionUri) {
        this.actionUri = actionUri;
        return this;
    }

    @Override
    public LoginFormsProvider setExecution(String execution) {
        this.execution = execution;
        return this;
    }

    @Override
    public LoginFormsProvider setResponseHeader(String headerName, String headerValue) {
        this.httpResponseHeaders.put(headerName, headerValue);
        return this;
    }

    @Override
    public LoginFormsProvider setAuthContext(AuthenticationFlowContext context) {
        this.context = context;
        return this;
    }

    @Override
    public LoginFormsProvider setAttributeMapper(Function<Map<String, Object>, Map<String, Object>> mapper) {
        this.attributeMapper = mapper;
        return this;
    }

    @Override
    public void close() {
    }

}
