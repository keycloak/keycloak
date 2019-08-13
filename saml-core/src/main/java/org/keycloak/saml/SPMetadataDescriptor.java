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

package org.keycloak.saml;

import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.XMLDSIG_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SPMetadataDescriptor {

    public static String getSPDescriptor(URI binding, URI assertionEndpoint, URI logoutEndpoint,
                                         boolean wantAuthnRequestsSigned, boolean wantAssertionsSigned, boolean wantAssertionsEncrypted,
                                         String entityId, String nameIDPolicyFormat, List<Element> signingCerts, List<Element> encryptionCerts)
            throws XMLStreamException, ProcessingException, ParserConfigurationException
    {
        return getSPDescriptor(binding, binding, assertionEndpoint, logoutEndpoint, wantAuthnRequestsSigned,
                wantAssertionsSigned, wantAssertionsEncrypted, entityId, nameIDPolicyFormat, signingCerts,
                encryptionCerts);
    }

    public static String getSPDescriptor(URI loginBinding, URI logoutBinding, URI assertionEndpoint, URI logoutEndpoint,
        boolean wantAuthnRequestsSigned, boolean wantAssertionsSigned, boolean wantAssertionsEncrypted,
        String entityId, String nameIDPolicyFormat, List<Element> signingCerts, List<Element> encryptionCerts) 
        throws XMLStreamException, ProcessingException, ParserConfigurationException
    {
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
        SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);

        EntityDescriptorType entityDescriptor = new EntityDescriptorType(entityId);
        entityDescriptor.setID(IDGenerator.create("ID_"));

        SPSSODescriptorType spSSODescriptor = new SPSSODescriptorType(Arrays.asList(PROTOCOL_NSURI.get()));
        spSSODescriptor.setAuthnRequestsSigned(wantAuthnRequestsSigned);
        spSSODescriptor.setWantAssertionsSigned(wantAssertionsSigned);
        spSSODescriptor.addNameIDFormat(nameIDPolicyFormat);
        spSSODescriptor.addSingleLogoutService(new EndpointType(logoutBinding, logoutEndpoint));

        if (wantAuthnRequestsSigned && signingCerts != null) {
            for (Element key: signingCerts)
            {
                KeyDescriptorType keyDescriptor = new KeyDescriptorType();
                keyDescriptor.setUse(KeyTypes.SIGNING);
                keyDescriptor.setKeyInfo(key);
                spSSODescriptor.addKeyDescriptor(keyDescriptor);
            }
        }

        if (wantAssertionsEncrypted && encryptionCerts != null) {
            for (Element key: encryptionCerts)
            {
                KeyDescriptorType keyDescriptor = new KeyDescriptorType();
                keyDescriptor.setUse(KeyTypes.ENCRYPTION);
                keyDescriptor.setKeyInfo(key);
                spSSODescriptor.addKeyDescriptor(keyDescriptor);
            }
        }

        IndexedEndpointType assertionConsumerEndpoint = new IndexedEndpointType(loginBinding, assertionEndpoint);
        assertionConsumerEndpoint.setIsDefault(true);
        assertionConsumerEndpoint.setIndex(1);
        spSSODescriptor.addAssertionConsumerService(assertionConsumerEndpoint);

        entityDescriptor.addChoiceType(new EntityDescriptorType.EDTChoiceType(Arrays.asList(new EntityDescriptorType.EDTDescriptorChoiceType(spSSODescriptor))));
        metadataWriter.writeEntityDescriptor(entityDescriptor);

        return sw.toString();
    }

    public static Element buildKeyInfoElement(String keyName, String pemEncodedCertificate)
        throws javax.xml.parsers.ParserConfigurationException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element keyInfo = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:KeyInfo");

        if (keyName != null) {
            Element keyNameElement = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:KeyName");
            keyNameElement.setTextContent(keyName);
            keyInfo.appendChild(keyNameElement);
        }

        Element x509Data = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:X509Data");

        Element x509Certificate = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:X509Certificate");
        x509Certificate.setTextContent(pemEncodedCertificate);
      
        x509Data.appendChild(x509Certificate);

        keyInfo.appendChild(x509Data);

        return keyInfo;
    }
}
