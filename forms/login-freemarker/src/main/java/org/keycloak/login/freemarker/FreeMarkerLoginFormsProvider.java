package org.keycloak.login.freemarker;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailProvider;
import org.keycloak.freemarker.BrowserSecurityHeaderSetup;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.login.LoginFormsPages;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.login.freemarker.model.ClientBean;
import org.keycloak.login.freemarker.model.CodeBean;
import org.keycloak.login.freemarker.model.LoginBean;
import org.keycloak.login.freemarker.model.MessageBean;
import org.keycloak.login.freemarker.model.OAuthGrantBean;
import org.keycloak.login.freemarker.model.ProfileBean;
import org.keycloak.login.freemarker.model.RealmBean;
import org.keycloak.login.freemarker.model.RegisterBean;
import org.keycloak.login.freemarker.model.SocialBean;
import org.keycloak.login.freemarker.model.TotpBean;
import org.keycloak.login.freemarker.model.UrlBean;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Urls;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginFormsProvider implements LoginFormsProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerLoginFormsProvider.class);

    private String message;
    private String accessCode;
    private Response.Status status = Response.Status.OK;
    private List<RoleModel> realmRolesRequested;
    private MultivaluedMap<String, RoleModel> resourceRolesRequested;
    private MultivaluedMap<String, String> queryParams;

    public static enum MessageType {SUCCESS, WARNING, ERROR}

    private MessageType messageType = MessageType.ERROR;

    private MultivaluedMap<String, String> formData;

    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;
    private RealmModel realm;

    private UserModel user;

    private ClientModel client;

    private UriInfo uriInfo;

    public FreeMarkerLoginFormsProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public LoginFormsProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public LoginFormsProvider setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    public Response createResponse(UserModel.RequiredAction action) {
        String actionMessage;
        LoginFormsPages page;

        switch (action) {
            case CONFIGURE_TOTP:
                actionMessage = Messages.ACTION_WARN_TOTP;
                page = LoginFormsPages.LOGIN_CONFIG_TOTP;
                break;
            case UPDATE_PROFILE:
                actionMessage = Messages.ACTION_WARN_PROFILE;
                page = LoginFormsPages.LOGIN_UPDATE_PROFILE;
                break;
            case UPDATE_PASSWORD:
                actionMessage = Messages.ACTION_WARN_PASSWD;
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
                    return setError("emailSendError").createErrorPage();
                }

                actionMessage = Messages.ACTION_WARN_EMAIL;
                page = LoginFormsPages.LOGIN_VERIFY_EMAIL;
                break;
            default:
                return Response.serverError().build();
        }

        if (message == null) {
            setWarning(actionMessage);
        }

        return createResponse(page);
    }

    private Response createResponse(LoginFormsPages page) {
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

        Map<String, Object> attributes = new HashMap<String, Object>();

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

        Properties messages;
        try {
            messages = theme.getMessages();
            attributes.put("rb", messages);
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messages = new Properties();
        }

        if (message != null) {
            attributes.put("message", new MessageBean(messages.containsKey(message) ? messages.getProperty(message) : message, messageType));
        }
        if (page == LoginFormsPages.OAUTH_GRANT) {
            // for some reason Resteasy 2.3.7 doesn't like query params and form params with the same name and will null out the code form param
            uriBuilder.replaceQuery(null);
        }
        URI baseUri = uriBuilder.build();

        if (realm != null) {
            attributes.put("realm", new RealmBean(realm));
            attributes.put("social", new SocialBean(realm, baseUri));
            attributes.put("url", new UrlBean(realm, theme, baseUri));
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
                attributes.put("oauth", new OAuthGrantBean(accessCode, client, realmRolesRequested, resourceRolesRequested));
                break;
            case CODE:
                attributes.put(OAuth2Constants.CODE, new CodeBean(accessCode, messageType == MessageType.ERROR ? message : null));
                break;
        }

        try {
            String result = freeMarker.processTemplate(attributes, Templates.getTemplate(page), theme);
            Response.ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_HTML).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
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

    public Response createErrorPage() {
        setStatus(Response.Status.INTERNAL_SERVER_ERROR);
        return createResponse(LoginFormsPages.ERROR);
    }

    public Response createOAuthGrant() {
        return createResponse(LoginFormsPages.OAUTH_GRANT);
    }

    @Override
    public Response createCode() {
        return createResponse(LoginFormsPages.CODE);
    }

    public FreeMarkerLoginFormsProvider setError(String message) {
        this.message = message;
        this.messageType = MessageType.ERROR;
        return this;
    }

    public FreeMarkerLoginFormsProvider setSuccess(String message) {
        this.message = message;
        this.messageType = MessageType.SUCCESS;
        return this;
    }

    public FreeMarkerLoginFormsProvider setWarning(String message) {
        this.message = message;
        this.messageType = MessageType.WARNING;
        return this;
    }

    public FreeMarkerLoginFormsProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    public FreeMarkerLoginFormsProvider setClient(ClientModel client) {
        this.client = client;
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
    public LoginFormsProvider setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String, RoleModel> resourceRolesRequested) {
        this.realmRolesRequested = realmRolesRequested;
        this.resourceRolesRequested = resourceRolesRequested;
        return this;
    }

    @Override
    public LoginFormsProvider setStatus(Response.Status status) {
        this.status = status;
        return this;
    }

    @Override
    public LoginFormsProvider setQueryParams(MultivaluedMap<String, String> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    @Override
    public void close() {
    }

}
