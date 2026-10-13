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

package org.keycloak.adapters.saml.config.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAdapterXMLParserTest {

    private static final String CURRENT_XSD_LOCATION = "/schema/keycloak_saml_adapter_1_13.xsd";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void testValidationValid(String fileName) throws Exception {
        InputStream schema = getClass().getResourceAsStream(CURRENT_XSD_LOCATION);
        InputStream is = getClass().getResourceAsStream(fileName);
        assertThat(is, Matchers.notNullValue());
        assertThat(schema, Matchers.notNullValue());
        StaxParserUtil.validate(is, schema);
    }

    @Test
    public void testValidationSimpleFile() throws Exception {
        testValidationValid("keycloak-saml.xml");
    }

    @Test
    public void testValidationMultipleKeys() throws Exception {
        testValidationValid("keycloak-saml-multiple-signing-keys.xml");
    }

    @Test
    public void testValidationWithHttpClient() throws Exception {
        testValidationValid("keycloak-saml-wth-http-client-settings.xml");
    }

    @Test
    public void testValidationWithMetadataUrl() throws Exception {
        testValidationValid("keycloak-saml-with-metadata-url.xml");
    }

    @Test
    public void testValidationWithAllowedClockSkew() throws Exception {
        testValidationValid("keycloak-saml-with-allowed-clock-skew-with-unit.xml");
    }

    @Test
    public void testValidationWithRoleMappingsProvider() throws Exception {
        testValidationValid("keycloak-saml-with-role-mappings-provider.xml");
    }

    @Test
    public void testValidationWithKeepDOMAssertion() throws Exception {
        testValidationValid("keycloak-saml-keepdomassertion.xml");
        // check keep dom assertion is TRUE
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-keepdomassertion.xml", KeycloakSamlAdapter.class);
        assertThat(config, Matchers.notNullValue());
        assertThat(config.getSps(), hasSize(1));
        SP sp = config.getSps().get(0);
        assertThat(sp.isKeepDOMAssertion(), is(true));
    }

    @Test
    public void testValidationKeyInvalid() throws Exception {
        InputStream schemaIs = KeycloakSamlAdapterV1Parser.class.getResourceAsStream(CURRENT_XSD_LOCATION);
        InputStream is = getClass().getResourceAsStream("keycloak-saml-invalid.xml");
        assertThat(is, Matchers.notNullValue());
        assertThat(schemaIs, Matchers.notNullValue());

        expectedException.expect(ParsingException.class);
        StaxParserUtil.validate(is, schemaIs);
    }

    @Test
    public void testParseSimpleFileNoNamespace() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-no-namespace.xml", KeycloakSamlAdapter.class);
    }

    @Test
    public void testXmlParserBaseFile() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml.xml", KeycloakSamlAdapter.class);

        assertThat(config, notNullValue());
        assertThat(config.getSps(), hasSize(1));

        SP sp = config.getSps().get(0);
        assertThat(sp.getEntityID(), is("sp"));
        assertThat(sp.getSslPolicy(), is("EXTERNAL"));
        assertThat(sp.getNameIDPolicyFormat(), is("format"));
        assertThat(sp.isForceAuthentication(), is(true));
        assertThat(sp.isIsPassive(), is(true));
        assertThat(sp.isAutodetectBearerOnly(), is(false));
        assertThat(sp.isKeepDOMAssertion(), is(false));
        assertThat(sp.getKeys(), hasSize(2));

        Key signing = sp.getKeys().get(0);
        assertThat(signing.isSigning(), is(true));
        Key.KeyStoreConfig keystore = signing.getKeystore();
        assertThat(keystore, notNullValue());
        assertThat(keystore.getFile(), is("file"));
        assertThat(keystore.getResource(), is("cp"));
        assertThat(keystore.getPassword(), is("pw"));
        assertThat(keystore.getPrivateKeyAlias(), is("private alias"));
        assertThat(keystore.getPrivateKeyPassword(), is("private pw"));
        assertThat(keystore.getCertificateAlias(), is("cert alias"));
        Key encryption = sp.getKeys().get(1);
        assertThat(encryption.isEncryption(), is(true));
        assertThat(encryption.getPrivateKeyPem(), is("private pem"));
        assertThat(encryption.getPublicKeyPem(), is("public pem"));
        assertThat(sp.getPrincipalNameMapping().getPolicy(), is("FROM_ATTRIBUTE"));
        assertThat(sp.getPrincipalNameMapping().getAttributeName(), is("attribute"));
        assertThat(sp.getRoleAttributes(), hasSize(1));
        assertThat(sp.getRoleAttributes(), Matchers.contains("member"));

        IDP idp = sp.getIdp();
        assertThat(idp.getEntityID(), is("idp"));
        assertThat(idp.getSignatureAlgorithm(), is("RSA_SHA256"));
        assertThat(idp.getSignatureCanonicalizationMethod(), is("canon"));
        assertThat(idp.getSingleSignOnService().isSignRequest(), is(true));
        assertThat(idp.getSingleSignOnService().isValidateResponseSignature(), is(true));
        assertThat(idp.getSingleSignOnService().getRequestBinding(), is("POST"));
        assertThat(idp.getSingleSignOnService().getBindingUrl(), is("url"));

        assertThat(idp.getSingleLogoutService().isSignRequest(), is(false));
        assertThat(idp.getSingleLogoutService().isSignResponse(), is(true));
        assertThat(idp.getSingleLogoutService().isValidateRequestSignature(), is(true));
        assertThat(idp.getSingleLogoutService().isValidateResponseSignature(), is(true));
        assertThat(idp.getSingleLogoutService().getRequestBinding(), is("REDIRECT"));
        assertThat(idp.getSingleLogoutService().getResponseBinding(), is("POST"));
        assertThat(idp.getSingleLogoutService().getPostBindingUrl(), is("posturl"));
        assertThat(idp.getSingleLogoutService().getRedirectBindingUrl(), is("redirecturl"));

        assertThat(idp.getKeys(), hasSize(1));
        assertThat(idp.getKeys().get(0).isSigning(), is(true));
        assertThat(idp.getKeys().get(0).getCertificatePem(), is("cert pem"));
    }

    private <T> T parseKeycloakSamlAdapterConfig(String fileName, Class<T> targetClass) throws ParsingException, IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            KeycloakSamlAdapterParser parser = KeycloakSamlAdapterParser.getInstance();
            return targetClass.cast(parser.parse(is));
        }
    }


    @Test
    public void testXmlParserMultipleSigningKeys() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-multiple-signing-keys.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), hasSize(1));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();

        assertThat(idp.getKeys(), hasSize(4));
        for (int i = 0; i < 4; i++) {
            Key key = idp.getKeys().get(i);
            assertThat(key.isSigning(), is(true));
            assertThat(idp.getKeys().get(i).getCertificatePem(), is("cert pem " + i));
        }
    }

    @Test
    public void testXmlParserHttpClientSettings() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-wth-http-client-settings.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), hasSize(1));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();

        assertThat(idp.getHttpClientConfig(), notNullValue());
        assertThat(idp.getHttpClientConfig().getClientKeystore(), is("ks"));
        assertThat(idp.getHttpClientConfig().getClientKeystorePassword(), is("ks-pwd"));
        assertThat(idp.getHttpClientConfig().getProxyUrl(), is("pu"));
        assertThat(idp.getHttpClientConfig().getTruststore(), is("ts"));
        assertThat(idp.getHttpClientConfig().getTruststorePassword(), is("tsp"));
        assertThat(idp.getHttpClientConfig().getConnectionPoolSize(), is(42));
        assertThat(idp.getHttpClientConfig().isAllowAnyHostname(), is(true));
        assertThat(idp.getHttpClientConfig().isDisableTrustManager(), is(true));
        assertThat(idp.getHttpClientConfig().getSocketTimeout(), is(6000L));
        assertThat(idp.getHttpClientConfig().getConnectionTimeout(), is(7000L));
        assertThat(idp.getHttpClientConfig().getConnectionTTL(), is(200L));
    }

    @Test
    public void testXmlParserSystemPropertiesNoPropertiesSet() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-properties.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();

        assertThat(sp.getEntityID(), is("sp"));
        assertThat(sp.getSslPolicy(), is("${keycloak-saml-properties.sslPolicy}"));

        assertThat(idp.isSignaturesRequired(), is(false));

        assertThat(idp.getSingleLogoutService().isSignRequest(), is(true));
        assertThat(idp.getSingleLogoutService().isSignResponse(), is(false));

        assertThat(idp.getSingleSignOnService().isSignRequest(), is(true));
        assertThat(idp.getSingleSignOnService().isValidateResponseSignature(), is(true));

        // These should take default from IDP.signaturesRequired
        assertThat(idp.getSingleLogoutService().isValidateRequestSignature(), is(false));
        assertThat(idp.getSingleLogoutService().isValidateResponseSignature(), is(false));

        assertThat(idp.getSingleSignOnService().isValidateAssertionSignature(), is(false));
    }

    @Test
    public void testXmlParserSystemPropertiesWithPropertiesSet() throws Exception {
        try {
            System.setProperty("keycloak-saml-properties.entityID", "meid");
            System.setProperty("keycloak-saml-properties.sslPolicy", "INTERNAL");
            System.setProperty("keycloak-saml-properties.signaturesRequired", "true");

            KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-properties.xml", KeycloakSamlAdapter.class);
            assertThat(config, notNullValue());
            assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
            SP sp = config.getSps().get(0);
            IDP idp = sp.getIdp();

            assertThat(sp.getEntityID(), is("meid"));
            assertThat(sp.getSslPolicy(), is("INTERNAL"));
            assertThat(idp.isSignaturesRequired(), is(true));

            assertThat(idp.getSingleLogoutService().isSignRequest(), is(true));
            assertThat(idp.getSingleLogoutService().isSignResponse(), is(false));

            assertThat(idp.getSingleSignOnService().isSignRequest(), is(true));
            assertThat(idp.getSingleSignOnService().isValidateResponseSignature(), is(true));

            // These should take default from IDP.signaturesRequired
            assertThat(idp.getSingleLogoutService().isValidateRequestSignature(), is(true));
            assertThat(idp.getSingleLogoutService().isValidateResponseSignature(), is(true));

            // This is false by default
            assertThat(idp.getSingleSignOnService().isValidateAssertionSignature(), is(false));
        } finally {
            System.clearProperty("keycloak-saml-properties.entityID");
            System.clearProperty("keycloak-saml-properties.sslPolicy");
            System.clearProperty("keycloak-saml-properties.signaturesRequired");
        }
    }

    @Test
    public void testMetadataUrl() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-with-metadata-url.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();
        assertThat(idp.getMetadataUrl(), is("https:///example.com/metadata.xml"));
    }

    @Test
    public void testAllowedClockSkewDefaultUnit() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-with-allowed-clock-skew-default-unit.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();
        assertThat(idp.getAllowedClockSkew(), is(3));
        assertThat(idp.getAllowedClockSkewUnit(), is(TimeUnit.SECONDS));
    }

    @Test
    public void testAllowedClockSkewWithUnit() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-with-allowed-clock-skew-with-unit.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();
        assertThat(idp.getAllowedClockSkew(), is(3500));
        assertThat(idp.getAllowedClockSkewUnit(), is (TimeUnit.MILLISECONDS));
    }

    @Test
    public void testParseRoleMappingsProvider() throws Exception {
        KeycloakSamlAdapter config = parseKeycloakSamlAdapterConfig("keycloak-saml-with-role-mappings-provider.xml", KeycloakSamlAdapter.class);
        assertThat(config, notNullValue());
        assertThat(config.getSps(), Matchers.contains(instanceOf(SP.class)));
        SP sp = config.getSps().get(0);
        SP.RoleMappingsProviderConfig roleMapperConfig = sp.getRoleMappingsProviderConfig();
        assertThat(roleMapperConfig, notNullValue());
        assertThat(roleMapperConfig.getId(), is("properties-based-role-mapper"));
        Properties providerConfig = roleMapperConfig.getConfiguration();
        assertThat(providerConfig.size(), is(2));
        assertThat(providerConfig.containsKey("properties.resource.location"), is(true));
        assertThat(providerConfig.getProperty("properties.resource.location"), is("role-mappings.properties"));
        assertThat(providerConfig.containsKey("another.property"), is(true));
        assertThat(providerConfig.getProperty("another.property"), is("another.value"));
    }
}
