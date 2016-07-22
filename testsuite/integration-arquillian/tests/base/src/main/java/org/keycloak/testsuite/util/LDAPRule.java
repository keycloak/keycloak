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

package org.keycloak.testsuite.util;

import java.util.Map;
import java.util.Properties;

import org.junit.rules.ExternalResource;
import org.keycloak.models.LDAPConstants;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "classpath:ldap/ldap-connection.properties";

    private static final String PROPERTY_ENABLE_SSL = "enableSSL";

    private static final String PROPERTY_KEYSTORE_FILE = "keystoreFile";

    private static final String PRIVATE_KEY = "keystore/keycloak.jks";

    private static final String PROPERTY_CERTIFICATE_PASSWORD = "certificatePassword";

    protected LDAPTestConfiguration ldapTestConfiguration;
    protected LDAPEmbeddedServer ldapEmbeddedServer;

    @Override
    protected void before() throws Throwable {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(connectionPropsLocation);

        if (ldapTestConfiguration.isStartEmbeddedLdapServer()) {
            ldapEmbeddedServer = createServer();
            ldapEmbeddedServer.init();
            ldapEmbeddedServer.start();
        }
    }

    @Override
    protected void after() {
        try {
            if (ldapEmbeddedServer != null) {
                ldapEmbeddedServer.stop();
                ldapEmbeddedServer = null;
                ldapTestConfiguration = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error tearDown Embedded LDAP server.", e);
        }
    }

    protected String getConnectionPropertiesLocation() {
        return LDAP_CONNECTION_PROPERTIES_LOCATION;
    }

    protected LDAPEmbeddedServer createServer() {
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:ldap/users.ldif");
        defaultProperties.setProperty(LDAPConstants.CONNECTION_URL, "ldaps://localhost:10636");
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, "10636");
        defaultProperties.setProperty(PROPERTY_ENABLE_SSL, "true");
        defaultProperties.setProperty(PROPERTY_CERTIFICATE_PASSWORD, "secret");
        defaultProperties.setProperty(PROPERTY_KEYSTORE_FILE, this.getClass().getClassLoader().getResource(LDAPRule.PRIVATE_KEY).getFile());

        return new LDAPEmbeddedServer(defaultProperties);
    }

    public Map<String, String> getConfig() {
        return ldapTestConfiguration.getLDAPConfig();
    }

    public int getSleepTime() {
        return ldapTestConfiguration.getSleepTime();
    }
}
