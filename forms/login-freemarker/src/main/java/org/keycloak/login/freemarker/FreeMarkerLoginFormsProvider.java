package org.keycloak.login.freemarker;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.freemarker.BrowserSecurityHeaderSetup;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.LocaleHelper;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.freemarker.beans.AdvancedMessageFormatterMethod;
import org.keycloak.freemarker.beans.LocaleBean;
import org.keycloak.freemarker.beans.MessageBean;
import org.keycloak.freemarker.beans.MessageFormatterMethod;
import org.keycloak.freemarker.beans.MessageType;
import org.keycloak.freemarker.beans.MessagesPerFieldBean;
import org.keycloak.login.LoginFormsPages;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.login.freemarker.model.ClientBean;
import org.keycloak.login.freemarker.model.CodeBean;
import org.keycloak.login.freemarker.model.IdentityProviderBean;
import org.keycloak.login.freemarker.model.LoginBean;
import org.keycloak.login.freemarker.model.OAuthGrantBean;
import org.keycloak.login.freemarker.model.ProfileBean;
import org.keycloak.login.freemarker.model.RealmBean;
import org.keycloak.login.freemarker.model.RegisterBean;
import org.keycloak.login.freemarker.model.RequiredActionUrlFormatterMethod;
import org.keycloak.login.freemarker.model.TotpBean;
import org.keycloak.login.freemarker.model.UrlBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MediaType;
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
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProvider implements LoginFormsProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerLoginFormsProvider.class);

    private String accessCode;
    private Response.Status status;
    private List<RoleModel> realmRolesRequested;
    private MultivaluedMap<String, RoleModel> resourceRolesRequested;
    private List<ProtocolMapperModel> protocolMappersRequested;
    private MultivaluedMap<String, String> queryParams;
    private Map<String, String> httpResponseHeaders = new HashMap<String, String>();
    private String accessRequestMessage;
    private URI actionUri;

    private List<FormMessage> messages = null;
    private MessageType messageType = MessageType.ERROR;

    private MultivaluedMap<String, String> formData;

    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;

    private UserModel user;

    private ClientSessionModel clientSession;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public FreeMarkerLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public Response createResponse(UserModel.RequiredAction action) {
        RealmModel realm = session.getContext().getRealm();
        UriInfo uriInfo = session.getContext().getUri();

        String actionMessage;
        LoginFormsPages page;

        switch (action) {
            case CONFIGURE_TOTP:
                actionMessage = Messages.CONFIGURE_TOTP;
                page = LoginFormsPages.LOGIN_CONFIG_TOTP;
                break;
            case UPDATE_PROFILE:
                actionMessage = Messages.UPDATE_PROFILE;
                page = LoginFormsPages.LOGIN_UPDATE_PROFILE;
                break;
            case UPDATE_PASSWORD:
                actionMessage = Messages.UPDATE_PASSWORD;
                page = LoginFormsPages.LOGIN_UPDATE_PASSWORD;
                break;
            case VERIFY_EMAIL:
                try {
                    UriBuilder builder = Urls.loginActionEmailVerificationBuilder(uriInfo.getBaseUri());
                    builder.queryParam("key", accessCode);

                    String link = builder.build(realm.getName()).toString();
                    long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

                    session.getProvider(EmailProvider.class).setRealm(realm).setUser(user).sendVerifyEmail(link, expiration);
                } catch (EmailException e) {
                    logger.error("Failed to send verification email", e);
                    return setError(Messages.EMAIL_SENT_ERROR).createErrorPage();
                }

                actionMessage = Messages.VERIFY_EMAIL;
                page = LoginFormsPages.LOGIN_VERIFY_EMAIL;
                break;
            default:
                return Response.serverError().build();
        }

        if (messages == null) {
            setMessage(MessageType.WARNING, actionMessage);
        }

        return createResponse(page);
    }

    private Response createResponse(LoginFormsPages page) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();
        UriInfo uriInfo = session.getContext().getUri();

        MultivaluedMap<String, String> queryParameterMap = queryParams != null ? queryParams : new MultivaluedMapImpl<String, String>();

        String requestURI = uriInfo.getBaseUri().getPath();
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);

        for (String k : queryParameterMap.keySet()) {

            Object[] objects = queryParameterMap.get(k).toArray();
            if (objects.length == 1 && objects[0] == null) continue; //
            uriBuilder.replaceQueryParam(k, objects);
        }

        if (accessCode != null) {
            uriBuilder.replaceQueryParam(OAuth2Constants.CODE, accessCode);
        }

        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        Theme theme;
        try {
            theme = themeProvider.getTheme(realm.getLoginTheme(), Theme.Type.LOGIN);
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            return Response.serverError().build();
        }

        try {
            attributes.put("properties", theme.getProperties());
        } catch (IOException e) {
            logger.warn("Failed to load properties", e);
        }

        Properties messagesBundle;
        Locale locale = LocaleHelper.getLocale(realm, user, uriInfo, session.getContext().getRequestHeaders());
        try {
            messagesBundle = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }

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
        }
        attributes.put("messagesPerField", messagesPerField);

        if (page == LoginFormsPages.OAUTH_GRANT) {
            // for some reason Resteasy 2.3.7 doesn't like query params and form params with the same name and will null out the code form param
            uriBuilder.replaceQuery(null);
        }
        URI baseUri = uriBuilder.build();
        attributes.put("requiredActionUrl", new RequiredActionUrlFormatterMethod(realm, baseUri));
        if (realm != null && user != null && session != null) {
            attributes.put("authenticatorConfigured", new AuthenticatorConfiguredMethod(realm, user, session));
        }

        if (realm != null) {
            attributes.put("realm", new RealmBean(realm));
            attributes.put("social", new IdentityProviderBean(realm, baseUri, uriInfo));
            attributes.put("url", new UrlBean(realm, theme, baseUri, this.actionUri));

            if (realm.isInternationalizationEnabled()) {
                UriBuilder b;
                switch (page) {
                    case LOGIN:
                        b = UriBuilder.fromUri(Urls.realmLoginPage(baseUri, realm.getName()));
                        break;
                    case REGISTER:
                        b = UriBuilder.fromUri(Urls.realmRegisterPage(baseUri, realm.getName()));
                        break;
                    default:
                        b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
                        break;
                }
                attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
            }
        }

        if (client != null) {
            attributes.put("client", new ClientBean(client));
        }

        attributes.put("login", new LoginBean(formData));

        switch (page) {
            case LOGIN_CONFIG_TOTP:
                attributes.put("totp", new TotpBean(realm, user, baseUri));
                break;
            case LOGIN_UPDATE_PROFILE:
                attributes.put("user", new ProfileBean(user));
                break;
            case REGISTER:
                attributes.put("register", new RegisterBean(formData));
                break;
            case OAUTH_GRANT:
                attributes.put("oauth", new OAuthGrantBean(accessCode, clientSession, client, realmRolesRequested, resourceRolesRequested, protocolMappersRequested, this.accessRequestMessage));
                attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));
                break;
            case CODE:
                attributes.put(OAuth2Constants.CODE, new CodeBean(accessCode, messageType == MessageType.ERROR ? getFirstMessageUnformatted() : null));
                break;
        }

        if (status == null) {
            status = Response.Status.OK;
        }

        try {
            String result = freeMarker.processTemplate(attributes, Templates.getTemplate(page), theme);
            Response.ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_HTML).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            for (Map.Entry<String, String> entry : httpResponseHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            LocaleHelper.updateLocaleCookie(builder, locale, realm, uriInfo, Urls.localeCookiePath(baseUri, realm.getName()));
            return builder.build();
        } catch (FreeMarkerException e) {
            logger.error("Failed to process template", e);
            return Response.serverError().build();
        }
    }

    @Override
    public Response createForm(String form, Map<String, Object> extraAttributes) {

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.getContext().getClient();
        UriInfo uriInfo = session.getContext().getUri();

        MultivaluedMap<String, String> queryParameterMap = queryParams != null ? queryParams : new MultivaluedMapImpl<String, String>();

        String requestURI = uriInfo.getBaseUri().getPath();
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);

        for (String k : queryParameterMap.keySet()) {

            Object[] objects = queryParameterMap.get(k).toArray();
            if (objects.length == 1 && objects[0] == null) continue; //
            uriBuilder.replaceQueryParam(k, objects);
        }
        if (accessCode != null) {
            uriBuilder.replaceQueryParam(OAuth2Constants.CODE, accessCode);
        }
        URI baseUri = uriBuilder.build();

        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        Theme theme;
        try {
            theme = themeProvider.getTheme(realm.getLoginTheme(), Theme.Type.LOGIN);
        } catch (IOException e) {
            logger.error("Failed to create theme", e);
            return Response.serverError().build();
        }

        try {
            attributes.put("properties", theme.getProperties());
        } catch (IOException e) {
            logger.warn("Failed to load properties", e);
        }
        if (client != null) {
            attributes.put("client", new ClientBean(client));
        }

        Properties messagesBundle;
        Locale locale = LocaleHelper.getLocale(realm, user, uriInfo, session.getContext().getRequestHeaders());
        try {
            messagesBundle = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
        }

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
        }
        attributes.put("messagesPerField", messagesPerField);

        if (status == null) {
            status = Response.Status.OK;
        }

        if (realm != null) {
            attributes.put("realm", new RealmBean(realm));
            attributes.put("social", new IdentityProviderBean(realm, baseUri, uriInfo));
            attributes.put("url", new UrlBean(realm, theme, baseUri, this.actionUri));
            attributes.put("requiredActionUrl", new RequiredActionUrlFormatterMethod(realm, baseUri));

            if (realm.isInternationalizationEnabled()) {
                UriBuilder b = UriBuilder.fromUri(baseUri).path(uriInfo.getPath());
                attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
            }
        }
        if (realm != null && user != null && session != null) {
            attributes.put("authenticatorConfigured", new AuthenticatorConfiguredMethod(realm, user, session));
        }
        try {
            String result = freeMarker.processTemplate(attributes, form, theme);
            Response.ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_HTML).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            for (Map.Entry<String, String> entry : httpResponseHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            LocaleHelper.updateLocaleCookie(builder, locale, realm, uriInfo, Urls.localeCookiePath(baseUri, realm.getName()));
            return builder.build();
        } catch (FreeMarkerException e) {
            logger.error("Failed to process template", e);
            return Response.serverError().build();
        }
    }


    public Response createLogin() {
        return createResponse(LoginFormsPages.LOGIN);
    }

    public Response createPasswordReset() {
        return createResponse(LoginFormsPages.LOGIN_RESET_PASSWORD);
    }

    public Response createLoginTotp() {
        return createResponse(LoginFormsPages.LOGIN_TOTP);
    }

    public Response createRegistration() {
        return createResponse(LoginFormsPages.REGISTER);
    }

    public Response createInfoPage() {
        return createResponse(LoginFormsPages.INFO);
    }

    public Response createErrorPage() {
        if (status == null) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        return createResponse(LoginFormsPages.ERROR);
    }


    public Response createOAuthGrant(ClientSessionModel clientSession) {
        this.clientSession = clientSession;
        return createResponse(LoginFormsPages.OAUTH_GRANT);
    }

    @Override
    public Response createCode() {
        return createResponse(LoginFormsPages.CODE);
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
        if (messages == null) return this;
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
    public FreeMarkerLoginFormsProvider setSuccess(String message, Object... parameters) {
        setMessage(MessageType.SUCCESS, message, parameters);
        return this;
    }

    public FreeMarkerLoginFormsProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

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
    public LoginFormsProvider setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String, RoleModel> resourceRolesRequested, List<ProtocolMapperModel> protocolMappersRequested) {
        this.realmRolesRequested = realmRolesRequested;
        this.resourceRolesRequested = resourceRolesRequested;
        this.protocolMappersRequested = protocolMappersRequested;
        return this;
    }

    @Override
    public LoginFormsProvider setAccessRequest(String accessRequestMessage) {
        this.accessRequestMessage = accessRequestMessage;
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
    public LoginFormsProvider setActionUri(URI actionUri) {
        this.actionUri = actionUri;
        return this;
    }

    @Override
    public LoginFormsProvider setResponseHeader(String headerName, String headerValue) {
        this.httpResponseHeaders.put(headerName, headerValue);
        return this;
    }

    @Override
    public void close() {
    }

}
