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

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import org.junit.Test;
import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.util.StaxParserUtil;

import java.io.InputStream;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.keycloak.saml.common.exceptions.ParsingException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakSamlAdapterXMLParserTest {

    private static final String CURRENT_XSD_LOCATION = "/schema/keycloak_saml_adapter_1_7.xsd";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void testValidationValid(String fileName) throws Exception {
        InputStream schema = getClass().getResourceAsStream(CURRENT_XSD_LOCATION);
        InputStream is = getClass().getResourceAsStream(fileName);
        assertNotNull(is);
        assertNotNull(schema);
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
    public void testValidationKeyInvalid() throws Exception {
        InputStream schemaIs = KeycloakSamlAdapterXMLParser.class.getResourceAsStream(CURRENT_XSD_LOCATION);
        InputStream is = getClass().getResourceAsStream("keycloak-saml-invalid.xml");
        assertNotNull(is);
        assertNotNull(schemaIs);

        expectedException.expect(ParsingException.class);
        StaxParserUtil.validate(is, schemaIs);
    }

    @Test
    public void testXmlParser() throws Exception {
        InputStream is = getClass().getResourceAsStream("keycloak-saml.xml");
        assertNotNull(is);
        KeycloakSamlAdapterXMLParser parser = new KeycloakSamlAdapterXMLParser();

        KeycloakSamlAdapter config = (KeycloakSamlAdapter)parser.parse(is);
        assertNotNull(config);
        assertEquals(1, config.getSps().size());
        SP sp = config.getSps().get(0);
        assertEquals("sp", sp.getEntityID());
        assertEquals("EXTERNAL", sp.getSslPolicy());
        assertEquals("format", sp.getNameIDPolicyFormat());
        assertTrue(sp.isForceAuthentication());
        assertTrue(sp.isIsPassive());
        assertEquals(2, sp.getKeys().size());
        Key signing = sp.getKeys().get(0);
        assertTrue(signing.isSigning());
        Key.KeyStoreConfig keystore = signing.getKeystore();
        assertNotNull(keystore);
        assertEquals("file", keystore.getFile());
        assertEquals("cp", keystore.getResource());
        assertEquals("pw", keystore.getPassword());
        assertEquals("private alias", keystore.getPrivateKeyAlias());
        assertEquals("private pw", keystore.getPrivateKeyPassword());
        assertEquals("cert alias", keystore.getCertificateAlias());
        Key encryption = sp.getKeys().get(1);
        assertTrue(encryption.isEncryption());
        assertEquals("private pem", encryption.getPrivateKeyPem());
        assertEquals("public pem", encryption.getPublicKeyPem());
        assertEquals("FROM_ATTRIBUTE", sp.getPrincipalNameMapping().getPolicy());
        assertEquals("attribute", sp.getPrincipalNameMapping().getAttributeName());
        assertTrue(sp.getRoleAttributes().size() == 1);
        assertTrue(sp.getRoleAttributes().contains("member"));

        IDP idp = sp.getIdp();
        assertEquals("idp", idp.getEntityID());
        assertEquals("RSA_SHA256", idp.getSignatureAlgorithm());
        assertEquals("canon", idp.getSignatureCanonicalizationMethod());
        assertTrue(idp.getSingleSignOnService().isSignRequest());
        assertTrue(idp.getSingleSignOnService().isValidateResponseSignature());
        assertEquals("POST", idp.getSingleSignOnService().getRequestBinding());
        assertEquals("url", idp.getSingleSignOnService().getBindingUrl());

        assertTrue(idp.getSingleLogoutService().isSignRequest());
        assertTrue(idp.getSingleLogoutService().isSignResponse());
        assertTrue(idp.getSingleLogoutService().isValidateRequestSignature());
        assertTrue(idp.getSingleLogoutService().isValidateResponseSignature());
        assertEquals("REDIRECT", idp.getSingleLogoutService().getRequestBinding());
        assertEquals("POST", idp.getSingleLogoutService().getResponseBinding());
        assertEquals("posturl", idp.getSingleLogoutService().getPostBindingUrl());
        assertEquals("redirecturl", idp.getSingleLogoutService().getRedirectBindingUrl());

        assertTrue(idp.getKeys().size() == 1);
        assertTrue(idp.getKeys().get(0).isSigning());
        assertEquals("cert pem", idp.getKeys().get(0).getCertificatePem());
    }


    @Test
    public void testXmlParserMultipleSigningKeys() throws Exception {
        InputStream is = getClass().getResourceAsStream("keycloak-saml-multiple-signing-keys.xml");
        assertNotNull(is);
        KeycloakSamlAdapterXMLParser parser = new KeycloakSamlAdapterXMLParser();

        KeycloakSamlAdapter config = (KeycloakSamlAdapter) parser.parse(is);
        assertNotNull(config);
        assertEquals(1, config.getSps().size());
        SP sp = config.getSps().get(0);
        IDP idp = sp.getIdp();

        assertTrue(idp.getKeys().size() == 4);
        for (int i = 0; i < 4; i ++) {
            Key key = idp.getKeys().get(i);
            assertTrue(key.isSigning());
            assertEquals("cert pem " + i, idp.getKeys().get(i).getCertificatePem());
        }
    }

    @Test
    public void testXmlParserHttpClientSettings() throws Exception {
        InputStream is = getClass().getResourceAsStream("keycloak-saml-wth-http-client-settings.xml");
        assertNotNull(is);
        KeycloakSamlAdapterXMLParser parser = new KeycloakSamlAdapterXMLParser();

        KeycloakSamlAdapter config = (KeycloakSamlAdapter) parser.parse(is);
        assertNotNull(config);
        assertEquals(1, config.getSps().size());
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
    }
}
