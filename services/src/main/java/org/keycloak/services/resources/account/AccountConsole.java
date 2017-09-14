package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.Version;
import org.keycloak.models.*;
import org.keycloak.models.Constants;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.theme.BrowserSecurityHeaderSetup;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.utils.MediaType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.services.validation.Validation;

/**
 * Created by st on 29/03/17.
 */
public class AccountConsole {
    private static final Logger logger = Logger.getLogger(AccountConsole.class);
    
    private final Pattern bundleParamPattern = Pattern.compile("(\\{\\s*(\\d+)\\s*\\})");

    @Context
    protected KeycloakSession session;
    @Context
    protected UriInfo uriInfo;
    
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
    public Response getMainPage() throws URISyntaxException, IOException, FreeMarkerException {
        if (!uriInfo.getRequestUri().getPath().endsWith("/")) {
            return Response.status(302).location(uriInfo.getRequestUriBuilder().path("/").build()).build();
        } else {
            Map<String, Object> map = new HashMap<>();

            URI baseUri = uriInfo.getBaseUri();

            String authUrl = baseUri.toString();
            authUrl = authUrl.substring(0, authUrl.length() - 1);

            map.put("authUrl", authUrl);
            map.put("baseUrl", authUrl + "/realms/" + realm.getName() + "/account");
            map.put("realm", realm.getName());
            map.put("resourceUrl", Urls.themeRoot(baseUri) + "/account/" + theme.getName());
            map.put("resourceVersion", Version.RESOURCES_VERSION);
            
            String[] referrer = getReferrer();
            if (referrer != null) {
                map.put("referrer", referrer[0]);
                map.put("referrer_uri", referrer[1]);
            }
            
            try {
                if (auth != null) {
                    Locale locale = session.getContext().resolveLocale(auth.getUser());
                    map.put("locale", locale.toLanguageTag());
                    map.put("msg", messagesToJsonString(theme.getMessages(locale)));
                }
            } catch (Exception e) {
                logger.warn("Failed to load messages", e);
            }
            
            map.put("properties", theme.getProperties());

            FreeMarkerUtil freeMarkerUtil = new FreeMarkerUtil();
            String result = freeMarkerUtil.processTemplate(map, "index.ftl", theme);
            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.TEXT_HTML_UTF_8).language(Locale.ENGLISH).entity(result);
            BrowserSecurityHeaderSetup.headers(builder, realm);
            return builder.build();
        }
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

    @GET
    @Path("keycloak.json")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ClientManager.InstallationAdapterConfig getConfig() {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient == null) {
            throw new javax.ws.rs.NotFoundException("Account console client not found");
        }
        RealmManager realmMgr = new RealmManager(session);
        URI baseUri = session.getContext().getUri().getBaseUri();
        return new ClientManager(realmMgr).toInstallationRepresentation(realm, accountClient, baseUri);
    }
    
    // TODO: took this code from elsewhere - refactor
    private String[] getReferrer() {
        String referrer = uriInfo.getQueryParameters().getFirst("referrer");
        if (referrer == null) {
            return null;
        }

        String referrerUri = uriInfo.getQueryParameters().getFirst("referrer_uri");

        ClientModel referrerClient = realm.getClientByClientId(referrer);
        if (referrerClient != null) {
            if (referrerUri != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, referrerClient);
            } else {
                referrerUri = ResolveRelative.resolveRelativeUri(uriInfo.getRequestUri(), client.getRootUrl(), referrerClient.getBaseUrl());
            }
            
            if (referrerUri != null) {
                String referrerName = referrerClient.getName();
                if (Validation.isBlank(referrerName)) {
                    referrerName = referrer;
                }
                return new String[]{referrerName, referrerUri};
            }
        } else if (referrerUri != null) {
            referrerClient = realm.getClientByClientId(referrer);
            if (client != null) {
                referrerUri = RedirectUtils.verifyRedirectUri(uriInfo, referrerUri, realm, referrerClient);

                if (referrerUri != null) {
                    return new String[]{referrer, referrerUri};
                }
            }
        }

        return null;
    }

}
