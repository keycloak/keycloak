package org.keycloak.services.error;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.Config;
import org.keycloak.OAuthErrorException;
import org.keycloak.forms.login.MessageType;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.AdvancedMessageFormatterMethod;
import org.keycloak.theme.beans.LocaleBean;
import org.keycloak.theme.beans.MessageBean;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.theme.freemarker.FreeMarkerProvider;
import org.keycloak.utils.KeycloakSessionUtil;
import org.keycloak.utils.MediaType;
import org.keycloak.utils.MediaTypeMatcher;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.jboss.logging.Logger;

import static org.keycloak.services.resources.KeycloakApplication.getSessionFactory;

@Provider
public class KeycloakErrorHandler implements ExceptionMapper<Throwable> {

    private static final Logger logger = Logger.getLogger(KeycloakErrorHandler.class);

    private static final Pattern realmNamePattern = Pattern.compile(".*/realms/([^/]+).*");

    public static final String UNCAUGHT_SERVER_ERROR_TEXT = "Uncaught server error";
    public static final String ERROR_RESPONSE_TEXT = "Error response {0}";

    @Override
    public Response toResponse(Throwable throwable) {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();

        if (session == null) {
            // errors might be thrown when handling errors from JAX-RS before the session is available
            return KeycloakModelUtils.runJobInTransactionWithResult(getSessionFactory(),
                    new KeycloakSessionTaskWithResult<Response>() {
                        @Override
                        public Response run(KeycloakSession session) {
                            return getResponse(session, throwable);
                        }
                    });
        }

        return getResponse(session, throwable);
    }

    public static Response getResponse(KeycloakSession session, Throwable throwable) {
        KeycloakTransaction tx = session.getTransactionManager();
        tx.setRollbackOnly();

        Response.Status responseStatus = getResponseStatus(throwable);
        boolean isServerError = responseStatus.getFamily().equals(Response.Status.Family.SERVER_ERROR);

        if (isServerError) {
            logger.error(UNCAUGHT_SERVER_ERROR_TEXT, throwable);
        } else {
            logger.debugv(throwable, ERROR_RESPONSE_TEXT, responseStatus);
        }

        HttpHeaders headers = session.getContext().getRequestHeaders();

        if (!MediaTypeMatcher.isHtmlRequest(headers)) {
            OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation();

            error.setError(getErrorCode(throwable));
            if (throwable.getCause() instanceof ModelException) {
                error.setErrorDescription(throwable.getMessage());
            } if (throwable instanceof ModelDuplicateException) {
                error.setErrorDescription(throwable.getMessage());
            } else if (throwable instanceof JsonProcessingException || throwable.getCause() instanceof JsonProcessingException) {
                error.setErrorDescription("Cannot parse the JSON");
            } else if (isServerError) {
                error.setErrorDescription("For more on this error consult the server log.");
            } else if (throwable instanceof ValidationException) {
                error.setErrorDescription(throwable.getMessage());
            }

            return Response.status(responseStatus)
                    .header(HttpHeaders.CONTENT_TYPE, jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE.toString())
                    .entity(error)
                    .build();
        }

        try {
            RealmModel realm = resolveRealm(session);

            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);

            Locale locale = session.getContext().resolveLocale(null);

            FreeMarkerProvider freeMarker = session.getProvider(FreeMarkerProvider.class);
            Map<String, Object> attributes = initAttributes(session, realm, theme, locale, responseStatus);

            String templateName = "error.ftl";

            String content = freeMarker.processTemplate(attributes, templateName, theme);
            return Response.status(responseStatus).type(MediaType.TEXT_HTML_UTF_8_TYPE).entity(content).build();
        } catch (Throwable t) {
            logger.error("Failed to create error page", t);
            return Response.serverError().build();
        }
    }

    private static Response.Status getResponseStatus(Throwable throwable) {
        if (throwable instanceof WebApplicationException ex) {
            return Response.Status.fromStatusCode(ex.getResponse().getStatus());
        }

        if (throwable instanceof JsonProcessingException || throwable instanceof ModelValidationException) {
            return Response.Status.BAD_REQUEST;
        }

        if (throwable instanceof ModelIllegalStateException) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }

        if (throwable instanceof ModelDuplicateException) {
            return Response.Status.CONFLICT;
        }

        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    private static String getErrorCode(Throwable throwable) {
        Throwable cause = throwable.getCause();

        if (cause instanceof JsonParseException) {
            return OAuthErrorException.INVALID_REQUEST;
        }

        if (cause instanceof ModelDuplicateException || throwable instanceof ModelDuplicateException) {
            return "conflict";
        }

        if (throwable instanceof WebApplicationException && throwable.getMessage() != null) {
            return throwable.getMessage();
        }

        return "unknown_error";
    }

    private static RealmModel resolveRealm(KeycloakSession session) {
        String path = session.getContext().getUri().getPath();
        Matcher m = realmNamePattern.matcher(path);
        String realmName;
        if(m.matches()) {
            realmName = m.group(1);
        } else {
            realmName = Config.getAdminRealm();
        }

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            realm = realmManager.getRealmByName(Config.getAdminRealm());
        }

        session.getContext().setRealm(realm);

        return realm;
    }

    private static Map<String, Object> initAttributes(KeycloakSession session, RealmModel realm, Theme theme, Locale locale, Response.Status responseStatus) throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        Properties messagesBundle = theme.getEnhancedMessages(realm, locale);

        final var localeBean =  new LocaleBean(realm, locale, session.getContext().getUri().getRequestUriBuilder(), messagesBundle);
        final var lang = realm.isInternationalizationEnabled() ? localeBean.getCurrentLanguageTag() : Locale.ENGLISH.toLanguageTag();

        attributes.put("pageId", "error");
        attributes.put("statusCode", responseStatus.getStatusCode());

        attributes.put("realm", realm);
        attributes.put("url", new UrlBean(realm, theme, session.getContext().getUri().getBaseUri(), null));
        attributes.put("locale", localeBean);
        attributes.put("lang", lang);

        String errorKey = responseStatus == Response.Status.NOT_FOUND ? Messages.PAGE_NOT_FOUND : Messages.INTERNAL_SERVER_ERROR;
        String errorMessage = messagesBundle.getProperty(errorKey);

        attributes.put("message", new MessageBean(errorMessage, MessageType.ERROR));
        // Default fallback in case an error occurs determining the dark mode later on.
        attributes.put("darkMode", true);

        attributes.put("msg", new MessageFormatterMethod(locale, messagesBundle));
        attributes.put("advancedMsg", new AdvancedMessageFormatterMethod(locale, messagesBundle));

        try {
            Properties properties = theme.getProperties();
            attributes.put("properties", properties);
            attributes.put("darkMode", "true".equals(properties.getProperty("darkMode"))
                    && realm.getAttribute("darkMode", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return attributes;
    }

}
