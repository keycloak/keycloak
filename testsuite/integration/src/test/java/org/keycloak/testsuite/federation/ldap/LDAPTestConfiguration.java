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

package org.keycloak.testsuite.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.UserStorageProvider;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTestConfiguration {

    private static final Logger log = Logger.getLogger(LDAPTestConfiguration.class);

    private String connectionPropertiesLocation;
    private int sleepTime;
    private boolean startEmbeddedLdapServer = true;
    private Map<String, String> config;

    protected static final Map<String, String> PROP_MAPPINGS = new HashMap<String, String>();

    protected static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>();

    static {
        PROP_MAPPINGS.put(LDAPConstants.CONNECTION_URL, "idm.test.ldap.connection.url");
        PROP_MAPPINGS.put(LDAPConstants.BASE_DN, "idm.test.ldap.base.dn");
        PROP_MAPPINGS.put(LDAPConstants.USERS_DN, "idm.test.ldap.user.dn.suffix");
        PROP_MAPPINGS.put(LDAPConstants.BIND_DN, "idm.test.ldap.bind.dn");
        PROP_MAPPINGS.put(LDAPConstants.BIND_CREDENTIAL, "idm.test.ldap.bind.credential");
        PROP_MAPPINGS.put(LDAPConstants.VENDOR, "idm.test.ldap.vendor");
        PROP_MAPPINGS.put(LDAPConstants.CONNECTION_POOLING, "idm.test.ldap.connection.pooling");
        PROP_MAPPINGS.put(LDAPConstants.PAGINATION, "idm.test.ldap.pagination");
        PROP_MAPPINGS.put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "idm.test.ldap.batch.size.for.sync");
        PROP_MAPPINGS.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, "idm.test.ldap.username.ldap.attribute");
        PROP_MAPPINGS.put(LDAPConstants.RDN_LDAP_ATTRIBUTE, "idm.test.ldap.rdn.ldap.attribute");
        PROP_MAPPINGS.put(LDAPConstants.USER_OBJECT_CLASSES, "idm.test.ldap.user.object.classes");
        PROP_MAPPINGS.put(LDAPConstants.EDIT_MODE, "idm.test.ldap.edit.mode");

        PROP_MAPPINGS.put(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "idm.test.kerberos.allow.kerberos.authentication");
        PROP_MAPPINGS.put(KerberosConstants.KERBEROS_REALM, "idm.test.kerberos.realm");
        PROP_MAPPINGS.put(KerberosConstants.SERVER_PRINCIPAL, "idm.test.kerberos.server.principal");
        PROP_MAPPINGS.put(KerberosConstants.KEYTAB, "idm.test.kerberos.keytab");
        PROP_MAPPINGS.put(KerberosConstants.DEBUG, "idm.test.kerberos.debug");
        PROP_MAPPINGS.put(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION, "idm.test.kerberos.allow.password.authentication");
        PROP_MAPPINGS.put(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "idm.test.kerberos.update.profile.first.login");
        PROP_MAPPINGS.put(KerberosConstants.USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION, "idm.test.kerberos.use.kerberos.for.password.authentication");

        DEFAULT_VALUES.put(LDAPConstants.CONNECTION_URL, "ldap://localhost:10389");
        DEFAULT_VALUES.put(LDAPConstants.BASE_DN, "dc=keycloak,dc=org");
        DEFAULT_VALUES.put(LDAPConstants.USERS_DN, "ou=People,dc=keycloak,dc=org");
        DEFAULT_VALUES.put(LDAPConstants.BIND_DN, "uid=admin,ou=system");
        DEFAULT_VALUES.put(LDAPConstants.BIND_CREDENTIAL, "secret");
        DEFAULT_VALUES.put(LDAPConstants.VENDOR, LDAPConstants.VENDOR_OTHER);
        DEFAULT_VALUES.put(LDAPConstants.CONNECTION_POOLING, "true");
        DEFAULT_VALUES.put(LDAPConstants.PAGINATION, "true");
        DEFAULT_VALUES.put(LDAPConstants.BATCH_SIZE_FOR_SYNC, String.valueOf(LDAPConstants.DEFAULT_BATCH_SIZE_FOR_SYNC));
        DEFAULT_VALUES.put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, null);
        DEFAULT_VALUES.put(LDAPConstants.USER_OBJECT_CLASSES, null);
        DEFAULT_VALUES.put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());

        DEFAULT_VALUES.put(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION, "false");
        DEFAULT_VALUES.put(KerberosConstants.KERBEROS_REALM, "KEYCLOAK.ORG");
        DEFAULT_VALUES.put(KerberosConstants.SERVER_PRINCIPAL, "HTTP/localhost@KEYCLOAK.ORG");
//        URL keytabUrl = LDAPTestConfiguration.class.getResource("/kerberos/http.keytab");
//        String keyTabPath = new File(keytabUrl.getFile()).getAbsolutePath();
//        DEFAULT_VALUES.put(KerberosConstants.KEYTAB, keyTabPath);
        DEFAULT_VALUES.put(KerberosConstants.DEBUG, "true");
        DEFAULT_VALUES.put(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION, "true");
        DEFAULT_VALUES.put(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "true");
        DEFAULT_VALUES.put(KerberosConstants.USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION, "false");
    }

    public static LDAPTestConfiguration readConfiguration(String connectionPropertiesLocation) {
        LDAPTestConfiguration ldapTestConfiguration = new LDAPTestConfiguration();
        ldapTestConfiguration.setConnectionPropertiesLocation(connectionPropertiesLocation);
        ldapTestConfiguration.loadConnectionProperties();
        return ldapTestConfiguration;
    }

    protected void loadConnectionProperties() {
        Properties p = new Properties();
        try {
            log.info("Reading LDAP configuration from: " + connectionPropertiesLocation);
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(connectionPropertiesLocation);
            p.load(is);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        config = new HashMap<String, String>();
        for (Map.Entry<String, String> property : PROP_MAPPINGS.entrySet()) {
            String propertyName = property.getKey();
            String configName = property.getValue();

            String value = (String) p.get(configName);
            if (value == null) {
                value = DEFAULT_VALUES.get(propertyName);
            }

            config.put(propertyName, value);
        }

        startEmbeddedLdapServer = Boolean.parseBoolean(p.getProperty("idm.test.ldap.start.embedded.ldap.server", "true"));
        sleepTime = Integer.parseInt(p.getProperty("idm.test.ldap.sleepTime", "1000"));
        config.put("startEmbeddedLdapServer", Boolean.toString(startEmbeddedLdapServer));
        log.info("Start embedded server: " + startEmbeddedLdapServer);
        log.info("Read config: " + config);
    }

    public Map<String,String> getLDAPConfig() {
        return config;
    }

    public void setConnectionPropertiesLocation(String connectionPropertiesLocation) {
        this.connectionPropertiesLocation = connectionPropertiesLocation;
    }

    public boolean isStartEmbeddedLdapServer() {
        return startEmbeddedLdapServer;
    }

    public int getSleepTime() {
        return sleepTime;
    }

}
