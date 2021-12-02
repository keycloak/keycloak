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

import org.jboss.logging.Logger;
import org.junit.Assume;
import org.junit.runners.model.Statement;
import org.junit.runner.Description;
import org.junit.rules.ExternalResource;
import org.keycloak.models.LDAPConstants;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Properties;

import static org.keycloak.testsuite.utils.io.IOUtil.PROJECT_BUILD_DIRECTORY;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPRule extends ExternalResource {

    private static final Logger log = Logger.getLogger(LDAPRule.class);

    // Note: Be sure to annotate the testing class with the "EnableVault" annotation
    // to get the necessary FilePlaintext vault created automatically for the test
    private static final String VAULT_EXPRESSION = "${vault.ldap_bindCredential}";

    public static final String LDAP_CONNECTION_PROPERTIES_LOCATION = "classpath:ldap/ldap-connection.properties";

    private static final String PROPERTY_ENABLE_ACCESS_CONTROL = "enableAccessControl";

    private static final String PROPERTY_ENABLE_ANONYMOUS_ACCESS = "enableAnonymousAccess";

    private static final String PROPERTY_ENABLE_SSL = "enableSSL";

    private static final String PROPERTY_ENABLE_STARTTLS = "enableStartTLS";

    private static final String PROPERTY_KEYSTORE_FILE = "keystoreFile";

    private static final String PRIVATE_KEY = "dependency/keystore/keycloak.jks";

    private static final String PROPERTY_CERTIFICATE_PASSWORD = "certificatePassword";

    LDAPTestConfiguration ldapTestConfiguration;
    private LDAPEmbeddedServer ldapEmbeddedServer;
    private LDAPAssume assume;

    protected Properties defaultProperties = new Properties();

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
    public Statement apply(Statement base, Description description) {
        // Default bind credential value
        defaultProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, "secret");
        // Default values of the authentication / access control method and connection encryption to use on the embedded
        // LDAP server upon start if not (re)set later via the LDAPConnectionParameters annotation directly on the test
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "true");
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false");
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "true");
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
        // Default LDAP server confidentiality required value
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "false");

        // Don't auto-update LDAP connection URL read from properties file for LDAP over SSL case even if it's wrong
        // (AKA don't try to guess, let the user to get it corrected in the properties file first)
        defaultProperties.setProperty("AUTO_UPDATE_LDAP_CONNECTION_URL", "false");

        Annotation ldapConnectionAnnotation = description.getAnnotation(LDAPConnectionParameters.class);
        if (ldapConnectionAnnotation != null) {
            // Mark the LDAP connection URL as auto-adjustable to correspond to specific annotation as necessary
            defaultProperties.setProperty("AUTO_UPDATE_LDAP_CONNECTION_URL", "true");
            LDAPConnectionParameters connectionParameters = (LDAPConnectionParameters) ldapConnectionAnnotation;
            // Configure the bind credential type of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.bindCredential()) {
                case SECRET:
                    log.debug("Setting bind credential to secret.");
                    defaultProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, "secret");
                    break;
                case VAULT:
                    log.debug("Setting bind credential to vault.");
                    defaultProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, VAULT_EXPRESSION);
                    break;
            }
            // Configure the authentication method of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.bindType()) {
                case NONE:
                    log.debug("Enabling anonymous authentication method on the LDAP server.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "true");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "false");
                    break;
                case SIMPLE:
                    log.debug("Disabling anonymous authentication method on the LDAP server.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "true");
                    break;
            }
            // Configure the connection encryption of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.encryption()) {
                case NONE:
                    log.debug("Disabling connection encryption on the LDAP server.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
                    break;
                case SSL:
                    log.debug("Enabling SSL connection encryption on the LDAP server.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "true");
                    // Require the LDAP server to accept only secured connections with SSL enabled
                    log.debug("Configuring the LDAP server to accepts only requests with a secured connection.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "true");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
                    break;
                case STARTTLS:
                    log.debug("Enabling StartTLS connection encryption on the LDAP server.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "true");
                    // Require the LDAP server to accept only secured connections with StartTLS enabled
                    log.debug("Configuring the LDAP server to accepts only requests with a secured connection.");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "true");
                    defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
                    break;
            }
        }
        return super.apply(base, description);
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
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        defaultProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:ldap/users.ldif");
        defaultProperties.setProperty(PROPERTY_CERTIFICATE_PASSWORD, "secret");
        defaultProperties.setProperty(PROPERTY_KEYSTORE_FILE, new File(PROJECT_BUILD_DIRECTORY, PRIVATE_KEY).getAbsolutePath());

        return new LDAPEmbeddedServer(defaultProperties);
    }

    public Map<String, String> getConfig() {
        Map<String, String> config = ldapTestConfiguration.getLDAPConfig();
        String ldapConnectionUrl = config.get(LDAPConstants.CONNECTION_URL);
        if (ldapConnectionUrl != null && defaultProperties.getProperty("AUTO_UPDATE_LDAP_CONNECTION_URL").equals("true")) {
            if (
                ldapConnectionUrl.startsWith("ldap://") &&
                defaultProperties.getProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL).equals("true")
               )
            {
                // Switch protocol prefix to "ldaps://" in connection URL if LDAP over SSL is requested
                String updatedUrl = ldapConnectionUrl.replaceAll("ldap://", "ldaps://");
                // Flip port number from LDAP to LDAPS
                updatedUrl = updatedUrl.replaceAll(
                    String.valueOf(ldapEmbeddedServer.getBindPort()),
                    String.valueOf(ldapEmbeddedServer.getBindLdapsPort())
                );
                config.put(LDAPConstants.CONNECTION_URL, updatedUrl);
                log.debugf("Using LDAP over SSL \"%s\" connection URL form over: \"%s\" since SSL connection was requested.", updatedUrl, ldapConnectionUrl);
            }
            if (
                ldapConnectionUrl.startsWith("ldaps://") &&
                !defaultProperties.getProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL).equals("true")
               )
            {
                // Switch protocol prefix back to "ldap://" in connection URL if LDAP over SSL flag is not set
                String updatedUrl = ldapConnectionUrl.replaceAll("ldaps://", "ldap://");
                // Flip port number from LDAPS to LDAP
                updatedUrl = updatedUrl.replaceAll(
                    String.valueOf(ldapEmbeddedServer.getBindLdapsPort()),
                    String.valueOf(ldapEmbeddedServer.getBindPort())
                );
                config.put(LDAPConstants.CONNECTION_URL, updatedUrl);
                log.debugf("Using plaintext / startTLS \"%s\" connection URL form over: \"%s\" since plaintext / startTLS connection was requested.", updatedUrl, ldapConnectionUrl);
            }
        }
        switch (defaultProperties.getProperty(LDAPConstants.BIND_CREDENTIAL)) {
            case VAULT_EXPRESSION:
                config.put(LDAPConstants.BIND_CREDENTIAL, VAULT_EXPRESSION);
                break;
        }
        switch (defaultProperties.getProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS)) {
            case "true":
                config.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_NONE);
                break;
            default:
                // Default to username + password LDAP authentication method
                config.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_SIMPLE);
        }
        switch (defaultProperties.getProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS)) {
            case "true":
                config.put(LDAPConstants.START_TLS, "true");
                // Use truststore from TruststoreSPI also for StartTLS connections
                config.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_ALWAYS);
                break;
            default:
                // Default to startTLS disabled
                config.put(LDAPConstants.START_TLS, "false");
                // By default use truststore from TruststoreSPI only for LDAP over SSL connections
                config.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_LDAPS_ONLY);
        }
        switch (defaultProperties.getProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED)) {
            case "true":
                System.setProperty("PROPERTY_SET_CONFIDENTIALITY_REQUIRED", "true");
                break;
            default:
                // Configure the LDAP server to accept not secured connections from clients by default
                System.setProperty("PROPERTY_SET_CONFIDENTIALITY_REQUIRED", "false");
        }
        return config;
    }

    public int getSleepTime() {
        return ldapTestConfiguration.getSleepTime();
    }

    public LDAPEmbeddedServer getLdapEmbeddedServer() {
        return ldapEmbeddedServer;
    }

    /** Allows to run particular LDAP test just under specific conditions (eg. some test running just on Active Directory) **/
    public interface LDAPAssume {

        boolean assumeTrue(LDAPTestConfiguration ldapConfig);

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface LDAPConnectionParameters {
        // Default to secret as the bind credential unless annotated otherwise
        BindCredential bindCredential() default LDAPConnectionParameters.BindCredential.SECRET;
        // Disable anonymous LDAP authentication by default unless annotated otherwise
        BindType bindType() default LDAPConnectionParameters.BindType.SIMPLE;
        // Enable SSL encrypted LDAP connections (along with the unencrypted ones) by default unless annotated otherwise
        Encryption encryption() default LDAPConnectionParameters.Encryption.SSL;

        public enum BindCredential {
            SECRET,
            VAULT
        }

        public enum BindType {
            NONE,
            SIMPLE
        }

        public enum Encryption {
            NONE,
            // Important: Choosing either of "SSL" or "STARTTLS" connection encryption methods below
            // will also configure the LDAP server to accept only a secured connection from clients
            // (IOW plaintext client connections will be prohibited). Use those two options with care!
            SSL,
            STARTTLS
        }
    }
}
