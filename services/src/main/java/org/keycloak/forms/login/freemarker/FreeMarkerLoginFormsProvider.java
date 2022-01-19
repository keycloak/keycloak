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

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.authentication.requiredactions.util.UpdateProfileContext;
import org.keycloak.authentication.requiredactions.util.UserUpdateProfileContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.forms.login.LoginFormsPages;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.forms.login.freemarker.model.ClientBean;
import org.keycloak.forms.login.freemarker.model.CodeBean;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.forms.login.freemarker.model.IdpReviewProfileBean;
import org.keycloak.forms.login.freemarker.model.LoginBean;
import org.keycloak.forms.login.freemarker.model.FrontChannelLogoutBean;
import org.keycloak.forms.login.freemarker.model.OAuthGrantBean;
import org.keycloak.forms.login.freemarker.model.ProfileBean;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.forms.login.freemarker.model.RegisterBean;
import org.keycloak.forms.login.freemarker.model.RequiredActionUrlFormatterMethod;
import org.keycloak.forms.login.freemarker.model.SAMLPostFormBean;
import org.keycloak.forms.login.freemarker.model.TotpBean;
import org.keycloak.forms.login.freemarker.model.TotpLoginBean;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.forms.login.freemarker.model.VerifyProfileBean;
import org.keycloak.forms.login.freemarker.model.X509ConfirmBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.rar.AuthorizationDetails;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.AdvancedMessageFormatterMethod;
import org.keycloak.theme.beans.LocaleBean;
import org.keycloak.theme.beans.MessageBean;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.beans.MessageType;
import org.keycloak.theme.beans.MessagesPerFieldBean;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.utils.MediaType;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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
import java.util.Properties;

import static org.keycloak.models.UserModel.RequiredAction.UPDATE_PASSWORD;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProvider implements LoginFormsProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerLoginFormsProvider.class);

    protected String accessCode;
    protected Response.Status status;
    protected javax.ws.rs.core.MediaType contentType;
    protected List<AuthorizationDetails> clientScopesRequested;
    protected Map<String, String> httpResponseHeaders = new HashMap<>();
    protected URI actionUri;
    protected String execution;
    protected AuthenticationFlowContext context;

    protected List<FormMessage> messages = null;
    protected MessageType messageType = MessageType.ERROR;

    protected MultivaluedMap<String, String> formData;

    protected KeycloakSession session;
    /** authenticationSession can be null for some renderings, mainly error pages */
    protected AuthenticationSessionModel authenticationSession;
    protected RealmModel realm;
    protected ClientModel client;
    protected UriInfo uriInfo;

    protected FreeMarkerUtil freeMarker;

    protected UserModel user;

    protected final Map<String, Object> attributes = new HashMap<>();

    public FreeMarkerLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
        this.attributes.put("scripts", new LinkedList<>());
        this.realm = session.getContext().getRealm();
        this.client = session.getContext().getClient();
        this.uriInfo = session.getContext().getUri();
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
            case UPDATE_PROFILE:
                UpdateProfileContext userBasedContext = new UserUpdateProfileContext(realm, user);
                this.attributes.put(UPDATE_PROFILE_CONTEXT_ATTR, userBasedContext);

                actionMessage = Messages.UPDATE_PROFILE;
                if(isDynamicUserProfile()) {
                    page = LoginFormsPages.UPDATE_USER_PROFILE;
                } else {
                    page = LoginFormsPages.LOGIN_UPDATE_PROFILE;
                }
                break;
            case UPDATE_PASSWORD:
                boolean isRequestedByAdmin = user.getRequiredActionsStream().filter(Objects::nonNull).anyMatch(UPDATE_PASSWORD.toString()::contains);
                actionMessage = isRequestedByAdmin ? Messages.UPDATE_PASSWORD : Messages.RESET_PASSWORD;
                page = LoginFormsPages.LOGIN_UPDATE_PASSWORD;
                break;
            case VERIFY_EMAIL:
                actionMessage = Messages.VERIFY_EMAIL;
                page = LoginFormsPages.LOGIN_VERIFY_EMAIL;
                break;
            case VERIFY_PROFILE:
                UpdateProfileContext verifyProfile = new UserUpdateProfileContext(realm, user);
                this.attributes.put(UPDATE_PROFILE_CONTEXT_ATTR, verifyProfile);

                actionMessage = Messages.UPDATE_PROFILE;
                page = LoginFormsPages.UPDATE_USER_PROFILE;
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

        switch (page) {
            case LOGIN_CONFIG_TOTP:
                attributes.put("totp", new TotpBean(session, realm, user, uriInfo.getRequestUriBuilder()));
                break;
            case LOGIN_UPDATE_PROFILE:
                UpdateProfileContext userCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);
                attributes.put("user", new ProfileBean(userCtx, formData));
                break;
            case LOGIN_IDP_LINK_CONFIRM:
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
            case REGISTER:
                if(isDynamicUserProfile()) {
                    page = LoginFormsPages.REGISTER_USER_PROFILE;
                }
                RegisterBean rb = new RegisterBean(formData,session);
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
                attributes.put(OAuth2Constants.CODE, new CodeBean(accessCode, messageType == MessageType.ERROR ? getFirstMessageUnformatted() : null));
                break;
            case X509_CONFIRM:
                attributes.put("x509", new X509ConfirmBean(formData));
                break;
            case SAML_POST_FORM:
                attributes.put("samlPost", new SAMLPostFormBean(formData));
                break;
            case UPDATE_USER_PROFILE:
                attributes.put("profile", new VerifyProfileBean(user, formData, session));
                break;
            case IDP_REVIEW_USER_PROFILE:
                UpdateProfileContext idpCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);
                attributes.put("profile", new IdpReviewProfileBean(idpCtx, formData, session));
                break;
            case FRONTCHANNEL_LOGOUT:
                attributes.put("logout", new FrontChannelLogoutBean(session));
                break;
        }

        return processTemplate(theme, Templates.getTemplate(page), locale);
    }
    
    private boolean isDynamicUserProfile() {
        return session.getProvider(UserProfileProvider.class).getConfiguration() != null;
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
     * @param theme actual Theme to load bundle from
     * @param locale to load bundle for
     * @return message bundle for other use
     */
    protected Properties handleThemeResources(Theme theme, Locale locale) {
        Properties messagesBundle = new Properties();
        try {
            messagesBundle.putAll(theme.getMessages(locale));
            messagesBundle.putAll(realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag()));
            attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
            attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }

        try {
            attributes.put("properties", theme.getProperties());
        } catch (IOException e) {
            logger.warn("Failed to load properties", e);
        }

        return messagesBundle;
    }

    /**
     * Handle messages to be shown on the page - set them to template attributes
     * 
     * @param locale to be used for message text loading
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
            attributes.put("message", null);
        }
        attributes.put("messagesPerField", messagesPerField);
    }

    @Override
    public String getMessage(String message) {
        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            throw new RuntimeException("Failed to create theme");
        }

        Locale locale = session.getContext().resolveLocale(user);
        Properties messagesBundle = handleThemeResources(theme, locale);
        Map<String, String> localizationTexts = realm.getRealmLocalizationTextsByLocale(locale.getCountry());
        messagesBundle.putAll(localizationTexts);
        FormMessage msg = new FormMessage(null, message);
        return formatMessage(msg, messagesBundle, locale);
    }

    @Override
    public String getMessage(String message, String... parameters) {
        Theme theme;
        try {
            theme = getTheme();
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            throw new RuntimeException("Failed to create theme");
        }

        Locale locale = session.getContext().resolveLocale(user);
        Properties messagesBundle = handleThemeResources(theme, locale);
        Map<String, String> localizationTexts = realm.getRealmLocalizationTextsByLocale(locale.getCountry());
        messagesBundle.putAll(localizationTexts);
        FormMessage msg = new FormMessage(message, (Object[]) parameters);
        return formatMessage(msg, messagesBundle, locale);
    }


    /**
     * Create common attributes used in all templates.
     * 
     * @param theme actual Theme used (provided by <code>getTheme()</code>) 
     * @param locale actual locale
     * @param messagesBundle actual message bundle (provided by <code>handleThemeResources()</code>)
     * @param baseUriBuilder actual base uri builder (provided by <code>prepareBaseUriBuilder()</code>)
     * @param page in case if common page is rendered, is null if called from <code>createForm()</code>
     * 
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

            List<IdentityProviderModel> identityProviders = LoginFormsUtil
                    .filterIdentityProviders(realm.getIdentityProvidersStream(), session, context);
            attributes.put("social", new IdentityProviderBean(realm, session, identityProviders, baseUriWithCodeAndClientId));

            attributes.put("url", new UrlBean(realm, theme, baseUri, this.actionUri));
            attributes.put("requiredActionUrl", new RequiredActionUrlFormatterMethod(realm, baseUri));
            attributes.put("auth", new AuthenticationContextBean(context, page));
            attributes.put(Constants.EXECUTION, execution);

            if (realm.isInternationalizationEnabled()) {
                UriBuilder b;
                if (page != null) {
                    switch (page) {
                        case LOGIN:
                        case LOGIN_USERNAME:
                        case X509_CONFIRM:
                            b = UriBuilder.fromUri(Urls.realmLoginPage(baseUri, realm.getName()));
                            break;
                        case REGISTER:
                            b = UriBuilder.fromUri(Urls.realmRegisterPage(baseUri, realm.getName()));
                            break;
                        default:
                            b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
                            break;
                    }
                } else {
                    b = UriBuilder.fromUri(baseUri)
                            .path(uriInfo.getPath());
                }

                if (execution != null) {
                    b.queryParam(Constants.EXECUTION, execution);
                }

                if (authenticationSession != null && authenticationSession.getAuthNote(Constants.KEY) != null) {
                    b.queryParam(Constants.KEY, authenticationSession.getAuthNote(Constants.KEY));
                }

                attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
            }
        }
        if (realm != null && user != null && session != null) {
            attributes.put("authenticatorConfigured", new AuthenticatorConfiguredMethod(realm, user, session));
        }

        if (authenticationSession != null && authenticationSession.getClientNote(Constants.KC_ACTION_EXECUTING) != null) {
            attributes.put("isAppInitiatedAction", true);
        }
    }

    /**
     * Process FreeMarker template and prepare Response. Some fields are used for rendering also.
     * 
     * @param theme to be used (provided by <code>getTheme()</code>)
     * @param templateName name of the template to be rendered
     * @param locale to be used
     * @return Response object to be returned to the browser, never null
     */
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        try {
            String result = freeMarker.processTemplate(attributes, templateName, theme);
            javax.ws.rs.core.MediaType mediaType = contentType == null ? MediaType.TEXT_HTML_UTF_8_TYPE : contentType;
            Response.ResponseBuilder builder = Response.status(status == null ? Response.Status.OK : status).type(mediaType).language(locale).entity(result);
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

    public Response createLoginUsername(){
        return createResponse(LoginFormsPages.LOGIN_USERNAME);
    };

    public Response createLoginPassword(){
        return createResponse(LoginFormsPages.LOGIN_PASSWORD);
    };

    @Override
    public Response createPasswordReset() {
        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (loginHint != null && !loginHint.isEmpty()) {
            authenticationSession.setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, loginHint);
        }
        return createResponse(LoginFormsPages.LOGIN_RESET_PASSWORD);
    }

    @Override
    public Response createLoginTotp() {
        return createResponse(LoginFormsPages.LOGIN_TOTP);
    }

    @Override
    public Response createLoginWebAuthn() {
        return createResponse(LoginFormsPages.LOGIN_WEBAUTHN);
    }

    @Override
    public Response createRegistration() {
        String loginHint = authenticationSession.getClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        if (loginHint != null && !loginHint.isEmpty()) {
            this.formData = new MultivaluedHashMap<>();
            if(this.realm.isRegistrationEmailAsUsername()) {
                this.formData.putSingle("email", loginHint);
            } else {
                this.formData.putSingle("username", loginHint);
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

        if(isDynamicUserProfile()) {
            UpdateProfileContext userCtx = (UpdateProfileContext) attributes.get(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR);
            if(userCtx != null && userCtx.getUserProfileContext() == UserProfileContext.IDP_REVIEW)
                return createResponse(LoginFormsPages.IDP_REVIEW_USER_PROFILE);
            else
                return createResponse(LoginFormsPages.UPDATE_USER_PROFILE);
        } else {
            return createResponse(LoginFormsPages.LOGIN_UPDATE_PROFILE);
        }
    }

    @Override
    public Response createIdpLinkConfirmLinkPage() {
        return createResponse(LoginFormsPages.LOGIN_IDP_LINK_CONFIRM);
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

    protected void setMessage(MessageType type, String message, Object... parameters) {
        messageType = type;
        messages = new ArrayList<>();
        messages.add(new FormMessage(null, message, parameters));
    }

    protected String getFirstMessageUnformatted() {
        if (messages != null && !messages.isEmpty()) {
            return messages.get(0).getMessage();
        }
        return null;
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
        this.attributes.put(name, value);
        return this;
    }

    @Override
    public LoginFormsProvider setStatus(Response.Status status) {
        this.status = status;
        return this;
    }

    @Override
    public LoginFormsProvider setMediaType(javax.ws.rs.core.MediaType type) {
        this.contentType = type;
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

    public LoginFormsProvider setAuthContext(AuthenticationFlowContext context){
        this.context = context;
        return this;
    }

    @Override
    public void close() {
    }

}
