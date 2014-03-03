package org.keycloak.login.freemarker;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeLoader;
import org.keycloak.login.LoginForms;
import org.keycloak.login.LoginFormsPages;
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
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.email.EmailException;
import org.keycloak.services.email.EmailSender;
import org.keycloak.services.messages.Messages;

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

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FreeMarkerLoginForms implements LoginForms {

    private static final Logger logger = Logger.getLogger(FreeMarkerLoginForms.class);

    private String message;
    private String accessCodeId;
    private String accessCode;
    private Response.Status status = Response.Status.OK;
    private List<RoleModel> realmRolesRequested;
    private MultivaluedMap<String, RoleModel> resourceRolesRequested;

    public static enum MessageType {SUCCESS, WARNING, ERROR}

    private MessageType messageType = MessageType.ERROR;

    private MultivaluedMap<String, String> formData;

    private RealmModel realm;

    // TODO Remove
    private HttpRequest request;

    private UserModel user;

    private ClientModel client;

    private UriInfo uriInfo;

    FreeMarkerLoginForms(RealmModel realm, org.jboss.resteasy.spi.HttpRequest request, UriInfo uriInfo) {
        this.realm = realm;
        this.request = request;
        this.uriInfo = uriInfo;
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
                    new EmailSender(realm.getSmtpConfig()).sendEmailVerification(user, realm, accessCodeId, uriInfo);
                } catch (EmailException e) {
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
        MultivaluedMap<String, String> queryParameterMap = uriInfo.getQueryParameters();

        String requestURI = uriInfo.getBaseUri().getPath();
        UriBuilder uriBuilder = UriBuilder.fromUri(requestURI);

        for (String k : queryParameterMap.keySet()) {
            uriBuilder.replaceQueryParam(k, queryParameterMap.get(k).toArray());
        }

        if (accessCode != null) {
            uriBuilder.replaceQueryParam("code", accessCode);
        }

        Map<String, Object> attributes = new HashMap<String, Object>();

        Theme theme;
        try {
            theme = ThemeLoader.createTheme(realm.getLoginTheme(), Theme.Type.LOGIN);
        } catch (FreeMarkerException e) {
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

        URI baseUri = uriBuilder.build();

        if (realm != null) {
            attributes.put("realm", new RealmBean(realm));
            attributes.put("social", new SocialBean(realm, baseUri));
            attributes.put("url", new UrlBean(realm, theme, baseUri));
        }

        attributes.put("login", new LoginBean(formData));

        switch (page) {
            case LOGIN_CONFIG_TOTP:
                attributes.put("totp", new TotpBean(user, baseUri));
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
                attributes.put("code", new CodeBean(accessCode, messageType == MessageType.ERROR ? message : null));
                break;
        }

        try {
            String result = FreeMarkerUtil.processTemplate(attributes, Templates.getTemplate(page), theme);
            return Response.status(status).type(MediaType.TEXT_HTML).entity(result).build();
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

    public FreeMarkerLoginForms setError(String message) {
        this.message = message;
        this.messageType = MessageType.ERROR;
        return this;
    }

    public FreeMarkerLoginForms setSuccess(String message) {
        this.message = message;
        this.messageType = MessageType.SUCCESS;
        return this;
    }

    public FreeMarkerLoginForms setWarning(String message) {
        this.message = message;
        this.messageType = MessageType.WARNING;
        return this;
    }

    public FreeMarkerLoginForms setUser(UserModel user) {
        this.user = user;
        return this;
    }

    public FreeMarkerLoginForms setClient(ClientModel client) {
        this.client = client;
        return this;
    }

    public FreeMarkerLoginForms setFormData(MultivaluedMap<String, String> formData) {
        this.formData = formData;
        return this;
    }

    @Override
    public LoginForms setAccessCode(String accessCodeId, String accessCode) {
        this.accessCodeId = accessCodeId;
        this.accessCode = accessCode;
        return this;
    }

    @Override
    public LoginForms setAccessRequest(List<RoleModel> realmRolesRequested, MultivaluedMap<String, RoleModel> resourceRolesRequested) {
        this.realmRolesRequested = realmRolesRequested;
        this.resourceRolesRequested = resourceRolesRequested;
        return this;
    }

    @Override
    public LoginForms setStatus(Response.Status status) {
        this.status = status;
        return this;
    }

}
