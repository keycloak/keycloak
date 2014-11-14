package org.keycloak.adapters;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.Principal;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.VerificationException;
import org.keycloak.constants.GenericConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.PemUtils;

/**
 * Login module, which allows to authenticate Keycloak access token in environments, which rely on JAAS
 * <p/>
 * It expects login based on username and password where username must be equal to "Bearer" and password is keycloak access token.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BearerTokenLoginModule implements LoginModule {

    private final static Logger log = Logger.getLogger("" + BearerTokenLoginModule.class);

    public static final String KEYCLOAK_CONFIG_FILE_OPTION = "keycloak-config-file";
    public static final String REALM_OPTION = "realm";
    public static final String RESOURCE_OPTION = "resource";
    public static final String PUBLIC_KEY_OPTION = "realm-public-key";
    public static final String AUTH_SERVER_URL_OPTION = "auth-server-url";
    public static final String USE_RESOURCE_ROLE_MAPPINGS_OPTION = "use-resource-role-mappings";
    public static final String PRINCIPAL_ATTRIBUTE_OPTION = "principal-attribute";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Auth auth;

    private static volatile KeycloakDeployment deployment;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        // Static as we don't want to parse config file in each authentication
        if (deployment == null) {
            KeycloakDeployment kd;
            String configFile = (String)options.get(KEYCLOAK_CONFIG_FILE_OPTION);
            if (configFile != null) {
                InputStream is = loadKeycloakConfigFile(configFile);
                kd = KeycloakDeploymentBuilder.build(is);
            } else {
                // init everything from provided options
                String realm = (String) options.get(REALM_OPTION);
                if (realm == null) {
                    throw new IllegalArgumentException("Realm is mandatory if you didn't provide keycloak-config-file");
                }
                String authServerUrl = (String) options.get(AUTH_SERVER_URL_OPTION);
                String publicKey = (String) options.get(PUBLIC_KEY_OPTION);
                if (publicKey == null && authServerUrl == null) {
                    throw new IllegalArgumentException("Option " + PUBLIC_KEY_OPTION + " is mandatory if you didn't provide keycloak-config-file or auth-server-url to resolver public key");
                }
                String resource = (String) options.get(RESOURCE_OPTION);
                String resRoleMappings = (String) options.get(USE_RESOURCE_ROLE_MAPPINGS_OPTION);
                boolean useResourceRoleMappings = resRoleMappings == null ? false : Boolean.parseBoolean(resRoleMappings);
                if (useResourceRoleMappings && resource == null) {
                    throw new IllegalArgumentException("You want resource-role-mappings, but you didn't provide resource in configuration");
                }
                String principalAttribute = (String) options.get(PRINCIPAL_ATTRIBUTE_OPTION);

                kd = new KeycloakDeployment();
                kd.setRealm(realm);
                kd.setResourceName(resource);
                kd.setUseResourceRoleMappings(useResourceRoleMappings);
                kd.setPrincipalAttribute(principalAttribute);
                if (publicKey != null) {
                    try {
                        PublicKey pk = PemUtils.decodePublicKey(publicKey);
                        kd.setRealmKey(pk);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (kd.getRealmKey() == null) {
                new AdapterDeploymentContext().resolveRealmKey(kd);
            }
            deployment = kd;
        }
    }

    protected InputStream loadKeycloakConfigFile(String keycloakConfigFile) {
        if (keycloakConfigFile.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {
            String classPathLocation = keycloakConfigFile.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
            log.info("Loading config from classpath on location: " + classPathLocation);
            // Try current class classloader first
            InputStream is = getClass().getClassLoader().getResourceAsStream(classPathLocation);
            if (is == null) {
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(classPathLocation);
            }

            if (is != null) {
                return is;
            } else {
                throw new RuntimeException("Unable to find config from classpath: " + keycloakConfigFile);
            }
        } else {
            // Fallback to file
            try {
                log.info("Loading config from file: " + keycloakConfigFile);
                return new FileInputStream(keycloakConfigFile);
            } catch (FileNotFoundException fnfe) {
                log.severe("Config not found on " + keycloakConfigFile);
                throw new RuntimeException(fnfe);
            }
        }
    }

    @Override
    public boolean login() throws LoginException {
        // get username and password
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(callbacks);
            String username = ((NameCallback) callbacks[0]).getName();
            char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
            String password = new String(tmpPassword);
            ((PasswordCallback) callbacks[1]).clearPassword();

            Auth auth = bearerAuth(username, password);
            if (auth != null) {
                this.auth = auth;
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedCallbackException uce) {
            LoginException le = new LoginException("Error: " + uce.getCallback().toString()
                    + " not available to gather authentication information from the user");
            le.initCause(uce);
            throw le;
        } catch (Exception ioe) {
            LoginException le = new LoginException(ioe.toString());
            le.initCause(ioe);
            throw le;
        }
    }

    protected Auth bearerAuth(String username, String tokenString) throws VerificationException {
        if ("Bearer".equalsIgnoreCase(username)) {
            log.fine("Username is expected to be bearer but is " + username + ". Ignoring login module");
            return null;
        }

        AccessToken token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealm());

        boolean verifyCaller;
        if (deployment.isUseResourceRoleMappings()) {
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        if (verifyCaller) {
            throw new IllegalStateException("VerifyCaller not supported yet in login module");
        }

        RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(deployment, null, tokenString, token, null, null, null);
        String principalName = AdapterUtils.getPrincipalName(deployment, token);
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(principalName, skSession);
        final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
        return new Auth(principal, roles, tokenString);
    }

    @Override
    public boolean commit() throws LoginException {
        if (auth == null) {
            return false;
        }

        this.subject.getPrincipals().add(auth.getPrincipal());
        this.subject.getPrivateCredentials().add(auth.getTokenString());
        for (String roleName : auth.getRoles()) {
            RolePrincipal rolePrinc = new RolePrincipal(roleName);
            this.subject.getPrincipals().add(rolePrinc);
        }

        return true;
    }

    // Might be needed if subclass wants to setup security context in some env specific way
    protected Auth getAuth() {
        return auth;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        Set<Principal> principals = new HashSet<Principal>(subject.getPrincipals());
        for (Principal principal : principals) {
            if (principal.getClass().equals(KeycloakPrincipal.class) || principal.getClass().equals(RolePrincipal.class)) {
                subject.getPrincipals().remove(principal);
            }
        }
        Set<Object> creds = subject.getPrivateCredentials();
        for (Object cred : creds) {
            subject.getPrivateCredentials().remove(cred);
        }
        subject = null;
        callbackHandler = null;
        return true;
    }

    private static class RolePrincipal implements Principal, Serializable {
        private static final long serialVersionUID = -5538962177019315447L;
        private String roleName = null;

        public RolePrincipal(String roleName) {
            this.roleName = roleName;
        }

        public boolean equals (Object p) {
            if (! (p instanceof RolePrincipal))
                return false;
            return getName().equals(((RolePrincipal)p).getName());
        }

        public int hashCode () {
            return getName().hashCode();
        }

        public String getName () {
            return this.roleName;
        }

        public String toString ()
        {
            return getName();
        }
    }

    public static class Auth {
        private final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal;
        private final Set<String> roles;
        private final String tokenString;

        public Auth(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal, Set<String> roles, String accessToken) {
            this.principal = principal;
            this.roles = roles;
            this.tokenString = accessToken;
        }

        public KeycloakPrincipal<RefreshableKeycloakSecurityContext> getPrincipal() {
            return principal;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public String getTokenString() {
            return tokenString;
        }
    }
}
