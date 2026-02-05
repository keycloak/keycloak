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

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.keycloak.models.LDAPConstants;
import org.keycloak.util.ldap.LDAPEmbeddedServer;

import org.jboss.logging.Logger;
import org.junit.Assume;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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

    private static final String PROPERTY_KEYSTORE_FILE = "keystoreFile";

    private static final String PRIVATE_KEY = "dependency/keystore/keycloak.jks";

    private static final String PROPERTY_CERTIFICATE_PASSWORD = "certificatePassword";

    LDAPTestConfiguration ldapTestConfiguration;
    private LDAPEmbeddedServer ldapEmbeddedServer;
    private LDAPAssume assume;

    protected Properties serverProperties = new Properties();
    protected Map<String, String> clientConfig = new HashMap<>();

    public LDAPRule assumeTrue(LDAPAssume assume) {
        this.assume = assume;
        return this;
    }

    @Override
    public void before() throws Throwable {

        Assume.assumeTrue("Assumption in LDAPRule is false. Skiping the test", assume==null || assume.assumeTrue(ldapTestConfiguration));

        if (ldapTestConfiguration.isStartEmbeddedLdapServer()) {
            ldapEmbeddedServer = createServer();
            ldapEmbeddedServer.init();
            ldapEmbeddedServer.start();
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        String connectionPropsLocation = getConnectionPropertiesLocation();
        ldapTestConfiguration = LDAPTestConfiguration.readConfiguration(connectionPropsLocation);

        // Default bind credential value
        serverProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, "secret");
        serverProperties.setProperty(LDAPConstants.CONNECTION_POOLING, "true");
        // Default values of the authentication / access control method and connection encryption to use on the embedded
        // LDAP server upon start if not (re)set later via the LDAPConnectionParameters annotation directly on the test
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "true");
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false");
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "true");
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
        // Default LDAP server confidentiality required value
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "false");

        // Don't auto-update LDAP connection URL read from properties file for LDAP over SSL case even if it's wrong
        // (AKA don't try to guess, let the user to get it corrected in the properties file first)
        serverProperties.setProperty("AUTO_UPDATE_LDAP_CONNECTION_URL", "false");

        // Default configuration for LDAP client.
        clientConfig = ldapTestConfiguration.getLDAPConfig();

        clientConfig.put(LDAPConstants.BIND_CREDENTIAL, "secret");
        clientConfig.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_SIMPLE);
        clientConfig.put(LDAPConstants.START_TLS, "false");
        clientConfig.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_NEVER);

        Annotation ldapConnectionAnnotation = description.getAnnotation(LDAPConnectionParameters.class);
        if (ldapConnectionAnnotation != null) {
            LDAPConnectionParameters connectionParameters = (LDAPConnectionParameters) ldapConnectionAnnotation;
            // Configure the bind credential type of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.bindCredential()) {
                case SECRET:
                    log.debug("Setting bind credential to secret.");
                    serverProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, "secret");
                    clientConfig.put(LDAPConstants.BIND_CREDENTIAL, "secret");
                    break;
                case VAULT:
                    log.debug("Setting bind credential to vault.");
                    serverProperties.setProperty(LDAPConstants.BIND_CREDENTIAL, VAULT_EXPRESSION);
                    clientConfig.put(LDAPConstants.BIND_CREDENTIAL, VAULT_EXPRESSION);
                    break;
            }
            // Configure the authentication method of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.bindType()) {
                case NONE:
                    log.debug("Enabling anonymous authentication method on the LDAP server.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "true");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "false");
                    clientConfig.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_NONE);
                    clientConfig.remove(LDAPConstants.BIND_DN);
                    clientConfig.remove(LDAPConstants.BIND_CREDENTIAL);
                    break;
                case SIMPLE:
                    log.debug("Disabling anonymous authentication method on the LDAP server.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "true");
                    clientConfig.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_SIMPLE);
                    break;
                case EXTERNAL:
                    log.debug("Enabling SASL EXTERNAL authentication method.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ANONYMOUS_ACCESS, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_ACCESS_CONTROL, "true");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ADMIN_CERTIFICATE_KEYSTORE, new File(PROJECT_BUILD_DIRECTORY, PRIVATE_KEY).getAbsolutePath());
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ADMIN_CERTIFICATE_KEYSTORE_PASSWORD, "secret");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_REQUIRE_CLIENT_CERTIFICATE, "true");
                    clientConfig.put(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_EXTERNAL);
                    break;
            }
            // Configure the connection encryption of the LDAP rule depending on the provided annotation arguments
            switch (connectionParameters.encryption()) {
                case NONE:
                    log.debug("Disabling connection encryption on the LDAP server.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, "10389");
                    clientConfig.put(LDAPConstants.START_TLS, "false");
                    clientConfig.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_NEVER);
                    clientConfig.put(LDAPConstants.CONNECTION_URL, "ldap://localhost:10389");
                    break;
                case SSL:
                    log.debug("Enabling SSL connection encryption on the LDAP server.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "true");
                    // Require the LDAP server to accept only secured connections with SSL enabled
                    log.debug("Configuring the LDAP server to accepts only requests with a secured connection.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "true");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_LDAPS_PORT, "10636");
                    // Use truststore from TruststoreSPI for LDAPS connections
                    clientConfig.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_LDAPS_ONLY);
                    clientConfig.put(LDAPConstants.CONNECTION_URL, "ldaps://localhost:10636");

                    break;
                case STARTTLS:
                    log.debug("Enabling StartTLS connection encryption on the LDAP server.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_STARTTLS, "true");
                    // Require the LDAP server to accept only secured connections with StartTLS enabled
                    log.debug("Configuring the LDAP server to accepts only requests with a secured connection.");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_SET_CONFIDENTIALITY_REQUIRED, "true");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_ENABLE_SSL, "false");
                    serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_BIND_PORT, "10389");
                    clientConfig.put(LDAPConstants.START_TLS, "true");
                    // Use truststore from TruststoreSPI for STARTTLS connections
                    clientConfig.put(LDAPConstants.USE_TRUSTSTORE_SPI, LDAPConstants.USE_TRUSTSTORE_ALWAYS);
                    clientConfig.put(LDAPConstants.CONNECTION_URL, "ldap://localhost:10389");
                    clientConfig.put(LDAPConstants.CONNECTION_POOLING, "false");
                    break;
            }
        }
        return super.apply(base, description);
    }

    @Override
    public void after() {
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
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_DSF, LDAPEmbeddedServer.DSF_INMEMORY);
        serverProperties.setProperty(LDAPEmbeddedServer.PROPERTY_LDIF_FILE, "classpath:ldap/users.ldif");
        serverProperties.setProperty(PROPERTY_CERTIFICATE_PASSWORD, "secret");
        serverProperties.setProperty(PROPERTY_KEYSTORE_FILE, new File(PROJECT_BUILD_DIRECTORY, PRIVATE_KEY).getAbsolutePath());

        return new LDAPEmbeddedServer(serverProperties);
    }

    public Map<String, String> getConfig() {
        return clientConfig;
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
            SIMPLE,
            EXTERNAL
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

    public boolean isEmbeddedServer() {
        return ldapTestConfiguration.isStartEmbeddedLdapServer();
    }
}
