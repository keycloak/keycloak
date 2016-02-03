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

package org.keycloak.test.adapters.saml;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.KeycloakSamlAdapter;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.adapters.saml.config.parsers.KeycloakSamlAdapterXMLParser;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class XmlParserTest {

    @Test
    public void testValidation() throws Exception {
        {
            InputStream schema = KeycloakSamlAdapterXMLParser.class.getResourceAsStream("/schema/keycloak_saml_adapter_1_6.xsd");
            InputStream is = getClass().getResourceAsStream("/keycloak-saml.xml");
            Assert.assertNotNull(is);
            Assert.assertNotNull(schema);
            StaxParserUtil.validate(is, schema);
        }
        {
            InputStream sch = KeycloakSamlAdapterXMLParser.class.getResourceAsStream("/schema/keycloak_saml_adapter_1_6.xsd");
            InputStream doc = getClass().getResourceAsStream("/keycloak-saml2.xml");
            Assert.assertNotNull(doc);
            Assert.assertNotNull(sch);
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                Schema schema = factory.newSchema(new StreamSource(sch));
                Validator validator = schema.newValidator();
                StreamSource source = new StreamSource(doc);
                source.setSystemId("/keycloak-saml2.xml");
                validator.validate(source);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }


    }

    @Test
    public void testXmlParser() throws Exception {
        InputStream is = getClass().getResourceAsStream("/keycloak-saml.xml");
        Assert.assertNotNull(is);
        KeycloakSamlAdapterXMLParser parser = new KeycloakSamlAdapterXMLParser();
        KeycloakSamlAdapter config = (KeycloakSamlAdapter)parser.parse(is);
        Assert.assertNotNull(config);
        Assert.assertEquals(1, config.getSps().size());
        SP sp = config.getSps().get(0);
        Assert.assertEquals("sp", sp.getEntityID());
        Assert.assertEquals("ssl", sp.getSslPolicy());
        Assert.assertEquals("format", sp.getNameIDPolicyFormat());
        Assert.assertTrue(sp.isForceAuthentication());
        Assert.assertTrue(sp.isIsPassive());
        Assert.assertEquals(2, sp.getKeys().size());
        Key signing = sp.getKeys().get(0);
        Assert.assertTrue(signing.isSigning());
        Key.KeyStoreConfig keystore = signing.getKeystore();
        Assert.assertNotNull(keystore);
        Assert.assertEquals("file", keystore.getFile());
        Assert.assertEquals("cp", keystore.getResource());
        Assert.assertEquals("pw", keystore.getPassword());
        Assert.assertEquals("private alias", keystore.getPrivateKeyAlias());
        Assert.assertEquals("private pw", keystore.getPrivateKeyPassword());
        Assert.assertEquals("cert alias", keystore.getCertificateAlias());
        Key encryption = sp.getKeys().get(1);
        Assert.assertTrue(encryption.isEncryption());
        Assert.assertEquals("private pem", encryption.getPrivateKeyPem());
        Assert.assertEquals("public pem", encryption.getPublicKeyPem());
        Assert.assertEquals("policy", sp.getPrincipalNameMapping().getPolicy());
        Assert.assertEquals("attribute", sp.getPrincipalNameMapping().getAttributeName());
        Assert.assertTrue(sp.getRoleAttributes().size() == 1);
        Assert.assertTrue(sp.getRoleAttributes().contains("member"));

        IDP idp = sp.getIdp();
        Assert.assertEquals("idp", idp.getEntityID());
        Assert.assertEquals("RSA", idp.getSignatureAlgorithm());
        Assert.assertEquals("canon", idp.getSignatureCanonicalizationMethod());
        Assert.assertTrue(idp.getSingleSignOnService().isSignRequest());
        Assert.assertTrue(idp.getSingleSignOnService().isValidateResponseSignature());
        Assert.assertEquals("post", idp.getSingleSignOnService().getRequestBinding());
        Assert.assertEquals("url", idp.getSingleSignOnService().getBindingUrl());

        Assert.assertTrue(idp.getSingleLogoutService().isSignRequest());
        Assert.assertTrue(idp.getSingleLogoutService().isSignResponse());
        Assert.assertTrue(idp.getSingleLogoutService().isValidateRequestSignature());
        Assert.assertTrue(idp.getSingleLogoutService().isValidateResponseSignature());
        Assert.assertEquals("redirect", idp.getSingleLogoutService().getRequestBinding());
        Assert.assertEquals("post", idp.getSingleLogoutService().getResponseBinding());
        Assert.assertEquals("posturl", idp.getSingleLogoutService().getPostBindingUrl());
        Assert.assertEquals("redirecturl", idp.getSingleLogoutService().getRedirectBindingUrl());

        Assert.assertTrue(idp.getKeys().size() == 1);
        Assert.assertTrue(idp.getKeys().get(0).isSigning());
        Assert.assertEquals("cert pem", idp.getKeys().get(0).getCertificatePem());




    }
}
