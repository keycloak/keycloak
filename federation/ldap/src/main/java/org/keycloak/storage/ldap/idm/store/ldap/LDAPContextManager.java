package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocketFactory;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.tracing.TracingProvider;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.vault.VaultStringSecret;

import org.jboss.logging.Logger;

import static javax.naming.Context.SECURITY_CREDENTIALS;

/**
 * @author mhajas
 */
public final class LDAPContextManager implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LDAPContextManager.class);

    private final KeycloakSession session;
    private final LDAPConfig ldapConfig;
    private StartTlsResponse tlsResponse;

    private VaultStringSecret vaultStringSecret = new VaultStringSecret() {
        @Override
        public Optional<String> get() {
            return Optional.empty();
        }

        @Override
        public void close() {

        }
    };

    private LdapContext ldapContext;

    public LDAPContextManager(KeycloakSession session, LDAPConfig connectionProperties) {
        this.session = session;
        this.ldapConfig = connectionProperties;
    }

    public static LDAPContextManager create(KeycloakSession session, LDAPConfig connectionProperties) {
        return new LDAPContextManager(session, connectionProperties);
    }

    private void createLdapContext() throws NamingException {
        var tracing = session.getProvider(TracingProvider.class);
        tracing.startSpan(LDAPContextManager.class, "createLdapContext");
        try {
            Hashtable<Object, Object> connProp = getConnectionProperties(ldapConfig);

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(ldapConfig.getAuthType())) {
                vaultStringSecret = getVaultSecret();

                if (vaultStringSecret != null && !ldapConfig.isStartTls() && ldapConfig.getBindCredential() != null) {
                    connProp.put(SECURITY_CREDENTIALS, vaultStringSecret.get()
                            .orElse(ldapConfig.getBindCredential()).toCharArray());
                }
            }

            if (ldapConfig.isConnectionTrace()) {
                connProp.put(LDAPConstants.CONNECTION_TRACE_BER, System.err);
            }

            ldapContext = new SessionBoundInitialLdapContext(session, connProp, null);
            if (ldapConfig.isStartTls()) {
                SSLSocketFactory sslSocketFactory = null;
                if (LDAPUtil.shouldUseTruststoreSpi(ldapConfig)) {
                    TruststoreProvider provider = session.getProvider(TruststoreProvider.class);
                    sslSocketFactory = provider.getSSLSocketFactory();
                }

                tlsResponse = startTLS(ldapContext, ldapConfig.getAuthType(), ldapConfig.getBindDN(),
                        vaultStringSecret.get().orElse(ldapConfig.getBindCredential()), sslSocketFactory);

                // Exception should be already thrown by LDAPContextManager.startTLS if "startTLS" could not be established, but rather do some additional check
                if (tlsResponse == null) {
                    throw new NamingException("Wasn't able to establish LDAP connection through StartTLS");
                }
            }
        } catch (NamingException e) {
            tracing.error(e);
            throw e;
        } finally {
            tracing.endSpan();
        }
    }

    public LdapContext getLdapContext() throws NamingException {
        if (ldapContext == null) createLdapContext();

        return ldapContext;
    }

    private VaultStringSecret getVaultSecret() {
        return LDAPConstants.AUTH_TYPE_NONE.equals(ldapConfig.getAuthType())
                ? null
                : session.vault().getStringSecret(ldapConfig.getBindCredential());
    }

    public static StartTlsResponse startTLS(LdapContext ldapContext, String authType, String bindDN, String bindCredential, SSLSocketFactory sslSocketFactory) throws NamingException {
        StartTlsResponse tls = null;

        try {
            tls = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
            tls.negotiate(sslSocketFactory);

            ldapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, authType);

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                ldapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, bindDN);
                ldapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, bindCredential != null ? bindCredential.toCharArray() : null);
            }
        } catch (Exception e) {
            logger.error("Could not negotiate TLS", e);
            NamingException ne = new AuthenticationException("Could not negotiate TLS");
            ne.setRootCause(e);
            throw ne;
        }

        // throws AuthenticationException when authentication fails
        ldapContext.lookup("");

        return tls;
    }

    // Get connection properties of admin connection
    private Hashtable<Object, Object> getConnectionProperties(LDAPConfig ldapConfig) {
        Hashtable<Object, Object> env = getNonAuthConnectionProperties(ldapConfig);

        if(!ldapConfig.isStartTls()) {
            String authType = ldapConfig.getAuthType();

            if (authType != null) env.put(Context.SECURITY_AUTHENTICATION, authType);

            String bindDN = ldapConfig.getBindDN();

            char[] bindCredential = null;

            if (ldapConfig.getBindCredential() != null) {
                bindCredential = ldapConfig.getBindCredential().toCharArray();
            }

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                if (bindDN != null) env.put(Context.SECURITY_PRINCIPAL, bindDN);
                if (bindCredential != null) env.put(Context.SECURITY_CREDENTIALS, bindCredential);
            }
        }

        if (logger.isDebugEnabled()) {
            Map<Object, Object> copyEnv = new Hashtable<>(env);
            if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
                copyEnv.put(Context.SECURITY_CREDENTIALS, "**************************************");
            }
            logger.debugf("Creating LdapContext using properties: [%s]", copyEnv);
        }

        return env;
    }


    /**
     * This method is used for admin connection and user authentication. Hence it returns just connection properties NOT related to
     * authentication (properties like bindType, bindDn, bindPassword). Caller of this method needs to fill auth-related connection properties
     * based on the fact whether he does admin connection or user authentication
     *
     * @param ldapConfig
     * @return
     */
    public static Hashtable<Object, Object> getNonAuthConnectionProperties(LDAPConfig ldapConfig) {
        HashMap<String, Object> env = new HashMap<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapConfig.getFactoryName());

        String url = ldapConfig.getConnectionUrl();

        if (url != null) {
            env.put(Context.PROVIDER_URL, url);
        } else {
            logger.warn("LDAP URL is null. LDAPOperationManager won't work correctly");
        }

        // when using Start TLS, use default socket factory for LDAP client but pass the TrustStore SSL socket factory later
        // when calling StartTlsResponse.negotiate(trustStoreSSLSocketFactory)
        if (!ldapConfig.isStartTls() && LDAPUtil.shouldUseTruststoreSpi(ldapConfig)) {
            env.put("java.naming.ldap.factory.socket", "org.keycloak.truststore.SSLSocketFactory");
        }

        String connectionPooling = ldapConfig.getConnectionPooling();
        if (connectionPooling != null) {
            env.put("com.sun.jndi.ldap.connect.pool", connectionPooling);
        }

        String connectionTimeout = ldapConfig.getConnectionTimeout();
        if (connectionTimeout != null && !connectionTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
        }

        String readTimeout = ldapConfig.getReadTimeout();
        if (readTimeout != null && !readTimeout.isEmpty()) {
            env.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        }

        // Just dump the additional properties
        Properties additionalProperties = ldapConfig.getAdditionalConnectionProperties();
        if (additionalProperties != null) {
            for (Object key : additionalProperties.keySet()) {
                env.put(key.toString(), additionalProperties.getProperty(key.toString()));
            }
        }

        StringBuilder binaryAttrsBuilder = new StringBuilder();
        if (ldapConfig.isObjectGUID()) {
            binaryAttrsBuilder.append(LDAPConstants.OBJECT_GUID).append(" ");
        }
        if (ldapConfig.isEdirectory()) {
            binaryAttrsBuilder.append(LDAPConstants.NOVELL_EDIRECTORY_GUID).append(" ");
        }
        for (String attrName : ldapConfig.getBinaryAttributeNames()) {
            binaryAttrsBuilder.append(attrName).append(" ");
        }

        String binaryAttrs = binaryAttrsBuilder.toString().trim();
        if (!binaryAttrs.isEmpty()) {
            env.put("java.naming.ldap.attributes.binary", binaryAttrs);
        }

        String referral = ldapConfig.getReferral();
        if (referral != null) {
            env.put(Context.REFERRAL, referral);
        }

        return new Hashtable<>(env);
    }

    @Override
    public void close() {
        if (vaultStringSecret != null) vaultStringSecret.close();
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
