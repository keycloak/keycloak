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

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.mdattr.EntityAttributes;
import org.keycloak.dom.saml.v2.mdrpi.RegistrationInfoType;
import org.keycloak.dom.saml.v2.mdui.LogoType;
import org.keycloak.dom.saml.v2.mdui.UIInfoType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.OrganizationType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.core.saml.v2.common.IDGenerator;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.model.SPDescriptorModel;
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
    
    private static final String ENTITY_CATEGORY_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    
        
    
    public static String getSPDescriptor(SPDescriptorModel sp)
        throws XMLStreamException, ProcessingException, ParserConfigurationException {

        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
        SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);

        EntityDescriptorType entityDescriptor = new EntityDescriptorType(sp.getEntityId());
        entityDescriptor.setID(IDGenerator.create("ID_")); 
        ExtensionsType entityExtension = new ExtensionsType();
        if (sp.getRegistrationAuthority() != null ) {            
            RegistrationInfoType registrationInfo = new RegistrationInfoType(URI.create(sp.getRegistrationAuthority()));
            if (sp.getRegistrationPolicy() != null) {
                LocalizedURIType policy = new LocalizedURIType(sp.getLocal());
                policy.setValue(URI.create(sp.getRegistrationPolicy()));
                registrationInfo.addRegistrationPolicy(policy);
            }
            entityExtension.addExtension(registrationInfo);
           
        }
        if (( sp.getSamlAttributes() != null && ! sp.getSamlAttributes().isEmpty())) {
            EntityAttributes attributes = new EntityAttributes();
            for (Entry<String, String> entry : sp.getSamlAttributes().entrySet()) {
                AttributeType attributeType = new AttributeType(entry.getKey());
                String[] values = entry.getValue().split(",");
                for (int i =0;i < values.length;i++) {
                    attributeType.addAttributeValue(values[i]);
                }                
                attributes.addAttribute(attributeType);
            }
           
            entityExtension.addExtension(attributes);
        }
        if (!entityExtension.getAny().isEmpty())
            entityDescriptor.setExtensions(entityExtension);

        SPSSODescriptorType spSSODescriptor = new SPSSODescriptorType(Arrays.asList(PROTOCOL_NSURI.get()));
        spSSODescriptor.setAuthnRequestsSigned(sp.isWantAuthnRequestsSigned());
        spSSODescriptor.setWantAssertionsSigned(sp.isWantAssertionsSigned());
        spSSODescriptor.addNameIDFormat(sp.getNameIDPolicyFormat());
        spSSODescriptor.addSingleLogoutService(new EndpointType(sp.getBinding(), sp.getLogoutEndpoint()));

        for (Element key : sp.getSigningCerts()) {
            KeyDescriptorType keyDescriptor = new KeyDescriptorType();
            keyDescriptor.setUse(KeyTypes.SIGNING);
            keyDescriptor.setKeyInfo(key);
            spSSODescriptor.addKeyDescriptor(keyDescriptor);
        }

        if (sp.isWantAssertionsEncrypted()) {
            for (Element key : sp.getEncryptionCerts()) {
                KeyDescriptorType keyDescriptor = new KeyDescriptorType();
                keyDescriptor.setUse(KeyTypes.ENCRYPTION);
                keyDescriptor.setKeyInfo(key);
                spSSODescriptor.addKeyDescriptor(keyDescriptor);
            }
        }

        IndexedEndpointType assertionConsumerEndpoint = new IndexedEndpointType(sp.getBinding(), sp.getAssertionEndpoint());
        assertionConsumerEndpoint.setIsDefault(true);
        assertionConsumerEndpoint.setIndex(1);
        spSSODescriptor.addAssertionConsumerService(assertionConsumerEndpoint);
        UIInfoType uiInfo = new UIInfoType();
        AttributeConsumingServiceType consumingService = new AttributeConsumingServiceType(0);
        boolean hasUIInfo = false;
        boolean hasAttributes = false;
        if (sp.getDisplayName() != null && !sp.getDisplayName().isEmpty()) {
            LocalizedNameType displayNameTag = new LocalizedNameType(sp.getLocal());
            displayNameTag.setValue(sp.getDisplayName());
            uiInfo.addDisplayName(displayNameTag);
            consumingService.addServiceName(displayNameTag);
            hasUIInfo = true;
            hasAttributes = true;
        }
        if (sp.getDescription() != null && !sp.getDescription().isEmpty()) {
            LocalizedNameType descriptionTag = new LocalizedNameType(sp.getLocal());
            descriptionTag.setValue(sp.getDescription());
            uiInfo.addDescription(descriptionTag);
            consumingService.addServiceDescription(descriptionTag);
            hasUIInfo = true;
            hasAttributes = true;
        }
        if (sp.getInformationURL() != null) {
            LocalizedURIType informationTag = new LocalizedURIType(sp.getLocal());
            informationTag.setValue(URI.create(sp.getInformationURL()));
            uiInfo.addInformationURL(informationTag);
            hasUIInfo = true;
        }
        if (sp.getPrivacyStatementURL() != null) {
            LocalizedURIType privacyTag = new LocalizedURIType(sp.getLocal());
            privacyTag.setValue(URI.create(sp.getPrivacyStatementURL()));
            uiInfo.addPrivacyStatementURL(privacyTag);
            hasUIInfo = true;
        }
        if (sp.getLogo() != null && sp.getLogoWidth() != null && sp.getLogoHeight() != null) {
            LogoType logoTag = new LogoType(sp.getLogoHeight(), sp.getLogoWidth());
            logoTag.setValue(URI.create(sp.getLogo()));
            uiInfo.addLogo(logoTag);
            hasUIInfo = true;
        }
        if (hasUIInfo) {
            ExtensionsType extension = new ExtensionsType();
            extension.addExtension(uiInfo);
            spSSODescriptor.setExtensions(extension);
        }
        if (!sp.getMappers().isEmpty()) {
            hasAttributes = true;
            for (List<String> mappersValues : sp.getMappers()) {
                RequestedAttributeType attribute = new RequestedAttributeType(mappersValues.get(0));
                if (mappersValues.size() == 2) {
                    attribute.setFriendlyName(mappersValues.get(1));
                    consumingService.addRequestedAttribute(attribute);
                }
            }
        }
        if (hasAttributes)
            spSSODescriptor.addAttributeConsumerService(consumingService);

        if (sp.getOrganizationName() != null) {
            OrganizationType organization = new OrganizationType();
            LocalizedNameType name = new LocalizedNameType(sp.getLocal());
            name.setValue(sp.getOrganizationName());
            organization.addOrganizationName(name);
            LocalizedNameType displayName = new LocalizedNameType(sp.getLocal());
            displayName.setValue(sp.getOrganizationDisplayName());
            organization.addOrganizationDisplayName(displayName);
            LocalizedURIType url = new LocalizedURIType(sp.getLocal());
            url.setValue(URI.create(sp.getOrganizationURL()));
            organization.addOrganizationURL(url);
            entityDescriptor.setOrganization(organization);
        }

        if (sp.getContactType() != null) {
            ContactType contact = new ContactType(sp.getContactType());
            contact.setCompany(sp.getContactCompany());
            contact.setGivenName(sp.getContactGivenName());
            contact.setSurName(sp.getContactSurname());
            if (sp.getContactEmailAddresses() !=null)
                contact.setEmailAddress(sp.getContactEmailAddresses());
            if (sp.getContactTelephoneNumbers() !=null)
                contact.setTelephoneNumber(sp.getContactTelephoneNumbers());
            entityDescriptor.addContactPerson(contact);
        }

        entityDescriptor.addChoiceType(new EntityDescriptorType.EDTChoiceType(
            Arrays.asList(new EntityDescriptorType.EDTDescriptorChoiceType(spSSODescriptor))));
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
