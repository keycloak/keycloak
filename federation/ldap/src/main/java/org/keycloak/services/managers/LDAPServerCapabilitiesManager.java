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

import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.naming.ldap.LdapContext;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPContextManager;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.storage.ldap.mappers.membership.group.GroupTreeResolver;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPServerCapabilitiesManager {

    private static final Logger logger = Logger.getLogger(LDAPServerCapabilitiesManager.class);

    public static final String TEST_CONNECTION = "testConnection";
    public static final String TEST_AUTHENTICATION = "testAuthentication";
    public static final String QUERY_SERVER_CAPABILITIES = "queryServerCapabilities";
    public static final int DEFAULT_TEST_TIMEOUT = 30000; // 30s default test timeout

    private static int parseConnectionTimeout(String connectionTimeout) {
        if (StringUtil.isNotBlank(connectionTimeout)) {
            try {
                int timeout = Integer.parseInt(connectionTimeout);
                if (timeout > 0) {
                    return timeout;
                }
            } catch (NumberFormatException e) {
                // just use default timeout
            }
        }
        return DEFAULT_TEST_TIMEOUT;
    }

    public static LDAPConfig buildLDAPConfig(TestLdapConnectionRepresentation config, RealmModel realm) {
        String bindCredential = config.getBindCredential();
        if (config.getComponentId() != null && !LDAPConstants.AUTH_TYPE_NONE.equals(config.getAuthType())
                && ComponentRepresentation.SECRET_VALUE.equals(bindCredential)) {
            // check the connection URL and the bind DN are the same to allow using the same configured password
            ComponentModel component = realm.getComponent(config.getComponentId());
            if (component != null) {
                LDAPConfig ldapConfig = new LDAPConfig(component.getConfig());
                if (checkLdapConnectionUrl(config, ldapConfig)
                        && config.getBindDn() != null && config.getBindDn().equalsIgnoreCase(ldapConfig.getBindDN())) {
                    bindCredential = ldapConfig.getBindCredential();
                }
            }
        }
        MultivaluedHashMap<String, String> configMap = new MultivaluedHashMap<>();
        configMap.putSingle(LDAPConstants.AUTH_TYPE, config.getAuthType());
        configMap.putSingle(LDAPConstants.BIND_DN, config.getBindDn());
        configMap.putSingle(LDAPConstants.BIND_CREDENTIAL, bindCredential);
        configMap.add(LDAPConstants.CONNECTION_URL, config.getConnectionUrl());
        configMap.add(LDAPConstants.USE_TRUSTSTORE_SPI, config.getUseTruststoreSpi());
        // set a forced timeout even when the timeout is infinite for testing
        // this is needed to not wait forever in the test and force connection creation in ldap
        String timeoutStr = Integer.toString(parseConnectionTimeout(config.getConnectionTimeout()));
        configMap.putSingle(LDAPConstants.CONNECTION_TIMEOUT, timeoutStr);
        configMap.putSingle(LDAPConstants.READ_TIMEOUT, timeoutStr);
        configMap.add(LDAPConstants.START_TLS, config.getStartTls());
        return new LDAPConfig(configMap);
    }

    /**
     * Ensure provided connection URI matches parsed LDAP connection URI.
     *
     * See: https://docs.oracle.com/javase/jndi/tutorial/ldap/misc/url.html
     * @param config
     * @param ldapConfig
     * @return
     */
    private static boolean checkLdapConnectionUrl(TestLdapConnectionRepresentation config, LDAPConfig ldapConfig) {
        // There could be multiple connection URIs separated via spaces.
        String[] configConnectionUrls = config.getConnectionUrl().trim().split(" ");
        String[] ldapConfigConnectionUrls = ldapConfig.getConnectionUrl().trim().split(" ");
        if (configConnectionUrls.length != ldapConfigConnectionUrls.length) {
            return false;
        }
        boolean urlsMatch = true;
        for (int i = 0; i < configConnectionUrls.length && urlsMatch; i++) {
            urlsMatch = Objects.equals(URI.create(configConnectionUrls[i]), URI.create(ldapConfigConnectionUrls[i]));
        }
        return urlsMatch;
    }

    public static Set<LDAPCapabilityRepresentation> queryServerCapabilities(TestLdapConnectionRepresentation config, KeycloakSession session,
                                                                            RealmModel realm) {

        if (! QUERY_SERVER_CAPABILITIES.equals(config.getAction())) {
            ServicesLogger.LOGGER.unknownAction(config.getAction());
            return Collections.emptySet();
        }

        LDAPConfig ldapConfig = buildLDAPConfig(config, realm);
        return new LDAPIdentityStore(session, ldapConfig).queryServerCapabilities();
    }

    public static class InvalidBindDNException extends javax.naming.NamingException {
        public InvalidBindDNException(String s) {
            super(s);
        }
    }

    public static String getErrorCode(Throwable throwable) {
        String errorMsg = "UnknownError";
        if (throwable instanceof javax.naming.NamingException)
             errorMsg = "NamingError";
        if (throwable instanceof javax.naming.AuthenticationException)
             errorMsg = "AuthenticationFailure";
        if (throwable instanceof javax.naming.CommunicationException)
             errorMsg = "CommunicationError";
        if (throwable instanceof javax.naming.ServiceUnavailableException)
             errorMsg = "ServiceUnavailable";
        if (throwable instanceof javax.naming.InvalidNameException)
             errorMsg = "InvalidName";
        if (throwable instanceof javax.naming.ServiceUnavailableException)
             errorMsg = "ServiceUnavailable";
        if (throwable instanceof InvalidBindDNException)
             errorMsg = "InvalidBindDN";
        if (throwable instanceof javax.naming.NameNotFoundException)
             errorMsg = "NameNotFound";
        if (throwable instanceof GroupTreeResolver.GroupTreeResolveException) {
             errorMsg = "GroupsMultipleParents";
        }

        if (throwable instanceof javax.naming.NamingException) {
            Throwable rootCause = ((javax.naming.NamingException)throwable).getRootCause();
            if (rootCause instanceof java.net.MalformedURLException)
                 errorMsg = "MalformedURL";
            if (rootCause instanceof java.net.NoRouteToHostException)
                 errorMsg = "NoRouteToHost";
            if (rootCause instanceof java.net.ConnectException)
                 errorMsg = "ConnectionFailed";
            if (rootCause instanceof java.net.UnknownHostException)
                 errorMsg = "UnknownHost";
            if (rootCause instanceof javax.net.ssl.SSLHandshakeException)
                 errorMsg = "SSLHandshakeFailed";
            if (rootCause instanceof java.net.SocketException)
                 errorMsg = "SocketReset";
        }
        return errorMsg;
    }

    public static void testLDAP(TestLdapConnectionRepresentation config, KeycloakSession session, RealmModel realm) throws javax.naming.NamingException {

        if (!TEST_CONNECTION.equals(config.getAction()) && !TEST_AUTHENTICATION.equals(config.getAction())) {
            ServicesLogger.LOGGER.unknownAction(config.getAction());
            throw new javax.naming.NamingException("testLDAP unknown action");
        }

        if (TEST_AUTHENTICATION.equals(config.getAction())) {
            // If AUTHENTICATION action is executed add also dn and credentials to configuration
            // LDAPContextManager is responsible for correct order of addition of credentials to context in case
            // tls is true
            if ((config.getBindDn() == null || config.getBindDn().isEmpty()) && LDAPConstants.AUTH_TYPE_SIMPLE.equals(config.getAuthType())) {
                throw new InvalidBindDNException("Unknown bind DN");
            }
        } else {
            // only test the connection.
            config.setAuthType(LDAPConstants.AUTH_TYPE_NONE);
        }

        LDAPConfig ldapConfig = buildLDAPConfig(config, realm);

        // Create ldapContextManager in try-with-resource so that ldapContext/tlsResponse/VaultSecret is closed/removed when it
        // is not needed anymore
        try (LDAPContextManager ldapContextManager = LDAPContextManager.create(session, ldapConfig)) {
            LdapContext ldapContext = ldapContextManager.getLdapContext();
            if (TEST_AUTHENTICATION.equals(config.getAction()) && LDAPConstants.AUTH_TYPE_NONE.equals(config.getAuthType())) {
                // reconnect to force an anonymous bind operation
                ldapContext.reconnect(null);
            }
        } catch (Exception ne) {
            String errorMessage = (TEST_AUTHENTICATION.equals(config.getAction())) ? "Error when authenticating to LDAP: "
                : "Error when connecting to LDAP: ";
            ServicesLogger.LOGGER.errorAuthenticating(ne, errorMessage + ne.getMessage());
            throw ne;
        }
    }
}
