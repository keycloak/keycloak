package org.keycloak.adapters.as7;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.GenericPrincipal;
import org.bouncycastle.openssl.PEMWriter;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.servlet.ServletUtil;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.jboss.resteasy.util.BasicAuthHelper;
import org.keycloak.EnvUtil;
import org.keycloak.PemUtils;
import org.keycloak.ResourceMetadata;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.as7.config.AuthServerConfig;
import org.keycloak.adapters.as7.config.ManagedResourceConfig;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.SkeletonKeyToken;

import javax.security.auth.login.LoginException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Turns a web deployment into an authentication server that follwos the OAuth 2 protocol and Skeleton Key bearer tokens.
 * Authentication store is backed by a JBoss security domain.
 * <p/>
 * Servlet FORM authentication that uses the local security domain to authenticate and for role mappings.
 * <p/>
 * Supports bearer token creation and authentication.  The client asking for access must be set up as a valid user
 * within the security domain.
 * <p/>
 * If no an OAuth access request, this works like normal FORM authentication and authorization.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthAuthenticationServerValve extends FormAuthenticator implements LifecycleListener {


    public static class AccessCode {
        protected String id = UUID.randomUUID().toString() + System.currentTimeMillis();
        protected long expiration;
        protected SkeletonKeyToken token;
        protected String client;
        protected boolean sso;
        protected String redirect;

        public boolean isExpired() {
            return expiration != 0 && (System.currentTimeMillis() / 1000) > expiration;
        }

        public String getId() {
            return id;
        }

        public long getExpiration() {
            return expiration;
        }

        public void setExpiration(long expiration) {
            this.expiration = expiration;
        }

        public SkeletonKeyToken getToken() {
            return token;
        }

        public void setToken(SkeletonKeyToken token) {
            this.token = token;
        }

        public String getClient() {
            return client;
        }

        public void setClient(String client) {
            this.client = client;
        }

        public boolean isSso() {
            return sso;
        }

        public void setSso(boolean sso) {
            this.sso = sso;
        }

        public String getRedirect() {
            return redirect;
        }

        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }
    }

    protected ConcurrentHashMap<String, AccessCode> accessCodeMap = new ConcurrentHashMap<String, AccessCode>();
    private static final Logger log = Logger.getLogger(OAuthAuthenticationServerValve.class);

    private static AtomicLong counter = new AtomicLong(1);

    private static String generateId() {
        return counter.getAndIncrement() + "." + UUID.randomUUID().toString();
    }

    protected AuthServerConfig skeletonKeyConfig;
    protected PrivateKey realmPrivateKey;
    protected PublicKey realmPublicKey;
    protected String realmPublicKeyPem;
    protected ResteasyProviderFactory providers;
    protected ResourceMetadata resourceMetadata;
    protected UserSessionManagement userSessionManagement = new UserSessionManagement();
    protected ObjectMapper mapper;
    protected ObjectWriter accessTokenResponseWriter;
    protected ObjectWriter mapWriter;

    private static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        File truststoreFile = new File(filename);
        FileInputStream trustStream = new FileInputStream(truststoreFile);
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    @Override
    public void start() throws LifecycleException {
        super.start();
        StandardContext standardContext = (StandardContext) context;
        standardContext.addLifecycleListener(this);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getType() == Lifecycle.AFTER_START_EVENT) init();
    }

    protected void init() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        accessTokenResponseWriter = mapper.writerWithType(AccessTokenResponse.class);
        mapWriter = mapper.writerWithType(mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

        InputStream is = null;
        String path = context.getServletContext().getInitParameter("skeleton.key.config.file");
        if (path == null) {
            is = context.getServletContext().getResourceAsStream("/WEB-INF/resteasy-oauth.json");
        } else {
            try {
                is = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            skeletonKeyConfig = mapper.readValue(is, AuthServerConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (skeletonKeyConfig.getLoginRole() == null) {
            throw new RuntimeException("You must define the login-role in your config file");
        }
        if (skeletonKeyConfig.getClientRole() == null) {
            throw new RuntimeException("You must define the oauth-client-role in your config file");
        }
        if (skeletonKeyConfig.getRealmPrivateKey() != null) {
            try {
                realmPrivateKey = PemUtils.decodePrivateKey(skeletonKeyConfig.getRealmPrivateKey());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (skeletonKeyConfig.getRealmPublicKey() != null) {
            try {
                realmPublicKey = PemUtils.decodePublicKey(skeletonKeyConfig.getRealmPublicKey());
                realmPublicKeyPem = skeletonKeyConfig.getRealmPublicKey();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (skeletonKeyConfig.getRealmKeyStore() != null) {
            if (skeletonKeyConfig.getRealmKeyAlias() == null) throw new RuntimeException("Must define realm-key-alias");
            String keystorePath = EnvUtil.replace(skeletonKeyConfig.getRealmKeyStore());
            try {
                KeyStore ks = loadKeyStore(keystorePath, skeletonKeyConfig.getRealmKeystorePassword());
                if (realmPrivateKey == null) {
                    realmPrivateKey = (PrivateKey) ks.getKey(skeletonKeyConfig.getRealmKeyAlias(), skeletonKeyConfig.getRealmPrivateKeyPassword().toCharArray());
                }
                if (realmPublicKey == null) {
                    Certificate cert = ks.getCertificate(skeletonKeyConfig.getRealmKeyAlias());
                    realmPublicKey = cert.getPublicKey();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (realmPublicKey == null) throw new RuntimeException("You have not declared a keystore or public key");
        if (realmPrivateKey == null) throw new RuntimeException("You have not declared a keystore or private key");
        if (realmPublicKeyPem == null) {
            StringWriter sw = new StringWriter();
            PEMWriter writer = new PEMWriter(sw);
            try {
                writer.writeObject(realmPublicKey);
                writer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            realmPublicKeyPem = sw.toString();
            realmPublicKeyPem = PemUtils.removeBeginEnd(realmPublicKeyPem);
        }
        providers = new ResteasyProviderFactory();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(OAuthAuthenticationServerValve.class.getClassLoader());
        try {
            ResteasyProviderFactory.getInstance(); // initialize builtins
            RegisterBuiltin.register(providers);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        resourceMetadata = new ResourceMetadata();
        resourceMetadata.setRealm(skeletonKeyConfig.getRealm());
        resourceMetadata.setRealmKey(realmPublicKey);
        String truststore = skeletonKeyConfig.getTruststore();
        if (truststore != null) {
            truststore = EnvUtil.replace(truststore);
            String truststorePassword = skeletonKeyConfig.getTruststorePassword();
            KeyStore trust = null;
            try {
                trust = loadKeyStore(truststore, truststorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
            resourceMetadata.setTruststore(trust);
        }
        String clientKeystore = skeletonKeyConfig.getClientKeystore();
        String clientKeyPassword = null;
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = skeletonKeyConfig.getClientKeystorePassword();
            KeyStore serverKS = null;
            try {
                serverKS = loadKeyStore(clientKeystore, clientKeystorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
            resourceMetadata.setClientKeystore(serverKS);
            clientKeyPassword = skeletonKeyConfig.getClientKeyPassword();
            resourceMetadata.setClientKeyPassword(clientKeyPassword);
        }
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        try {
            String contextPath = request.getContextPath();
            String requestURI = request.getDecodedRequestURI();
            log.debug("--- invoke: " + requestURI);
            if (request.getMethod().equalsIgnoreCase("GET")
                    && context.getLoginConfig().getLoginPage().equals(request.getRequestPathMB().toString())) {
                if (handleLoginPage(request, response)) return;
            } else if (request.getMethod().equalsIgnoreCase("GET")
                    && requestURI.endsWith(Actions.J_OAUTH_LOGOUT)) {
                logoutCurrentUser(request, response);
                return;
            } else if (request.getMethod().equalsIgnoreCase("POST")
                    && requestURI.endsWith(Actions.J_OAUTH_ADMIN_FORCED_LOGOUT)) {
                adminLogout(request, response);
                return;
            } else if (request.getMethod().equalsIgnoreCase("POST")
                    && requestURI.startsWith(contextPath) &&
                    requestURI.endsWith(Constants.FORM_ACTION)
                    && request.getParameter("client_id") != null) {
                handleOAuth(request, response);
                return;
            } else if (request.getMethod().equalsIgnoreCase("POST")
                    && requestURI.endsWith(Actions.J_OAUTH_TOKEN_GRANT)) {
                tokenGrant(request, response);
                return;
            } else if (request.getMethod().equalsIgnoreCase("POST")
                    && requestURI.startsWith(contextPath) &&
                    requestURI.endsWith(Actions.J_OAUTH_RESOLVE_ACCESS_CODE)) {
                resolveAccessCode(request, response);
                return;
            } else if (request.getMethod().equalsIgnoreCase("GET")
                    && requestURI.startsWith(contextPath) &&
                    requestURI.endsWith("j_oauth_realm_info.html")) {
                publishRealmInfoHtml(request, response);
                return;
            }
            // propagate the skeleton key token string?
            if (!skeletonKeyConfig.isCancelPropagation()) {
                if (request.getAttribute(SkeletonKeySession.class.getName()) == null && request.getSessionInternal() != null) {
                    SkeletonKeySession skSession = (SkeletonKeySession) request.getSessionInternal().getNote(SkeletonKeySession.class.getName());
                    if (skSession != null) {
                        request.setAttribute(SkeletonKeySession.class.getName(), skSession);
                        ResteasyProviderFactory.pushContext(SkeletonKeySession.class, skSession);
                    }
                }
            }
            request.setAttribute("OAUTH_FORM_ACTION", "j_security_check");
            super.invoke(request, response);
        } finally {
            ResteasyProviderFactory.clearContextData();  // to clear push of SkeletonKeySession
        }
    }

    protected boolean handleLoginPage(Request request, Response response) throws IOException, ServletException {
        String client_id = request.getParameter("client_id");
        // if this is not an OAUTH redirect, just return and let the default flow happen
        if (client_id == null) return false;

        String redirect_uri = request.getParameter("redirect_uri");
        String state = request.getParameter("state");

        if (redirect_uri == null) {
            response.sendError(400, "No oauth redirect query parameter set");
            return true;
        }
        // only bypass authentication if our session is authenticated,
        // the login query parameter is on request URL,
        // and we have configured the login-role
        else if (!skeletonKeyConfig.isSsoDisabled()
                && request.getSessionInternal() != null
                && request.getSessionInternal().getPrincipal() != null
                && request.getParameter("login") != null) {
            log.debug("We're ALREADY LOGGED IN!!!");
            GenericPrincipal gp = (GenericPrincipal) request.getSessionInternal().getPrincipal();
            redirectAccessCode(true, response, redirect_uri, client_id, state, gp);
        } else {
            UriBuilder builder = UriBuilder.fromUri("j_security_check")
                    .queryParam("redirect_uri", redirect_uri)
                    .queryParam("client_id", client_id);
            if (state != null) builder.queryParam("state", state);
            String loginAction = builder.build().toString();
            request.setAttribute("OAUTH_FORM_ACTION", loginAction);
            getNext().invoke(request, response);
        }
        return true;
    }

    protected GenericPrincipal checkLoggedIn(Request request, HttpServletResponse response) {
        if (request.getPrincipal() != null) {
            return (GenericPrincipal) request.getPrincipal();
        } else if (request.getSessionInternal() != null && request.getSessionInternal().getPrincipal() != null) {
            return (GenericPrincipal) request.getSessionInternal().getPrincipal();
        }
        return null;
    }


    protected void adminLogout(Request request, HttpServletResponse response) throws IOException {
        log.debug("<< adminLogout");
        GenericPrincipal gp = checkLoggedIn(request, response);
        if (gp == null) {
            if (bearer(request, response, false)) {
                gp = (GenericPrincipal) request.getPrincipal();
            } else {
                response.sendError(403);
                return;
            }
        }
        if (!gp.hasRole(skeletonKeyConfig.getAdminRole())) {
            response.sendError(403);
            return;
        }
        String logoutUser = request.getParameter("user");
        if (logoutUser != null) {
            userSessionManagement.logout(logoutUser);
            logoutResources(logoutUser, gp.getName());
        } else {
            userSessionManagement.logoutAllBut(gp.getName());
            logoutResources(null, gp.getName());
        }
        String forwardTo = request.getParameter("forward");
        if (forwardTo == null) {
            response.setStatus(204);
            return;
        }
        RequestDispatcher disp =
                context.getServletContext().getRequestDispatcher(forwardTo);
        try {
            disp.forward(request.getRequest(), response);
        } catch (Throwable t) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "failed to forward");
        }


    }


    protected void logoutCurrentUser(Request request, HttpServletResponse response) throws IOException {
        if (request.getSessionInternal() == null || request.getSessionInternal().getPrincipal() == null) {
            redirectToWelcomePage(request, response);
            return;
        }
        GenericPrincipal principal = (GenericPrincipal) request.getSessionInternal().getPrincipal();
        String username = principal.getName();
        String admin = username;
        userSessionManagement.logout(username);
        request.setUserPrincipal(null);
        request.setAuthType(null);
        // logout user on all declared authenticated resources
        logoutResources(username, admin);
        redirectToWelcomePage(request, response);
    }

    protected void logoutResources(String username, String admin) {
        if (skeletonKeyConfig.getResources().size() != 0) {
            SkeletonKeyToken token = new SkeletonKeyToken();
            token.id(generateId());
            token.principal(admin);
            token.audience(skeletonKeyConfig.getRealm());
            SkeletonKeyToken.Access realmAccess = new SkeletonKeyToken.Access();
            realmAccess.addRole(skeletonKeyConfig.getAdminRole());
            token.setRealmAccess(realmAccess);
            String tokenString = buildTokenString(realmPrivateKey, token);
            ResteasyClient client = new ResteasyClientBuilder()
                    .providerFactory(providers)
                    .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY)
                    .trustStore(resourceMetadata.getTruststore())
                    .keyStore(resourceMetadata.getClientKeystore(), resourceMetadata.getClientKeyPassword())
                    .build();
            try {
                for (String resource : skeletonKeyConfig.getResources()) {
                    try {
                        log.debug("logging out: " + resource);
                        WebTarget target = client.target(resource).path(Actions.J_OAUTH_REMOTE_LOGOUT);
                        if (username != null) target = target.queryParam("user", username);
                        javax.ws.rs.core.Response response = target.request()
                                .header("Authorization", "Bearer " + tokenString)
                                .put(null);
                        if (response.getStatus() != 204) log.error("Failed to log out");
                        response.close();
                    } catch (Exception ignored) {
                        log.error("Failed to log out", ignored);
                    }
                }
            } finally {
                client.close();
            }
        }
    }

    protected void redirectToWelcomePage(Request request, HttpServletResponse response) throws IOException {
        ResteasyUriInfo uriInfo = ServletUtil.extractUriInfo(request, null);
        String[] welcomes = context.findWelcomeFiles();
        if (welcomes.length > 0) {
            UriBuilder welcome = uriInfo.getBaseUriBuilder().path(welcomes[0]);
            response.sendRedirect(welcome.toTemplate());
        } else {
            response.setStatus(204);
        }
    }


    protected void publishRealmInfoHtml(Request request, HttpServletResponse response) throws IOException {
        ManagedResourceConfig rep = getRealmRepresentation(request);
        StringWriter writer;
        String json;

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);

        StringBuffer html = new StringBuffer();
        html.append("<html><body bgcolor=\"#CED8F6\">");
        html.append("<h1>Realm: ").append(rep.getRealm()).append("</h1>");

        ManagedResourceConfig bearer = new ManagedResourceConfig();
        bearer.setRealm(rep.getRealm());
        bearer.setRealmKey(rep.getRealmKey());
        writer = new StringWriter();
        mapper.writeValue(writer, bearer);
        json = writer.toString();

        html.append("<h3>BearerTokenAuthValve Json Config</h3>");
        html.append("<form><textarea rows=\"7\" cols=\"80\">").append(json).append("</textarea></form>");

        html.append("<br>");

        writer = new StringWriter();
        rep.getCredentials().put("password", "REQUIRED");
        //rep.setClientId("REQUIRED");
        rep.setTruststore("REQUIRED");
        rep.setTruststorePassword("REQUIRED");
        mapper.writeValue(writer, rep);
        json = writer.toString();
        html.append("<h3>OAuthManagedResourceValve Json Config</h3>");
        html.append("<form><textarea rows=\"20\" cols=\"80\">").append(json).append("</textarea></form>");

        html.append("</body></html>");

        response.setStatus(200);
        response.setContentType("text/html");
        response.getOutputStream().println(html.toString());
        response.getOutputStream().flush();

    }


    protected ManagedResourceConfig getRealmRepresentation(Request request) {
        ManagedResourceConfig rep = new ManagedResourceConfig();
        ResteasyUriInfo uriInfo = ServletUtil.extractUriInfo(request, null);
        UriBuilder authUrl = uriInfo.getBaseUriBuilder().path(context.getLoginConfig().getLoginPage());
        UriBuilder codeUrl = uriInfo.getBaseUriBuilder().path(Actions.J_OAUTH_RESOLVE_ACCESS_CODE);
        rep.setRealm(skeletonKeyConfig.getRealm());
        rep.setRealmKey(realmPublicKeyPem);
        rep.setAuthUrl(authUrl.toTemplate());
        rep.setCodeUrl(codeUrl.toTemplate());
        rep.setAdminRole(skeletonKeyConfig.getAdminRole());
        return rep;
    }

    public boolean bearer(Request request, HttpServletResponse response, boolean propagate) throws IOException {
        if (request.getHeader("Authorization") != null) {
            CatalinaBearerTokenAuthenticator bearer = new CatalinaBearerTokenAuthenticator(resourceMetadata, true, false, false);
            try {
                if (bearer.login(request, response)) {
                    return true;
                }
            } catch (LoginException e) {
            }
        }
        return false;
    }

    @Override
    protected void register(Request request, HttpServletResponse response, Principal principal, String authType, String username, String password) {
        super.register(request, response, principal, authType, username, password);
        log.debug("authenticate userSessionManage.login(): " + principal.getName());
        userSessionManagement.login(request.getSessionInternal(), principal.getName());
        if (!skeletonKeyConfig.isCancelPropagation()) {
            GenericPrincipal gp = (GenericPrincipal) request.getPrincipal();
            if (gp != null) {
                SkeletonKeyToken token = buildToken(gp);
                String stringToken = buildTokenString(realmPrivateKey, token);
                SkeletonKeySession skSession = new SkeletonKeySession(stringToken, resourceMetadata);
                request.setAttribute(SkeletonKeySession.class.getName(), skSession);
                ResteasyProviderFactory.pushContext(SkeletonKeySession.class, skSession);
                request.getSessionInternal(true).setNote(SkeletonKeySession.class.getName(), skSession);
            }
        }
    }

    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        if (bearer(request, response, true)) {
            return true;
        }
        return super.authenticate(request, response, config);
    }


    protected void resolveAccessCode(Request request, Response response) throws IOException {
        if (!request.isSecure()) {
            response.sendError(400);
            return;
        }
        // always verify code and remove access code from map before authenticating user
        // if user authentication fails, we want the code to be removed irreguardless just in case we're under attack
        String code = request.getParameter("code");
        JWSInput input = new JWSInput(code, providers);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realmPublicKey);
        } catch (Exception ignored) {
            log.error("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Unable to verify code signature");
            response.sendError(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        }
        String key = input.readContent(String.class);
        AccessCode accessCode = accessCodeMap.remove(key);
        String redirect = request.getParameter("redirect_uri");

        GenericPrincipal gp = basicAuth(request, response);
        if (gp == null) {
            log.error("Failed to authenticate client_id");
            return;
        }
        if (accessCode == null) {
            log.error("No access code: " + code);
            response.sendError(400);
            return;
        }
        if (accessCode.isExpired()) {
            log.debug("Access code expired");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Code is expired");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        }
        if (!accessCode.getToken().isActive()) {
            log.debug("token not active");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Token expired");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        }
        if (!gp.getName().equals(accessCode.getClient())) {
            log.debug("not equal client");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        }
        if (!accessCode.getRedirect().equals(redirect)) {
            log.debug("not equal redirect");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        }
        if (accessCode.isSso() && !gp.hasRole(skeletonKeyConfig.getLoginRole())) {
            // we did not authenticate user on an access code request because a session was already established
            // but, the client_id does not have permission to bypass this on a simple grant.  We want
            // to always ask for credentials from a simple oath request

            log.debug("does not have login permission");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;
        } else if (!gp.hasRole(skeletonKeyConfig.getClientRole()) && !gp.hasRole(skeletonKeyConfig.getLoginRole())) {
            log.debug("does not have login or client role permission for access token request");
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            response.setStatus(400);
            response.setContentType("application/json");
            mapWriter.writeValue(response.getOutputStream(), res);
            response.getOutputStream().flush();
            return;

        }
        String wildcard = skeletonKeyConfig.getWildcardRole() == null ? "*" : skeletonKeyConfig.getWildcardRole();
        Set<String> codeRoles = accessCode.getToken().getRealmAccess().getRoles();
        if (codeRoles != null &&
                (codeRoles.contains(skeletonKeyConfig.getClientRole()) || codeRoles.contains(skeletonKeyConfig.getLoginRole()))) {
            // we store roles a oauth client is granted in the user role mapping, remove those roles as we don't want those clients with those
            // permissions if they are logging in.
            Set<String> newRoles = new HashSet<String>();
            if (codeRoles.contains(skeletonKeyConfig.getClientRole())) newRoles.add(skeletonKeyConfig.getClientRole());
            if (codeRoles.contains(skeletonKeyConfig.getLoginRole())) newRoles.add(skeletonKeyConfig.getLoginRole());
            if (codeRoles.contains(wildcard)) newRoles.add(wildcard);
            codeRoles.clear();
            codeRoles.addAll(newRoles);
        }

        // is we have a login role, then we don't need to filter out roles, just grant all the roles the user has
        // Also, if the client has the "wildcard" role, then we don't need to filter out roles
        if (codeRoles != null
                && !gp.hasRole(wildcard)
                && !gp.hasRole(skeletonKeyConfig.getLoginRole())) {
            Set<String> clientAllowed = new HashSet<String>();
            for (String role : gp.getRoles()) {
                clientAllowed.add(role);
            }
            Set<String> newRoles = new HashSet<String>();
            newRoles.addAll(codeRoles);
            for (String role : newRoles) {
                if (!clientAllowed.contains(role)) {
                    codeRoles.remove(role);
                }
            }
        }
        AccessTokenResponse res = accessTokenResponse(realmPrivateKey, accessCode.getToken());
        response.setStatus(200);
        response.setContentType("application/json");
        accessTokenResponseWriter.writeValue(response.getOutputStream(), res);
        response.getOutputStream().flush();
    }

    protected AccessTokenResponse accessTokenResponse(PrivateKey privateKey, SkeletonKeyToken token) {
        String encodedToken = buildTokenString(privateKey, token);

        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedToken);
        res.setTokenType("bearer");
        if (token.getExpiration() != 0) {
            long time = token.getExpiration() - (System.currentTimeMillis() / 1000);
            res.setExpiresIn(time);
        }
        return res;
    }

    protected String buildTokenString(PrivateKey privateKey, SkeletonKeyToken token) {
        byte[] tokenBytes = null;
        try {
            tokenBytes = JsonSerialization.toByteArray(token, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new JWSBuilder()
                .content(tokenBytes)
                .rsa256(privateKey);
    }


    protected void handleOAuth(Request request, Response response) throws IOException {
        log.debug("<--- Begin oauthAuthenticate");
        String redirect_uri = request.getParameter("redirect_uri");
        String client_id = request.getParameter("client_id");
        String state = request.getParameter("state");
        String username = request.getParameter(Constants.FORM_USERNAME);
        String password = request.getParameter(Constants.FORM_PASSWORD);
        Principal principal = context.getRealm().authenticate(username, password);
        if (principal == null) {
            UriBuilder builder = UriBuilder.fromUri(redirect_uri).queryParam("error", "unauthorized_client");
            if (state != null) builder.queryParam("state", state);
            response.sendRedirect(builder.toTemplate());
            return;
        }
        GenericPrincipal gp = (GenericPrincipal) principal;
        register(request, response, principal, HttpServletRequest.FORM_AUTH, username, password);
        userSessionManagement.login(request.getSessionInternal(), username);
        redirectAccessCode(false, response, redirect_uri, client_id, state, gp);

        return;
    }

    protected void tokenGrant(Request request, Response response) throws IOException {
        if (!request.isSecure()) {
            response.sendError(400);
            return;
        }
        GenericPrincipal gp = basicAuth(request, response);
        if (gp == null) return;
        SkeletonKeyToken token = buildToken(gp);
        AccessTokenResponse res = accessTokenResponse(realmPrivateKey, token);
        response.setStatus(200);
        response.setContentType("application/json");
        accessTokenResponseWriter.writeValue(response.getOutputStream(), res);
        response.getOutputStream().flush();
    }

    protected GenericPrincipal basicAuth(Request request, Response response) throws IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) {
            basicAuthError(response);
            return null;
        }
        String[] creds = BasicAuthHelper.parseHeader(authHeader);
        if (creds == null) {
            basicAuthError(response);
            return null;
        }
        String username = creds[0];
        String password = creds[1];
        GenericPrincipal gp = (GenericPrincipal) context.getRealm().authenticate(username, password);
        if (gp == null) {
            basicAuthError(response);
            return null;
        }
        return gp;
    }

    protected void basicAuthError(Response response) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + context.getLoginConfig().getRealmName() + "\"");
        response.sendError(401);
    }

    protected void redirectAccessCode(boolean sso, Response response, String redirect_uri, String client_id, String state, GenericPrincipal gp) throws IOException {
        SkeletonKeyToken token = buildToken(gp);
        AccessCode code = new AccessCode();
        code.setToken(token);
        code.setClient(client_id);
        code.setSso(sso);
        code.setRedirect(redirect_uri);
        int expiration = skeletonKeyConfig.getAccessCodeLifetime() == 0 ? 300 : skeletonKeyConfig.getAccessCodeLifetime();
        code.setExpiration((System.currentTimeMillis() / 1000) + expiration);
        accessCodeMap.put(code.getId(), code);
        log.debug("--- sign access code");
        String accessCode = null;
        try {
            accessCode = new JWSBuilder().content(code.getId().getBytes("UTF-8")).rsa256(realmPrivateKey);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        log.debug("--- build redirect");
        UriBuilder redirectUri = UriBuilder.fromUri(redirect_uri).queryParam("code", accessCode);
        if (state != null) redirectUri.queryParam("state", state);
        response.sendRedirect(redirectUri.toTemplate());
        log.debug("<--- end oauthAuthenticate");
    }

    protected SkeletonKeyToken buildToken(GenericPrincipal gp) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(generateId());
        token.principal(gp.getName());
        token.audience(skeletonKeyConfig.getRealm());
        int expiration = skeletonKeyConfig.getAccessCodeLifetime() == 0 ? 3600 : skeletonKeyConfig.getAccessCodeLifetime();
        if (skeletonKeyConfig.getTokenLifetime() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + expiration);
        }
        SkeletonKeyToken.Access realmAccess = new SkeletonKeyToken.Access();
        for (String role : gp.getRoles()) {
            realmAccess.addRole(role);
        }
        token.setRealmAccess(realmAccess);
        return token;
    }

}
