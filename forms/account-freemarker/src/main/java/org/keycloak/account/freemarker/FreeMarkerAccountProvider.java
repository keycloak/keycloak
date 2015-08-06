package org.keycloak.account.freemarker;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.account.AccountPages;
import org.keycloak.account.AccountProvider;
import org.keycloak.account.freemarker.model.ApplicationsBean;
import org.keycloak.account.freemarker.model.AccountBean;
import org.keycloak.account.freemarker.model.AccountFederatedIdentityBean;
import org.keycloak.account.freemarker.model.FeaturesBean;
import org.keycloak.account.freemarker.model.LogBean;
import org.keycloak.account.freemarker.model.PasswordBean;
import org.keycloak.account.freemarker.model.RealmBean;
import org.keycloak.account.freemarker.model.ReferrerBean;
import org.keycloak.account.freemarker.model.SessionsBean;
import org.keycloak.account.freemarker.model.TotpBean;
import org.keycloak.account.freemarker.model.UrlBean;
import org.keycloak.events.Event;
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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.Urls;

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
    private boolean identityProviderEnabled;
    private boolean eventsEnabled;
    private boolean passwordUpdateSupported;
    private boolean passwordSet;
    private KeycloakSession session;
    private FreeMarkerUtil freeMarker;
    private HttpHeaders headers;

    private UriInfo uriInfo;

    private List<FormMessage> messages = null;
    private MessageType messageType = MessageType.ERROR;

    public FreeMarkerAccountProvider(KeycloakSession session, FreeMarkerUtil freeMarker) {
        this.session = session;
        this.freeMarker = freeMarker;
    }

    public AccountProvider setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public AccountProvider setHttpHeaders(HttpHeaders httpHeaders) {
        this.headers = httpHeaders;
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

        Locale locale = LocaleHelper.getLocale(realm, user, uriInfo, headers);
        Properties messagesBundle;
        try {
            messagesBundle = theme.getMessages(locale);
            attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        } catch (IOException e) {
            logger.warn("Failed to load messages", e);
            messagesBundle = new Properties();
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

        if (referrer != null) {
            attributes.put("referrer", new ReferrerBean(referrer));
        }

        if(realm != null){
            attributes.put("realm", new RealmBean(realm));
        }

        attributes.put("url", new UrlBean(realm, theme, baseUri, baseQueryUri, uriInfo.getRequestUri(), stateChecker));

        if (realm.isInternationalizationEnabled()) {
            UriBuilder b;
            switch (page) {
                default:
                    b = UriBuilder.fromUri(baseQueryUri).path(uriInfo.getPath());
                    break;
            }
            attributes.put("locale", new LocaleBean(realm, locale, b, messagesBundle));
        }

        attributes.put("features", new FeaturesBean(identityProviderEnabled, eventsEnabled, passwordUpdateSupported));
        attributes.put("account", new AccountBean(user, profileFormData));

        switch (page) {
            case TOTP:
                attributes.put("totp", new TotpBean(session, realm, user, baseUri));
                break;
            case FEDERATED_IDENTITY:
                attributes.put("federatedIdentity", new AccountFederatedIdentityBean(session, realm, user, uriInfo.getBaseUri(), stateChecker));
                break;
            case LOG:
                attributes.put("log", new LogBean(events));
                break;
            case SESSIONS:
                attributes.put("sessions", new SessionsBean(realm, sessions));
                break;
            case APPLICATIONS:
                attributes.put("applications", new ApplicationsBean(realm, user));
                attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));
                break;
            case PASSWORD:
                attributes.put("password", new PasswordBean(passwordSet));
        }

        try {
            String result = freeMarker.processTemplate(attributes, Templates.getTemplate(page), theme);
            Response.ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_HTML).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            LocaleHelper.updateLocaleCookie(builder, locale, realm, uriInfo, Urls.localeCookiePath(baseUri,realm.getName()));
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
    
    protected void setMessage(MessageType type, String message, Object... parameters) {
        messageType = type;
        messages = new ArrayList<>();
        messages.add(new FormMessage(null, message, parameters));
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
    public AccountProvider setErrors(List<FormMessage> messages) {
        this.messageType = MessageType.ERROR;
        this.messages = new ArrayList<>(messages);
        return this;
    }


    @Override
    public AccountProvider setError(String message, Object ... parameters) {
        setMessage(MessageType.ERROR, message, parameters);
        return this;
    }

    @Override
    public AccountProvider setSuccess(String message, Object ... parameters) {
        setMessage(MessageType.SUCCESS, message, parameters);
        return this;
    }

    @Override
    public AccountProvider setWarning(String message, Object ... parameters) {
        setMessage(MessageType.WARNING, message, parameters);
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
    public AccountProvider setFeatures(boolean identityProviderEnabled, boolean eventsEnabled, boolean passwordUpdateSupported) {
        this.identityProviderEnabled = identityProviderEnabled;
        this.eventsEnabled = eventsEnabled;
        this.passwordUpdateSupported = passwordUpdateSupported;
        return this;
    }

    @Override
    public void close() {
    }

}
