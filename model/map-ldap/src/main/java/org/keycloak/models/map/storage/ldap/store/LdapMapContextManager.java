/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.store;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.map.storage.ldap.config.LdapMapConfig;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.vault.VaultCharSecret;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static javax.naming.Context.SECURITY_CREDENTIALS;

/**
 * @author mhajas
 */
public final class LdapMapContextManager implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LdapMapContextManager.class);

    private final KeycloakSession session;
    private final LdapMapConfig ldapMapConfig;
    private StartTlsResponse tlsResponse;

    private  VaultCharSecret vaultCharSecret = new VaultCharSecret() {
        @Override
        public Optional<CharBuffer> get() {
            return Optional.empty();
        }

        @Override
        public Optional<char[]> getAsArray() {
            return Optional.empty();
        }

        @Override
        public void close() {

        }
    };

    private LdapContext ldapContext;

    public LdapMapContextManager(KeycloakSession session, LdapMapConfig connectionProperties) {
        this.session = session;
        this.ldapMapConfig = connectionProperties;
    }

    public static LdapMapContextManager create(KeycloakSession session, LdapMapConfig connectionProperties) {
        return new LdapMapContextManager(session, connectionProperties);
    }

    private void createLdapContext() throws NamingException {
        Hashtable<Object, Object> connProp = getConnectionProperties(ldapMapConfig);

        if (!LDAPConstants.AUTH_TYPE_NONE.equals(ldapMapConfig.getAuthType())) {
            vaultCharSecret = getVaultSecret();

            if (vaultCharSecret != null && !ldapMapConfig.isStartTls()) {
                connProp.put(SECURITY_CREDENTIALS, vaultCharSecret.getAsArray()
                        .orElse(ldapMapConfig.getBindCredential().toCharArray()));
            }
        }

        ldapContext = new InitialLdapContext(connProp, null);
        if (ldapMapConfig.isStartTls()) {
            SSLSocketFactory sslSocketFactory = null;
            String useTruststoreSpi = ldapMapConfig.getUseTruststoreSpi();
            if (useTruststoreSpi != null && useTruststoreSpi.equals(LDAPConstants.USE_TRUSTSTORE_ALWAYS)) {
                TruststoreProvider provider = session.getProvider(TruststoreProvider.class);
                sslSocketFactory = provider.getSSLSocketFactory();
            }

            tlsResponse = startTLS(ldapContext, ldapMapConfig.getAuthType(), ldapMapConfig.getBindDN(),
                    vaultCharSecret.getAsArray().orElse(ldapMapConfig.getBindCredential().toCharArray()), sslSocketFactory);

            // Exception should be already thrown by LDAPContextManager.startTLS if "startTLS" could not be established, but rather do some additional check
            if (tlsResponse == null) {
                throw new NamingException("Wasn't able to establish LDAP connection through StartTLS");
            }
        }
    }

    public LdapContext getLdapContext() throws NamingException {
        if (ldapContext == null) createLdapContext();

        return ldapContext;
    }

    private VaultCharSecret getVaultSecret() {
        return LDAPConstants.AUTH_TYPE_NONE.equals(ldapMapConfig.getAuthType())
                ? null
                : session.vault().getCharSecret(ldapMapConfig.getBindCredential());
    }

    public static StartTlsResponse startTLS(LdapContext ldapContext, String authType, String bindDN, char[] bindCredential, SSLSocketFactory sslSocketFactory) throws NamingException {
        StartTlsResponse tls;

        try {
            tls = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
            tls.negotiate(sslSocketFactory);

            ldapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, authType);

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                ldapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, bindDN);
                ldapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, bindCredential);
            }
        } catch (Exception e) {
            logger.error("Could not negotiate TLS", e);
            throw new AuthenticationException("Could not negotiate TLS");
        }

        // throws AuthenticationException when authentication fails
        ldapContext.lookup("");

        return tls;
    }

    // Get connection properties of admin connection
    private Hashtable<Object, Object> getConnectionProperties(LdapMapConfig ldapMapConfig) {
        Hashtable<Object, Object> env = getNonAuthConnectionProperties(ldapMapConfig);

        if(!ldapMapConfig.isStartTls()) {
            String authType = ldapMapConfig.getAuthType();

            env.put(Context.SECURITY_AUTHENTICATION, authType);

            String bindDN = ldapMapConfig.getBindDN();

            char[] bindCredential = null;

            if (ldapMapConfig.getBindCredential() != null) {
                bindCredential = ldapMapConfig.getBindCredential().toCharArray();
            }

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                env.put(Context.SECURITY_PRINCIPAL, bindDN);
                env.put(Context.SECURITY_CREDENTIALS, bindCredential);
            }
        }

        if (logger.isTraceEnabled()) {
            Map<Object, Object> copyEnv = new Hashtable<>(env);
            if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
                copyEnv.put(Context.SECURITY_CREDENTIALS, "**************************************");
            }
            logger.tracef("Creating LdapContext using properties: [%s]", copyEnv);
        }

        return env;
    }


    /**
     * This method is used for admin connection and user authentication. Hence it returns just connection properties NOT related to
     * authentication (properties like bindType, bindDn, bindPassword). Caller of this method needs to fill auth-related connection properties
     * based on the fact whether he does admin connection or user authentication
     *
     */
    public static Hashtable<Object, Object> getNonAuthConnectionProperties(LdapMapConfig ldapMapConfig) {
        HashMap<String, Object> env = new HashMap<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapMapConfig.getFactoryName());

        String url = ldapMapConfig.getConnectionUrl();

        if (url != null) {
            env.put(Context.PROVIDER_URL, url);
        } else {
            logger.warn("LDAP URL is null. LDAPOperationManager won't work correctly");
        }

        // when using Start TLS, use default socket factory for LDAP client but pass the TrustStore SSL socket factory later
        // when calling StartTlsResponse.negotiate(trustStoreSSLSocketFactory)
        if (!ldapMapConfig.isStartTls()) {
            String useTruststoreSpi = ldapMapConfig.getUseTruststoreSpi();
            LDAPConstants.setTruststoreSpiIfNeeded(useTruststoreSpi, url, env);
        }

        String connectionPooling = ldapMapConfig.getConnectionPooling();
        if (connectionPooling != null) {
            env.put("com.sun.jndi.ldap.connect.pool", connectionPooling);
        }

        String connectionTimeout = ldapMapConfig.getConnectionTimeout();
        if (connectionTimeout != null && !connectionTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
        }

        String readTimeout = ldapMapConfig.getReadTimeout();
        if (readTimeout != null && !readTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        }

        // Just dump the additional properties
        Properties additionalProperties = ldapMapConfig.getAdditionalConnectionProperties();
        if (additionalProperties != null) {
            for (Object key : additionalProperties.keySet()) {
                env.put(key.toString(), additionalProperties.getProperty(key.toString()));
            }
        }

        StringBuilder binaryAttrsBuilder = new StringBuilder();
        if (ldapMapConfig.isObjectGUID()) {
            binaryAttrsBuilder.append(LDAPConstants.OBJECT_GUID).append(" ");
        }
        if (ldapMapConfig.isEdirectory()) {
            binaryAttrsBuilder.append(LDAPConstants.NOVELL_EDIRECTORY_GUID).append(" ");
        }
        for (String attrName : ldapMapConfig.getBinaryAttributeNames()) {
            binaryAttrsBuilder.append(attrName).append(" ");
        }

        String binaryAttrs = binaryAttrsBuilder.toString().trim();
        if (!binaryAttrs.isEmpty()) {
            env.put("java.naming.ldap.attributes.binary", binaryAttrs);
        }

        return new Hashtable<>(env);
    }

    @Override
    public void close() {
        if (vaultCharSecret != null) vaultCharSecret.close();
        if (tlsResponse != null) {
            try {
                tlsResponse.close();
            } catch (IOException e) {
                logger.error("Could not close Ldap tlsResponse.", e);
            }
        }

        if (ldapContext != null) {
            try {
                ldapContext.close();
            } catch (NamingException e) {
                logger.error("Could not close Ldap context.", e);
            }
        }
    }
}
