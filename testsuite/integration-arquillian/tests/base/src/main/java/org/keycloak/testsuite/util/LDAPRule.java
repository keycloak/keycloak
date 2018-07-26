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

import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.keycloak.models.LDAPConstants;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "classpath:ldap/ldap-connection.properties";

    private static final String PROPERTY_ENABLE_SSL = "enableSSL";

    private static final String PROPERTY_KEYSTORE_FILE = "keystoreFile";

    private static final String PRIVATE_KEY = "keystore/keycloak.jks";

    private static final String PROPERTY_CERTIFICATE_PASSWORD = "certificatePassword";

    LDAPTestConfiguration ldapTestConfiguration;
    private LDAPEmbeddedServer ldapEmbeddedServer;
    private LDAPAssume assume;

    public LDAPRule assumeTrue(LDAPAssume assume) {
        this.assume = assume;
        return this;
    }


    @Override
    protected void before() throws Throwable {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(connectionPropsLocation);

        Assume.assumeTrue("Assumption in LDAPRule is false. Skiping the test", assume==null || assume.assumeTrue(ldapTestConfiguration));

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


    /** Allows to run particular LDAP test just under specific conditions (eg. some test running just on Active Directory) **/
    public interface LDAPAssume {

        boolean assumeTrue(LDAPTestConfiguration ldapConfig);

    }
}
