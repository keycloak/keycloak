/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.jaas;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.RSATokenVerifier;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.FindFile;
import org.keycloak.representations.AccessToken;
import org.keycloak.common.util.reflections.Reflections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKeycloakLoginModule implements LoginModule {

    public static final String KEYCLOAK_CONFIG_FILE_OPTION = "keycloak-config-file";
    public static final String ROLE_PRINCIPAL_CLASS_OPTION = "role-principal-class";

    protected Subject subject;
    protected CallbackHandler callbackHandler;
    protected Auth auth;
    protected KeycloakDeployment deployment;
    protected String rolePrincipalClass;

    // This is to avoid parsing keycloak.json file in each request. Key is file location, Value is parsed keycloak deployment
    private static ConcurrentMap<String, KeycloakDeployment> deployments = new ConcurrentHashMap<String, KeycloakDeployment>();

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        String configFile = (String)options.get(KEYCLOAK_CONFIG_FILE_OPTION);
        rolePrincipalClass = (String)options.get(ROLE_PRINCIPAL_CLASS_OPTION);
        getLogger().debug("Declared options: " + KEYCLOAK_CONFIG_FILE_OPTION + "=" + configFile + ", " + ROLE_PRINCIPAL_CLASS_OPTION + "=" + rolePrincipalClass);

        if (configFile != null) {
            deployment = deployments.get(configFile);
            if (deployment == null) {
                // lazy init of our deployment
                deployment = resolveDeployment(configFile);
                deployments.putIfAbsent(configFile, deployment);
            }
        }
    }

    protected KeycloakDeployment resolveDeployment(String keycloakConfigFile) {
        try {
            InputStream is = FindFile.findFile(keycloakConfigFile);
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(is);
            if (kd.getRealmKey() == null) {
                new AdapterDeploymentContext().resolveRealmKey(kd);
            }
            return kd;
        } catch (RuntimeException e) {
            getLogger().debug("Unable to find or parse file " + keycloakConfigFile + " due to " + e.getMessage(), e);
            throw e;
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

            Auth auth = doAuth(username, password);
            if (auth != null) {
                this.auth = auth;
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedCallbackException uce) {
            getLogger().warn("Error: " + uce.getCallback().toString()
                    + " not available to gather authentication information from the user");
            return false;
        } catch (Exception e) {
            LoginException le = new LoginException(e.toString());
            le.initCause(e);
            throw le;
        }
    }


    @Override
    public boolean commit() throws LoginException {
        if (auth == null) {
            return false;
        }

        this.subject.getPrincipals().add(auth.getPrincipal());
        this.subject.getPrivateCredentials().add(auth.getTokenString());
        if (auth.getRoles() != null) {
            for (String roleName : auth.getRoles()) {
                Principal rolePrinc = createRolePrincipal(roleName);
                this.subject.getPrincipals().add(rolePrinc);
            }
        }

        return true;
    }


    protected Principal createRolePrincipal(String roleName) {
        if (rolePrincipalClass != null && rolePrincipalClass.length() > 0) {
            try {
                Class<Principal> clazz = Reflections.classForName(rolePrincipalClass, getClass().getClassLoader());
                Constructor<Principal> constructor = clazz.getDeclaredConstructor(String.class);
                return constructor.newInstance(roleName);
            } catch (Exception e) {
                getLogger().warn("Unable to create declared roleClass " + rolePrincipalClass + " due to " + e.getMessage());
            }
        }

        // Fallback to default rolePrincipal class
        return new RolePrincipal(roleName);
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


    protected Auth bearerAuth(String tokenString) throws VerificationException {
        AccessToken token = RSATokenVerifier.verifyToken(tokenString, deployment.getRealmKey(), deployment.getRealmInfoUrl());

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


    protected abstract Auth doAuth(String username, String password) throws Exception;

    protected abstract Logger getLogger();


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
