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
package org.keycloak.testsuite.saml;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.saml.SPMetadataDescriptor;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ValidationTest {

    public static String getIDPMetadataDescriptor() throws IOException {
        InputStream is = SamlService.class.getResourceAsStream("/idp-metadata-template.xml");
        String template = StreamUtil.readString(is);
        template = template.replace("${idp.entityID}", "http://keycloak.org/auth/realms/test");
        template = template.replace("${idp.sso.HTTP-POST}", "http://keycloak.org/auth/realms/test/saml");
        template = template.replace("${idp.sso.HTTP-Redirect}", "http://keycloak.org/auth/realms/test/saml");
        template = template.replace("${idp.sls.HTTP-POST}", "http://keycloak.org/auth/realms/test/saml");
        template = template.replace("${idp.signing.certificate}", KeycloakModelUtils.generateKeyPairCertificate("test").getCertificate());
        return template;
    }


    @Test
    @Ignore // ignore because it goes out to web
    public void testIDPDescriptor() throws Exception {
        URL schemaFile = getClass().getResource("/schema/saml/v2/saml-schema-metadata-2.0.xsd");
        Source xmlFile = new StreamSource(new ByteArrayInputStream(getIDPMetadataDescriptor().getBytes()), "IDPSSODescriptor");
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid");
            System.out.println("Reason: " + e.getLocalizedMessage());
            Assert.fail();
        }
    }
    @Test
    @Ignore // ignore because it goes out to web
    public void testBrokerExportDescriptor() throws Exception {
        URL schemaFile = getClass().getResource("/schema/saml/v2/saml-schema-metadata-2.0.xsd");
        Source xmlFile = new StreamSource(new ByteArrayInputStream(SPMetadataDescriptor.getSPDescriptor(
                "POST", "http://realm/assertion", "http://realm/logout", true, false, false, "test", SamlProtocol.SAML_DEFAULT_NAMEID_FORMAT, KeycloakModelUtils.generateKeyPairCertificate("test").getCertificate(), ""
        ).getBytes()), "SP Descriptor");
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid");
            System.out.println("Reason: " + e.getLocalizedMessage());
            Assert.fail();
        }
    }
}
