package org.keycloak.account.freemarker;

import org.jboss.logging.Logger;
import org.keycloak.account.AccountPages;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.freemarker.model.AccountBean;
import org.keycloak.account.freemarker.model.AccountSocialBean;
import org.keycloak.account.freemarker.model.FeaturesBean;
import org.keycloak.account.freemarker.model.LogBean;
import org.keycloak.account.freemarker.model.MessageBean;
import org.keycloak.account.freemarker.model.PasswordBean;
import org.keycloak.account.freemarker.model.ReferrerBean;
import org.keycloak.account.freemarker.model.SessionsBean;
import org.keycloak.account.freemarker.model.TotpBean;
import org.keycloak.account.freemarker.model.UrlBean;
import org.keycloak.events.Event;
import org.keycloak.freemarker.BrowserSecurityHeaderSetup;
import org.keycloak.freemarker.FreeMarkerException;
import org.keycloak.freemarker.FreeMarkerUtil;
import org.keycloak.freemarker.Theme;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

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
public class FreeMarkerAccountProvider implements AccountProvider {

    private static final Logger logger = Logger.getLogger(FreeMarkerAccountProvider.class);

    private UserModel user;
    private MultivaluedMap<String, String> profileFormData;
    private Response.Status status = Response.Status.OK;
    private RealmModel realm;
    private String[] referrer;
    private List<Event> events;
    private String stateChecker;
    private List<UserSessionModel> sessions;
    private boolean socialEnabled;
    private boolean eventsEnabled;
    private boolean passwordUpdateSupported;
    private boolean passwordSet;
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

        ThemeProvider themeProvider = session.getProvider(ThemeProvider.class, "extending");
        Theme theme;
        try {
            theme = themeProvider.getTheme(realm.getAccountTheme(), Theme.Type.ACCOUNT);
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

        if (stateChecker != null) {
            attributes.put("stateChecker", stateChecker);
        }

        if (message != null) {
            attributes.put("message", new MessageBean(messages.containsKey(message) ? messages.getProperty(message) : message, messageType));
        }

        if (referrer != null) {
            attributes.put("referrer", new ReferrerBean(referrer));
        }

        attributes.put("url", new UrlBean(realm, theme, baseUri, baseQueryUri, uriInfo.getRequestUri(), stateChecker));

        attributes.put("features", new FeaturesBean(socialEnabled, eventsEnabled, passwordUpdateSupported));

        switch (page) {
            case ACCOUNT:
                attributes.put("account", new AccountBean(user, profileFormData));
                break;
            case TOTP:
                attributes.put("totp", new TotpBean(realm, user, baseUri));
                break;
            case SOCIAL:
                attributes.put("social", new AccountSocialBean(session, realm, user, uriInfo.getBaseUri(), stateChecker));
                break;
            case LOG:
                attributes.put("log", new LogBean(events));
                break;
            case SESSIONS:
                attributes.put("sessions", new SessionsBean(realm, sessions));
                break;
            case PASSWORD:
                attributes.put("password", new PasswordBean(passwordSet));
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

    public AccountProvider setPasswordSet(boolean passwordSet) {
        this.passwordSet = passwordSet;
        return this;
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
    public AccountProvider setProfileFormData(MultivaluedMap<String, String> formData) {
        this.profileFormData = formData;
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
    public AccountProvider setStateChecker(String stateChecker) {
        this.stateChecker = stateChecker;
        return this;
    }

    @Override
    public AccountProvider setFeatures(boolean socialEnabled, boolean eventsEnabled, boolean passwordUpdateSupported) {
        this.socialEnabled = socialEnabled;
        this.eventsEnabled = eventsEnabled;
        this.passwordUpdateSupported = passwordUpdateSupported;
        return this;
    }

    @Override
    public void close() {
    }

}
