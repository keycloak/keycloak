package org.keycloak.storage.ldap.idm.store.ldap;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.vault.VaultCharSecret;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
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
public final class LDAPContextManager implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(LDAPContextManager.class);

    private final KeycloakSession session;
    private final LDAPConfig ldapConfig;
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

    public LDAPContextManager(KeycloakSession session, LDAPConfig connectionProperties) {
        this.session = session;
        this.ldapConfig = connectionProperties;
    }

    public static LDAPContextManager create(KeycloakSession session, LDAPConfig connectionProperties) {
        return new LDAPContextManager(session, connectionProperties);
    }

    private void createLdapContext() throws NamingException {
        Hashtable<Object, Object> connProp = getConnectionProperties(ldapConfig);

        if (!LDAPConstants.AUTH_TYPE_NONE.equals(ldapConfig.getAuthType())) {
            vaultCharSecret = getVaultSecret();

            if (vaultCharSecret != null && !ldapConfig.isStartTls()) {
                connProp.put(SECURITY_CREDENTIALS, vaultCharSecret.getAsArray()
                        .orElse(ldapConfig.getBindCredential().toCharArray()));
            }
        }

        ldapContext = new InitialLdapContext(connProp, null);
        if (ldapConfig.isStartTls()) {
            tlsResponse = startTLS(ldapContext, ldapConfig.getAuthType(), ldapConfig.getBindDN(),
                    vaultCharSecret.getAsArray().orElse(ldapConfig.getBindCredential().toCharArray()));
        }

    }

    public LdapContext getLdapContext() throws NamingException {
        if (ldapContext == null) createLdapContext();

        return ldapContext;
    }

    private VaultCharSecret getVaultSecret() {
        return LDAPConstants.AUTH_TYPE_NONE.equals(ldapConfig.getAuthType())
                ? null
                : session.vault().getCharSecret(ldapConfig.getBindCredential());
    }

    public static StartTlsResponse startTLS(LdapContext ldapContext, String authType, String bindDN, char[] bindCredential) throws NamingException {
        try {
            StartTlsResponse tls = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
            tls.negotiate();

            ldapContext.addToEnvironment(Context.SECURITY_AUTHENTICATION, authType);

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                ldapContext.addToEnvironment(Context.SECURITY_PRINCIPAL, bindDN);
                ldapContext.addToEnvironment(Context.SECURITY_CREDENTIALS, bindCredential);
            }

            ldapContext.lookup("");

            return tls;
        } catch (Exception e) {
            logger.error("Could not negotiate TLS", e);
        }

        return null;
    }

    public static Hashtable<Object, Object> getConnectionProperties(LDAPConfig ldapConfig) {
        HashMap<String, Object> env = new HashMap<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapConfig.getFactoryName());

        if(!ldapConfig.isStartTls()) {
            String authType = ldapConfig.getAuthType();

            env.put(Context.SECURITY_AUTHENTICATION, authType);

            String bindDN = ldapConfig.getBindDN();

            char[] bindCredential = null;

            if (ldapConfig.getBindCredential() != null) {
                bindCredential = ldapConfig.getBindCredential().toCharArray();
            }

            if (!LDAPConstants.AUTH_TYPE_NONE.equals(authType)) {
                env.put(Context.SECURITY_PRINCIPAL, bindDN);
                env.put(Context.SECURITY_CREDENTIALS, bindCredential);
            }
        }
        String url = ldapConfig.getConnectionUrl();

        if (url != null) {
            env.put(Context.PROVIDER_URL, url);
        } else {
            logger.warn("LDAP URL is null. LDAPOperationManager won't work correctly");
        }

        String useTruststoreSpi = ldapConfig.getUseTruststoreSpi();
        LDAPConstants.setTruststoreSpiIfNeeded(useTruststoreSpi, url, env);

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

        if (logger.isDebugEnabled()) {
            Map<String, Object> copyEnv = new HashMap<>(env);
            if (copyEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
                copyEnv.put(Context.SECURITY_CREDENTIALS, "**************************************");
            }
            logger.debugf("Creating LdapContext using properties: [%s]", copyEnv);
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
