package org.keycloak.account.freemarker;

import org.jboss.logging.Logger;
import org.keycloak.account.AccountPages;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.freemarker.model.AccountBean;
import org.keycloak.account.freemarker.model.AccountSocialBean;
import org.keycloak.account.freemarker.model.FeaturesBean;
import org.keycloak.account.freemarker.model.LogBean;
import org.keycloak.account.freemarker.model.MessageBean;
import org.keycloak.account.freemarker.model.ReferrerBean;
import org.keycloak.account.freemarker.model.SessionsBean;
import org.keycloak.account.freemarker.model.TotpBean;
import org.keycloak.account.freemarker.model.UrlBean;
import org.keycloak.audit.Event;
import org.keycloak.freemarker.ExtendingThemeManager;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import javax.ws.rs.core.MediaType;
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
public class FreeMarkerAccountProvider implements AccountProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerAccountProvider.class);

    private UserModel user;
    private Response.Status status = Response.Status.OK;
    private RealmModel realm;
    private String[] referrer;
    private List<Event> events;
    private List<UserSessionModel> sessions;
    private boolean social;
    private boolean audit;
    private boolean passwordUpdateSupported;
    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;

    public static enum MessageType {SUCCESS, WARNING, ERROR}

    private UriInfo uriInfo;

    private String message;
    private MessageType messageType;

    public FreeMarkerAccountProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public AccountProvider setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public Response createResponse(AccountPages page) {
        Map<String, Object> attributes = new HashMap<String, Object>();

        ExtendingThemeManager themeManager = new ExtendingThemeManager(session);
        Theme theme;
        try {
            theme = themeManager.createTheme(realm.getAccountTheme(), Theme.Type.ACCOUNT);
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

        URI baseUri = uriInfo.getBaseUri();
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        for (Map.Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
           baseUriBuilder.queryParam(e.getKey(), e.getValue().toArray());
        }
        URI baseQueryUri = baseUriBuilder.build();

        if (message != null) {
            attributes.put("message", new MessageBean(messages.containsKey(message) ? messages.getProperty(message) : message, messageType));
        }

        if (referrer != null) {
            attributes.put("referrer", new ReferrerBean(referrer));
        }

        attributes.put("url", new UrlBean(realm, theme, baseUri, baseQueryUri, uriInfo.getRequestUri()));

        attributes.put("features", new FeaturesBean(social, audit, passwordUpdateSupported));

        switch (page) {
            case ACCOUNT:
                attributes.put("account", new AccountBean(user));
                break;
            case TOTP:
                attributes.put("totp", new TotpBean(user, baseUri));
                break;
            case SOCIAL:
                attributes.put("social", new AccountSocialBean(session, realm, user, uriInfo.getBaseUri()));
                break;
            case LOG:
                attributes.put("log", new LogBean(events));
                break;
            case SESSIONS:
                attributes.put("sessions", new SessionsBean(realm, sessions));
                break;
        }

        try {
            String result = freeMarker.processTemplate(attributes, Templates.getTemplate(page), theme);
            return Response.status(status).type(MediaType.TEXT_HTML).entity(result).build();
        } catch (FreeMarkerException e) {
            logger.error("Failed to process template", e);
            return Response.serverError().build();
        }
    }

    @Override
    public AccountProvider setError(String message) {
        this.message = message;
        this.messageType = MessageType.ERROR;
        return this;
    }

    @Override
    public AccountProvider setSuccess(String message) {
        this.message = message;
        this.messageType = MessageType.SUCCESS;
        return this;
    }

    @Override
    public AccountProvider setWarning(String message) {
        this.message = message;
        this.messageType = MessageType.WARNING;
        return this;
    }

    @Override
    public AccountProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public AccountProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public AccountProvider setStatus(Response.Status status) {
        this.status = status;
        return this;
    }

    @Override
    public AccountProvider setReferrer(String[] referrer) {
        this.referrer = referrer;
        return this;
    }

    @Override
    public AccountProvider setEvents(List<Event> events) {
        this.events = events;
        return this;
    }

    @Override
    public AccountProvider setSessions(List<UserSessionModel> sessions) {
        this.sessions = sessions;
        return this;
    }

    @Override
    public AccountProvider setFeatures(boolean social, boolean audit, boolean passwordUpdateSupported) {
        this.social = social;
        this.audit = audit;
        this.passwordUpdateSupported = passwordUpdateSupported;
        return this;
    }

    @Override
    public void close() {
    }

}
