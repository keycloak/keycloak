package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocketFactory;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.tracing.TracingProvider;
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
    private LdapContext ldapContext;

    public LDAPContextManager(KeycloakSession session, LDAPConfig connectionProperties) {
        this.session = session;
        this.ldapConfig = connectionProperties;

        String useTruststoreSpi = connectionProperties.getUseTruststoreSpi();
        if (useTruststoreSpi != null && !useTruststoreSpi.equals(LDAPConstants.USE_TRUSTSTORE_NEVER)) {
            // Initialize LDAP socket factory that utilizes TrustStore SPI and KeyStore SPI.
            LDAPSSLSocketFactory.initialize(session);
        }
    }

    public static LDAPContextManager create(KeycloakSession session, LDAPConfig connectionProperties) {
        return new LDAPContextManager(session, connectionProperties);
    }

    // Create connection and authenticate as admin user.
    private void createLdapContext() throws NamingException {
        var tracing = session.getProvider(TracingProvider.class);
        tracing.startSpan(LDAPContextManager.class, "createLdapContext");
        try {
            // Create connection but avoid triggering automatic bind request by not setting security principal and credentials yet.
            // That allows us to send optional StartTLS request before binding.
            ldapContext = new InitialLdapContext(LDAPContextManager.getNonAuthConnectionProperties(ldapConfig), null);

            // Send StartTLS request and setup SSL context if needed.
            if (ldapConfig.isStartTls()) {
                SSLSocketFactory sslSocketFactory = null;
                if (LDAPUtil.shouldUseTruststoreSpi(ldapConfig)) {
                    sslSocketFactory = LDAPSSLSocketFactory.getDefault();
                }

                tlsResponse = startTLS(ldapContext, sslSocketFactory);

                // Exception should be already thrown by LDAPContextManager.startTLS if "startTLS" could not be established, but rather do some additional check
                if (tlsResponse == null) {
                    throw new NamingException("Wasn't able to establish LDAP connection through StartTLS");
                }
            }

            setAdminConnectionAuthProperties(ldapContext);
            if (!LDAPConstants.AUTH_TYPE_NONE.equals(ldapConfig.getAuthType())) {
                // Explicitly send bind with given credentials.
                // Throws AuthenticationException when authentication fails.
                ldapContext.reconnect(null);
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

    // Get bind password from vault or from directly from configuration, may be null.
    private String getBindPassword() {
        VaultStringSecret vaultSecret = session.vault().getStringSecret(ldapConfig.getBindCredential());
        return vaultSecret.get().orElse(ldapConfig.getBindCredential());
    }

    public static StartTlsResponse startTLS(LdapContext ldapContext, SSLSocketFactory sslSocketFactory) throws NamingException {
        StartTlsResponse tls = null;

        try {
            tls = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
            tls.negotiate(sslSocketFactory);
        } catch (Exception e) {
            logger.error("Could not negotiate TLS", e);
            NamingException ne = new AuthenticationException("Could not negotiate TLS");
            ne.setRootCause(e);
            throw ne;
        }

        return tls;
    }

    // Fill in the connection properties for admin connection
    private void setAdminConnectionAuthProperties(LdapContext ldapContext) throws NamingException {
        String authType = ldapConfig.getAuthType();
        if (authType != null) {
            ldapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, authType);
        }

        String bindPassword = getBindPassword();
        if (bindPassword != null) {
            ldapContext.addToEnvironment(SECURITY_CREDENTIALS, bindPassword);
        }

        String bindDN = ldapConfig.getBindDN();
        if (bindDN != null) {
            ldapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, bindDN);
        }

        if (logger.isDebugEnabled()) {
            Map<Object, Object> copyEnv = new Hashtable<>(ldapContext.getEnvironment());
            if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
                copyEnv.put(Context.SECURITY_CREDENTIALS, "**************************************");
            }
            logger.debugf("Creating LdapContext using properties: [%s]", copyEnv);
        }
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
            env.put("java.naming.ldap.factory.socket", "org.keycloak.storage.ldap.idm.store.ldap.LDAPSSLSocketFactory");
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
