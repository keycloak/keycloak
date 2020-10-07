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

import java.util.Collections;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPContextManager;
import org.keycloak.storage.ldap.idm.store.ldap.LDAPIdentityStore;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPServerCapabilitiesManager {

    private static final Logger logger = Logger.getLogger(LDAPServerCapabilitiesManager.class);

    public static final String TEST_CONNECTION = "testConnection";
    public static final String TEST_AUTHENTICATION = "testAuthentication";
    public static final String QUERY_SERVER_CAPABILITIES = "queryServerCapabilities";

    public static LDAPConfig buildLDAPConfig(TestLdapConnectionRepresentation config, RealmModel realm) {
        String bindCredential = config.getBindCredential();
        if (config.getComponentId() != null && ComponentRepresentation.SECRET_VALUE.equals(bindCredential)) {
            bindCredential = realm.getComponent(config.getComponentId()).getConfig().getFirst(LDAPConstants.BIND_CREDENTIAL);
        }
        MultivaluedHashMap<String, String> configMap = new MultivaluedHashMap<>();
        configMap.putSingle(LDAPConstants.AUTH_TYPE, config.getAuthType());
        configMap.putSingle(LDAPConstants.BIND_DN, config.getBindDn());
        configMap.putSingle(LDAPConstants.BIND_CREDENTIAL, bindCredential);
        configMap.add(LDAPConstants.CONNECTION_URL, config.getConnectionUrl());
        configMap.add(LDAPConstants.USE_TRUSTSTORE_SPI, config.getUseTruststoreSpi());
        configMap.putSingle(LDAPConstants.CONNECTION_TIMEOUT, config.getConnectionTimeout());
        configMap.add(LDAPConstants.START_TLS, config.getStartTls());
        return new LDAPConfig(configMap);
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

    public static boolean testLDAP(TestLdapConnectionRepresentation config, KeycloakSession session, RealmModel realm) {

        if (!TEST_CONNECTION.equals(config.getAction()) && !TEST_AUTHENTICATION.equals(config.getAction())) {
            ServicesLogger.LOGGER.unknownAction(config.getAction());
            return false;
        }

        if (TEST_AUTHENTICATION.equals(config.getAction())) {
            // If AUTHENTICATION action is executed add also dn and credentials to configuration
            // LDAPContextManager is responsible for correct order of addition of credentials to context in case
            // tls is true
            if (config.getBindDn() == null || config.getBindDn().isEmpty()) {
                logger.error("Unknown bind DN");
                return false;
            }
        } else {
            // only test the connection.
            config.setAuthType(LDAPConstants.AUTH_TYPE_NONE);
        }

        LDAPConfig ldapConfig = buildLDAPConfig(config, realm);

        // Create ldapContextManager in try-with-resource so that ldapContext/tlsResponse/VaultSecret is closed/removed when it
        // is not needed anymore
        try (LDAPContextManager ldapContextManager = LDAPContextManager.create(session, ldapConfig)) {
            ldapContextManager.getLdapContext();

            // Connection was successful, no exception was raised returning true
            return true;
        } catch (Exception ne) {
            String errorMessage = (TEST_AUTHENTICATION.equals(config.getAction())) ? "Error when authenticating to LDAP: "
                : "Error when connecting to LDAP: ";
            ServicesLogger.LOGGER.errorAuthenticating(ne, errorMessage + ne.getMessage());
            return false;
        }
    }
}
