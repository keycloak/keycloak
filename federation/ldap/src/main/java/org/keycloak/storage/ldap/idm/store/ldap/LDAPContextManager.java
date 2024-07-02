package org.keycloak.storage.ldap.idm.store.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.vault.VaultStringSecret;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

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
    }

    public static LDAPContextManager create(KeycloakSession session, LDAPConfig connectionProperties) {
        return new LDAPContextManager(session, connectionProperties);
    }

    // Create connection that is authenticated as admin user.
    private void createLdapContext() throws NamingException {
        // Create the LDAP context without setting the security principal and credentials yet.
        // This avoids triggering an automatic bind request, allowing us to send an optional StartTLS request before binding.
        ldapContext = new InitialLdapContext(getNonAuthConnectionProperties(ldapConfig), null);

        // Send StartTLS request and setup SSL context if needed.
        if (ldapConfig.isStartTls()) {
            SSLSocketFactory sslSocketFactory = null;
            if (LDAPUtil.shouldUseTruststoreSpi(ldapConfig)) {
                TruststoreProvider provider = session.getProvider(TruststoreProvider.class);
                sslSocketFactory = provider.getSSLSocketFactory();
            }

            tlsResponse = startTLS(ldapContext, sslSocketFactory);

            // Exception should be already thrown by LDAPContextManager.startTLS if "startTLS" could not be established, but rather do some additional check
            if (tlsResponse == null) {
                throw new NamingException("Wasn't able to establish LDAP connection through StartTLS");
            }
        }

        setAdminConnectionAuthProperties(ldapContext);

        // Bind will be automatically called when operations are executed on the context,
        // or it can be explicitly called by invoking the reconnect() method (e.g., authentication test in LDAPServerCapabilitiesManager.testLDAP()).
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

    // Fill in the connection properties to authenticate as admin.
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
