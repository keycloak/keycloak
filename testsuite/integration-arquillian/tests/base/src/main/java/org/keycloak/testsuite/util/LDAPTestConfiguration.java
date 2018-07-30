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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.jboss.logging.Logger;
import org.keycloak.common.constants.GenericConstants;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.UserStorageProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.keycloak.testsuite.utils.io.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPTestConfiguration {

    private static final Logger log = Logger.getLogger(LDAPTestConfiguration.class);

    private int sleepTime;
    private boolean startEmbeddedLdapServer = true;
    private boolean caseSensitiveLogin = true;
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
        String keyTabPath =  getResource("/kerberos/http.keytab");
        DEFAULT_VALUES.put(KerberosConstants.KEYTAB, keyTabPath);
        DEFAULT_VALUES.put(KerberosConstants.DEBUG, "true");
        DEFAULT_VALUES.put(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION, "true");
        DEFAULT_VALUES.put(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "true");
        DEFAULT_VALUES.put(KerberosConstants.USE_KERBEROS_FOR_PASSWORD_AUTHENTICATION, "false");
    }

    public static LDAPTestConfiguration readConfiguration(String connectionPropertiesLocation) {
        LDAPTestConfiguration ldapTestConfiguration = new LDAPTestConfiguration();
        ldapTestConfiguration.loadConnectionProperties(connectionPropertiesLocation);
        return ldapTestConfiguration;
    }
    
    public static String getResource(String resourcePath) {
        URL urlPath = LDAPTestConfiguration.class.getResource(resourcePath);
        String absolutePath = new File(urlPath.getFile()).getAbsolutePath();
        return absolutePath;
    }

    protected void loadConnectionProperties(String connectionPropertiesLocation) {
        // TODO: Improve and possibly use FindFile
        InputStream is;
        try {
            if (connectionPropertiesLocation.startsWith(GenericConstants.PROTOCOL_CLASSPATH)) {
                String classPathLocation = connectionPropertiesLocation.replace(GenericConstants.PROTOCOL_CLASSPATH, "");
                log.info("Reading LDAP configuration from classpath from: " + classPathLocation);
                is = LDAPTestConfiguration.class.getClassLoader().getResourceAsStream(classPathLocation);
            } else {
                String file = getResource(connectionPropertiesLocation);
                log.info("Reading LDAP configuration from: " + connectionPropertiesLocation);
                is = new FileInputStream(file);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        PropertiesConfiguration p;
        try {
            p = new PropertiesConfiguration();
            p.setDelimiterParsingDisabled(true);
            p.load(is);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        config = new HashMap<String, String>();
        for (Map.Entry<String, String> property : PROP_MAPPINGS.entrySet()) {
            String propertyName = property.getKey();
            String configName = property.getValue();

            String value = p.getString(configName);
            if (value == null) {
                value = DEFAULT_VALUES.get(propertyName);
            }

            config.put(propertyName, value);
        }

        startEmbeddedLdapServer = p.getBoolean("idm.test.ldap.start.embedded.ldap.server", true);
        sleepTime = p.getInteger("idm.test.ldap.sleepTime", 1000);
        caseSensitiveLogin = p.getBoolean("idm.test.kerberos.caseSensitiveLogin", true);
        log.info("Start embedded server: " + startEmbeddedLdapServer);
        log.info("Read config: " + config);
    }

    public Map<String,String> getLDAPConfig() {
        return config;
    }

    public boolean isStartEmbeddedLdapServer() {
        return startEmbeddedLdapServer;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    public boolean isCaseSensitiveLogin() {
        return caseSensitiveLogin;
    }

}
