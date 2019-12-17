package org.keycloak.services.resources.account;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.Profile;
import org.keycloak.common.Version;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
import org.keycloak.theme.BrowserSecurityHeaderSetup;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;
import org.keycloak.urls.UrlType;
import org.keycloak.utils.MediaType;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.UriInfo;
import org.keycloak.services.resources.RealmsResource;

/**
 * Created by st on 29/03/17.
 */
public class AccountConsole {
    private static final Logger logger = Logger.getLogger(AccountConsole.class);
    
    private final Pattern bundleParamPattern = Pattern.compile("(\\{\\s*(\\d+)\\s*\\})");

    @Context
    protected KeycloakSession session;

    private final AppAuthManager authManager;
    private final RealmModel realm;
    private final ClientModel client;
    private final Theme theme;

    private Auth auth;

    public AccountConsole(RealmModel realm, ClientModel client, Theme theme) {
        this.realm = realm;
        this.client = client;
        this.theme = theme;
        this.authManager = new AppAuthManager();
    }

    public void init() {
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm);
        if (authResult != null) {
            auth = new Auth(realm, authResult.getToken(), authResult.getUser(), client, authResult.getSession(), true);
        }
    }

    @GET
    @NoCache
    public Response getMainPage() throws IOException, FreeMarkerException {
        if (!session.getContext().getUri().getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(session.getContext().getUri().getRequestUriBuilder().path("/").build()).build();
        } else {
            Map<String, Object> map = new HashMap<>();

            UriInfo uriInfo = session.getContext().getUri(UrlType.FRONTEND);
            URI authUrl = uriInfo.getBaseUri();
            map.put("authUrl", authUrl.toString());
            map.put("baseUrl", uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(realm.getName()).path(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).build(realm).toString());
            map.put("realm", realm);
            map.put("resourceUrl", Urls.themeRoot(authUrl).getPath() + "/" + Constants.ACCOUNT_MANAGEMENT_CLIENT_ID + "/" + theme.getName());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            
            String[] referrer = getReferrer();
            if (referrer != null) {
                map.put("referrer", referrer[0]);
                map.put("referrerName", referrer[1]);
                map.put("referrer_uri", referrer[2]);
            }
            
            UserModel user = null;
            if (auth != null) user = auth.getUser();
            Locale locale = session.getContext().resolveLocale(user);
            map.put("locale", locale.toLanguageTag());
            Properties messages = theme.getMessages(locale);
            map.put("msg", new MessageFormatterMethod(locale, messages));
            map.put("msgJSON", messagesToJsonString(messages));
            map.put("supportedLocales", supportedLocales(messages));
            map.put("properties", theme.getProperties());
            
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            map.put("isEventsEnabled", eventStore != null && realm.isEventsEnabled());
            map.put("isAuthorizationEnabled", true);
            
            boolean isTotpConfigured = false;
            if (user != null) {
                isTotpConfigured = session.userCredentialManager().isConfiguredFor(realm, user, realm.getOTPPolicy().getType());
            }
            map.put("isTotpConfigured", isTotpConfigured);

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            return builder.build();
        }
    }
    
    private Map<String, String> supportedLocales(Properties messages) throws IOException {
        Map<String, String> supportedLocales = new HashMap<>();
        for (String l : realm.getSupportedLocales()) {
            String label = messages.getProperty("locale_" + l, l);
            supportedLocales.put(l, label);
        }
        return supportedLocales;
    }
    
    private String messagesToJsonString(Properties props) {
        if (props == null) return "";
        
        JsonObjectBuilder json = Json.createObjectBuilder();
        for (String prop : props.stringPropertyNames()) {
            json.add(prop, convertPropValue(props.getProperty(prop)));
        }
        
        return json.build().toString();
    }
    
    private String convertPropValue(String propertyValue) {
        propertyValue = propertyValue.replace("''", "%27");
        propertyValue = propertyValue.replace("'", "%27");
        propertyValue = propertyValue.replace("\"", "%22");
        
        propertyValue = putJavaParamsInNgTranslateFormat(propertyValue);

        return propertyValue;
    }
    
    // Put java resource bundle params in ngx-translate format
    // Do you like {0} and {1} ?
    //    becomes
    // Do you like {{param_0}} and {{param_1}} ?
    private String putJavaParamsInNgTranslateFormat(String propertyValue) {
        Matcher matcher = bundleParamPattern.matcher(propertyValue);
        while (matcher.find()) {
            propertyValue = propertyValue.replace(matcher.group(1), "{{param_" + matcher.group(2) + "}}");
        }

        return propertyValue;
    }
    
    @GET
    @Path("index.html")
    public Response getIndexHtmlRedirect() {
        return Response.status(302).location(session.getContext().getUri().getRequestUriBuilder().path("../").build()).build();
    }

    // TODO: took this code from elsewhere - refactor
    private String[] getReferrer() {
        String referrer = session.getContext().getUri().getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = session.getContext().getUri().getQueryParameters().getFirst("referrer_uri");

        ClientModel referrerClient = realm.getClientByClientId(referrer);
        if (referrerClient != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, referrerClient);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(session, client.getRootUrl(), referrerClient.getBaseUrl());
            }
            
            if (referrerUri != null) {
                String referrerName = referrerClient.getName();
                if (Validation.isBlank(referrerName)) {
                    referrerName = referrer;
                }
                return new String[]{referrer, referrerName, referrerUri};
            }
        } else if (referrerUri != null) {
            referrerClient = realm.getClientByClientId(referrer);
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(session, referrerUri, referrerClient);

                if (referrerUri != null) {
                    return new String[]{referrer, referrer, referrerUri};
                }
            }
        }

        return null;
    }

}
