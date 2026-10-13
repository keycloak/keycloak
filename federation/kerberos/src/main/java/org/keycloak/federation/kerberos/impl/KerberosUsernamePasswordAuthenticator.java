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

import java.io.IOException;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosJdkProvider;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosPrincipal;
import org.keycloak.models.ModelException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosUsernamePasswordAuthenticator extends KerberosServerSubjectAuthenticator {

    private static final Logger logger = Logger.getLogger(KerberosUsernamePasswordAuthenticator.class);

    private LoginContext loginContext;
    private final boolean disableKerberosAuthenticationRoundTrip;

    public KerberosUsernamePasswordAuthenticator(CommonKerberosConfig config) {
        this(config, false);
    }

    @Deprecated(since = "26.8.1", forRemoval = true)
    public KerberosUsernamePasswordAuthenticator(CommonKerberosConfig config, boolean disableKerberosAuthenticationRoundTrip) {
        super(config);
        this.disableKerberosAuthenticationRoundTrip = disableKerberosAuthenticationRoundTrip;
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
            checkKerberosUsername(le);

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
            Subject clientSubject = authenticateSubject(username, password);
            if (disableKerberosAuthenticationRoundTrip) {
                logger.warnf("Kerberos authentication round-trip is disabled (disable-kerberos-authentication-round-trip). This behavior is deprecated and will be removed in future releases.");
                return true;
            }
            Subject serverSubject = authenticateServerSubject();
            return validateKerberosServiceTicketRoundTrip(clientSubject, serverSubject);
        } catch (LoginException le) {
            checkKerberosServerAvailable(le);
            checkKerberosUsername(le);

            logger.debug("Failed to authenticate user " + username, le);
            return false;
        } finally {
            logoutSubject();
            logoutServerSubject();
        }
    }

    protected boolean validateKerberosServiceTicketRoundTrip(Subject clientSubject, Subject serverSubject) {
        KerberosClient client = null;
        KerberosServer server = null;
        try {
            GSSManager manager = GSSManager.getInstance();
            client = new KerberosClient(manager, clientSubject, config.getServerPrincipal());
            server = new KerberosServer(manager, serverSubject, config.getServerPrincipal());
            byte[] token = new byte[0];
            while (token != null && (!client.isEstablised() || !server.isEstablised())) {
                token = client.processToken(token);
                if (token != null) {
                    token = server.processToken(token);
                }
            }
            return client.isEstablised() && server.isEstablised();
        } catch (LoginException e) {
            return false;
        } finally {
            if (client != null) {
                client.dispose();
            }
            if (server != null) {
                server.dispose();
            }
        }
    }

    protected void checkKerberosServerAvailable(LoginException le) {
        if (le.getMessage() != null) {
            String message = le.getMessage().toUpperCase();
            if (message.contains("PORT UNREACHABLE") ||
                message.contains("CANNOT LOCATE") ||
                message.contains("CANNOT CONTACT") ||
                message.contains("CANNOT FIND") ||
                message.contains("UNKNOWN ERROR") ||
                message.contains("RECEIVE TIMED OUT")) {
                throw new ModelException("Kerberos unreachable", le);
            }
        } else if (le.getCause() instanceof IOException) {
            // for example, a PortUnreachable exception if the server is not running
            throw new ModelException("Kerberos unreachable", le);
        }
    }

    protected void checkKerberosUsername(LoginException le) {
        if (le.getMessage() != null) {
            String message = le.getMessage();
            if (message.contains("IllegalArgumentException")) {
                throw new ModelException("Kerberos illegal username", le);
            }
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
        logger.debug("Principal " + principal + " authenticated successfully");
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


    public String getKerberosPrincipal(String username) throws LoginException {
        if (username.contains("@")) {
            return new KerberosPrincipal(username).toString();
        } else {
            return username + "@" + config.getKerberosRealm();
        }
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

    private static class KerberosContext {

        private final GSSCredential credential;
        private final GSSContext context;

        public KerberosContext(GSSCredential credential, GSSContext context) {
            this.credential = credential;
            this.context = context;
        }

        public GSSCredential getCredential() {
            return credential;
        }

        public GSSContext getContext() {
            return context;
        }

        public void dispose() {
            dispose(credential);
            dispose(context);
        }

        public static void dispose(GSSCredential credential) {
            if (credential != null) {
                try {
                    credential.dispose();
                } catch (GSSException e) {
                    //no-op
                }
            }
        }

        public static void dispose(GSSContext context) {
            if (context != null) {
                try {
                    context.dispose();
                } catch (GSSException e) {
                    //no-op
                }
            }
        }
    }

    private static class KerberosClient {

        private final Subject clientSubject;
        private final KerberosContext clientContext;

        public KerberosClient(GSSManager manager, Subject clientSubject, String serverName) throws LoginException {
            this.clientSubject = clientSubject;
            this.clientContext = Subject.doAs(clientSubject, (PrivilegedAction<KerberosContext>) () -> {
                GSSCredential credential = null;
                GSSContext context = null;
                try {
                    GSSName target = manager.createName(serverName, KerberosConstants.KRB5_NAME_OID, KerberosConstants.KRB5_OID);
                    credential = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, KerberosConstants.KRB5_OID, GSSCredential.INITIATE_ONLY);
                    context = manager.createContext(target, KerberosConstants.KRB5_OID, credential, GSSContext.DEFAULT_LIFETIME);
                    context.requestMutualAuth(true);
                    return new KerberosContext(credential, context);
                } catch (GSSException e) {
                    logger.warn("Error creating GSS context for the client", e);
                    KerberosContext.dispose(credential);
                    KerberosContext.dispose(context);
                    return null;
                }
            });
            if (clientContext == null) {
                throw new LoginException("Error creating GSS context for the client");
            }
        }

        public byte[] processToken(byte[] token) {
            return Subject.doAs(clientSubject, (PrivilegedAction<byte[]>) () -> {
                try {
                    return clientContext.getContext().initSecContext(token, 0, token.length);
                } catch (GSSException e) {
                    logger.debug("Exception performing initSecContext", e);
                    return null;
                }
            });
        }

        public boolean isEstablised() {
            return clientContext.getContext().isEstablished();
        }

        public void dispose() {
            clientContext.dispose();
        }
    }

    private static class KerberosServer  {

        private final Subject serverSubject;
        private final KerberosContext serverContext;

        public KerberosServer(GSSManager manager, Subject serverSubject, String serverName) throws LoginException {
            this.serverSubject = serverSubject;
            this.serverContext = Subject.doAs(serverSubject, (PrivilegedAction<KerberosContext>) () -> {
                GSSCredential credential = null;
                GSSContext context = null;
                try {
                    GSSName acceptorName = manager.createName(serverName, KerberosConstants.KRB5_NAME_OID, KerberosConstants.KRB5_OID);
                    credential = manager.createCredential(acceptorName, GSSCredential.DEFAULT_LIFETIME, KerberosConstants.KRB5_OID, GSSCredential.ACCEPT_ONLY);
                    context = manager.createContext(credential);
                    return new KerberosContext(credential, context);
                } catch (GSSException e) {
                    logger.warn("Error creating GSS context for the server", e);
                    KerberosContext.dispose(credential);
                    KerberosContext.dispose(context);
                    return null;
                }
            });
            if (serverContext == null) {
                throw new LoginException("Error creating GSS context for the server");
            }
        }

        public byte[] processToken(byte[] token) {
            return Subject.doAs(serverSubject, (PrivilegedAction<byte[]>) () -> {
                try {
                    return serverContext.getContext().acceptSecContext(token, 0, token.length);
                } catch (GSSException e) {
                    logger.debug("Exception performing acceptSecContext", e);
                    return null;
                }
            });
        }

        public boolean isEstablised() {
            return serverContext.getContext().isEstablished();
        }

        public void dispose() {
            serverContext.dispose();
        }
    }
}
