package org.keycloak.services.resources.account;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.Profile;
import org.keycloak.authentication.requiredactions.DeleteAccount;
import org.keycloak.common.Version;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.UriBuilder;
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
        UriInfo uriInfo = session.getContext().getUri(UrlType.FRONTEND);
        URI accountBaseUrl = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(realm.getName())
                .path(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).path("/").build(realm);

        if (!session.getContext().getUri().getRequestUri().getPath().endsWith("/")) {
            UriBuilder redirectUri = session.getContext().getUri().getRequestUriBuilder().uri(accountBaseUrl);
            return Response.status(302).location(redirectUri.build()).build();
        } else {
            Map<String, Object> map = new HashMap<>();

            URI adminBaseUri = session.getContext().getUri(UrlType.ADMIN).getBaseUri();
            URI authUrl = uriInfo.getBaseUri();
            map.put("authUrl", authUrl.getPath().endsWith("/") ? authUrl : authUrl + "/");
            map.put("baseUrl", accountBaseUrl);
            map.put("realm", realm);
            map.put("resourceUrl", Urls.themeRoot(authUrl).getPath() + "/" + Constants.ACCOUNT_MANAGEMENT_CLIENT_ID + "/" + theme.getName());
            map.put("resourceCommonUrl", Urls.themeRoot(adminBaseUri).getPath() + "/common/keycloak");
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
            map.put("theme", (Function<String, String>) file -> {
                try {
                    final InputStream resource = theme.getResourceAsStream(file);
                    return new Scanner(resource, "UTF-8").useDelimiter("\\A").next();
                } catch (IOException e) {
                    throw new RuntimeException("could not load file", e);
                }
            });

            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            map.put("isEventsEnabled", eventStore != null && realm.isEventsEnabled());
            map.put("isAuthorizationEnabled", Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION));
            
            boolean isTotpConfigured = false;
            boolean deleteAccountAllowed = false;
            if (user != null) {
                isTotpConfigured = session.userCredentialManager().isConfiguredFor(realm, user, realm.getOTPPolicy().getType());
                RoleModel deleteAccountRole = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).getRole(AccountRoles.DELETE_ACCOUNT);
                deleteAccountAllowed = deleteAccountRole != null && user.hasRole(deleteAccountRole) && realm.getRequiredActionProviderByAlias(DeleteAccount.PROVIDER_ID).isEnabled();
            }

            map.put("isTotpConfigured", isTotpConfigured);

            map.put("deleteAccountAllowed", deleteAccountAllowed);

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);
            return builder.build();
        }
    }
    
    private Map<String, String> supportedLocales(Properties messages) {
        return realm.getSupportedLocalesStream()
                .collect(Collectors.toMap(Function.identity(), l -> messages.getProperty("locale_" + l, l)));
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
