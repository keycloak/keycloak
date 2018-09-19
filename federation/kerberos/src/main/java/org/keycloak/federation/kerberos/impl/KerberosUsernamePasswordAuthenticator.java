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

package org.keycloak.federation.kerberos.impl;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KerberosJdkProvider;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.ModelException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosUsernamePasswordAuthenticator {

    private static final Logger logger = Logger.getLogger(KerberosUsernamePasswordAuthenticator.class);

    protected final CommonKerberosConfig config;
    private LoginContext loginContext;

    public KerberosUsernamePasswordAuthenticator(CommonKerberosConfig config) {
        this.config = config;
    }


    /**
     * Returns true if user with given username exists in kerberos database
     *
     * @param username username without Kerberos realm attached or with correct realm attached
     * @return true if user available
     */
    public boolean isUserAvailable(String username) {
        logger.debugf("Checking existence of user: %s", username);
        try {
            String principal = getKerberosPrincipal(username);
            loginContext = new LoginContext("does-not-matter", null,
                    createJaasCallbackHandler(principal, "fake-password-which-nobody-has"),
                    createJaasConfiguration());

            loginContext.login();

            throw new IllegalStateException("Didn't expect to end here");
        } catch (LoginException le) {
            String message = le.getMessage();
            logger.debugf("Message from kerberos: %s", message);

            checkKerberosServerAvailable(le);

            // Bit cumbersome, but seems to work with tested kerberos servers
            boolean exists = (!message.contains("Client not found"));
            return exists;
        }
    }


    /**
     * Returns true if user was successfully authenticated against Kerberos
     *
     * @param username username without Kerberos realm attached or with correct realm attached
     * @param password kerberos password
     * @return  true if user was successfully authenticated
     */
    public boolean validUser(String username, String password) {
        try {
            authenticateSubject(username, password);
            logoutSubject();
            return true;
        } catch (LoginException le) {
            checkKerberosServerAvailable(le);

            logger.debug("Failed to authenticate user " + username, le);
            return false;
        }
    }

    protected void checkKerberosServerAvailable(LoginException le) {
        String message = le.getMessage().toUpperCase();
        if (message.contains("PORT UNREACHABLE") ||
            message.contains("CANNOT LOCATE") ||
            message.contains("CANNOT CONTACT") ||
            message.contains("CANNOT FIND") ||
            message.contains("UNKNOWN ERROR")) {
            throw new ModelException("Kerberos unreachable", le);
        }
    }


    /**
     * Returns true if user was successfully authenticated against Kerberos
     *
     * @param username username without Kerberos realm attached
     * @param password kerberos password
     * @return  true if user was successfully authenticated
     */
    public Subject authenticateSubject(String username, String password) throws LoginException {
        String principal = getKerberosPrincipal(username);

        logger.debug("Validating password of principal: " + principal);
        loginContext = new LoginContext("does-not-matter", null,
                createJaasCallbackHandler(principal, password),
                createJaasConfiguration());

        loginContext.login();
        logger.debug("Principal " + principal + " authenticated succesfully");
        return loginContext.getSubject();
    }


    public void logoutSubject() {
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException le) {
                logger.error("Failed to logout kerberos subject", le);
            }
        }
    }


    protected String getKerberosPrincipal(String username) throws LoginException {
        if (username.contains("@")) {
            String[] tokens = username.split("@");

            String kerberosRealm = tokens[1];
            if (!kerberosRealm.toUpperCase().equals(config.getKerberosRealm())) {
                logger.warn("Invalid kerberos realm. Expected realm: " + config.getKerberosRealm() + ", username: " + username);
                throw new LoginException("Client not found");
            }

            username = tokens[0];
        }

        return username + "@" + config.getKerberosRealm();
    }


    protected CallbackHandler createJaasCallbackHandler(final String principal, final String password) {
        return new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName(principal);
                    } else if (callback instanceof PasswordCallback) {
                        PasswordCallback passwordCallback = (PasswordCallback) callback;
                        passwordCallback.setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(callback, "Unsupported callback: " + callback.getClass().getCanonicalName());
                    }
                }
            }
        };
    }


    protected Configuration createJaasConfiguration() {
        return KerberosJdkProvider.getProvider().createJaasConfigurationForUsernamePasswordLogin(config.isDebug());
    }
}
