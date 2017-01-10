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
package org.keycloak.services.managers;

import org.jboss.logging.Logger;
import org.keycloak.models.LDAPConstants;
import org.keycloak.services.ServicesLogger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import java.util.Hashtable;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPConnectionTestManager {

    private static final Logger logger = Logger.getLogger(LDAPConnectionTestManager.class);

    public static final String TEST_CONNECTION = "testConnection";
    public static final String TEST_AUTHENTICATION = "testAuthentication";

    public boolean testLDAP(String action, String connectionUrl, String bindDn, String bindCredential, String useTruststoreSpi, String connectionTimeout) {
        if (!TEST_CONNECTION.equals(action) && !TEST_AUTHENTICATION.equals(action)) {
            ServicesLogger.LOGGER.unknownAction(action);
            return false;
        }

        Context ldapContext = null;
        try {
            Hashtable<String, Object> env = new Hashtable<String, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

            if (connectionUrl == null) {
                logger.errorf("Unknown connection URL");
                return false;
            }
            env.put(Context.PROVIDER_URL, connectionUrl);

            if (TEST_AUTHENTICATION.equals(action)) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");

                if (bindDn == null) {
                    logger.error("Unknown bind DN");
                    return false;
                }
                env.put(Context.SECURITY_PRINCIPAL, bindDn);

                char[] bindCredentialChar = null;
                if (bindCredential != null) {
                    bindCredentialChar = bindCredential.toCharArray();
                }
                env.put(Context.SECURITY_CREDENTIALS, bindCredentialChar);
            }

            LDAPConstants.setTruststoreSpiIfNeeded(useTruststoreSpi, connectionUrl, env);

            if (connectionTimeout != null && !connectionTimeout.isEmpty()) {
                env.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
            }

            ldapContext = new InitialLdapContext(env, null);
            return true;
        } catch (Exception ne) {
            String errorMessage = (TEST_AUTHENTICATION.equals(action)) ? "Error when authenticating to LDAP: " : "Error when connecting to LDAP: ";
            ServicesLogger.LOGGER.errorAuthenticating(ne, errorMessage + ne.getMessage());
            return false;
        } finally {
            if (ldapContext != null) {
                try {
                    ldapContext.close();
                } catch (NamingException ne) {
                    ServicesLogger.LOGGER.errorClosingLDAP(ne);
                }
            }
        }
    }
}
