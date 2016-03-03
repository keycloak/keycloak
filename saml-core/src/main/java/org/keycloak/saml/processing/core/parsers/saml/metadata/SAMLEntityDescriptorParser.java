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
package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.AttributeAuthorityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.OrganizationType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.RoleDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptionMethodType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;

/**
 * Parse the SAML Metadata element "EntityDescriptor"
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 14, 2010
 */
public class SAMLEntityDescriptorParser extends AbstractDescriptorParser implements ParserNamespaceSupport {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private final String EDT = JBossSAMLConstants.ENTITY_DESCRIPTOR.get();

    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {

        xmlEventReader = filterWhiteSpaceCharacters(xmlEventReader);

        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, EDT);

        Attribute entityID = startElement.getAttributeByName(new QName(JBossSAMLConstants.ENTITY_ID.get()));
        String entityIDValue = StaxParserUtil.getAttributeValue(entityID);
        EntityDescriptorType entityDescriptorType = new EntityDescriptorType(entityIDValue);

        Attribute validUntil = startElement.getAttributeByName(new QName(JBossSAMLConstants.VALID_UNTIL.get()));
        if (validUntil != null) {
            String validUntilValue = StaxParserUtil.getAttributeValue(validUntil);
            entityDescriptorType.setValidUntil(XMLTimeUtil.parse(validUntilValue));
        }

        Attribute id = startElement.getAttributeByName(new QName(JBossSAMLConstants.ID.get()));
        if (id != null) {
            entityDescriptorType.setID(StaxParserUtil.getAttributeValue(id));
        }

        Attribute cacheDuration = startElement.getAttributeByName(new QName(JBossSAMLConstants.CACHE_DURATION.get()));
        if (cacheDuration != null) {
            entityDescriptorType.setCacheDuration(XMLTimeUtil.parseAsDuration(StaxParserUtil.getAttributeValue(cacheDuration)));
        }

        // Get the Child Elements
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                StaxParserUtil.validate((EndElement) xmlEvent, EDT);
                StaxParserUtil.getNextEndElement(xmlEventReader);
                break;
            }
            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.IDP_SSO_DESCRIPTOR.get().equals(localPart)) {
                IDPSSODescriptorType idpSSO = parseIDPSSODescriptor(xmlEventReader);

                EntityDescriptorType.EDTDescriptorChoiceType edtDescChoice = new EntityDescriptorType.EDTDescriptorChoiceType(idpSSO);
                EntityDescriptorType.EDTChoiceType edtChoice = EntityDescriptorType.EDTChoiceType.oneValue(edtDescChoice);
                entityDescriptorType.addChoiceType(edtChoice);
            } else if (JBossSAMLConstants.SP_SSO_DESCRIPTOR.get().equals(localPart)) {
                SPSSODescriptorType spSSO = parseSPSSODescriptor(xmlEventReader);

                EntityDescriptorType.EDTDescriptorChoiceType edtDescChoice = new EntityDescriptorType.EDTDescriptorChoiceType(spSSO);
                EntityDescriptorType.EDTChoiceType edtChoice = EntityDescriptorType.EDTChoiceType.oneValue(edtDescChoice);
                entityDescriptorType.addChoiceType(edtChoice);
            } else if (JBossSAMLConstants.ATTRIBUTE_AUTHORITY_DESCRIPTOR.get().equals(localPart)) {
                AttributeAuthorityDescriptorType attrAuthority = parseAttributeAuthorityDescriptor(xmlEventReader);

                EntityDescriptorType.EDTDescriptorChoiceType edtDescChoice = new EntityDescriptorType.EDTDescriptorChoiceType(attrAuthority);
                EntityDescriptorType.EDTChoiceType edtChoice = EntityDescriptorType.EDTChoiceType.oneValue(edtDescChoice);
                entityDescriptorType.addChoiceType(edtChoice);
            } else if (JBossSAMLConstants.AUTHN_AUTHORITY_DESCRIPTOR.get().equals(localPart)) {
                throw logger.unsupportedType("AuthnAuthorityDescriptor");
            } else if (JBossSAMLConstants.AFFILIATION_DESCRIPTOR.get().equals(localPart)) {
                throw logger.unsupportedType(" AffiliationDescriptor");
            } else if (JBossSAMLConstants.PDP_DESCRIPTOR.get().equals(localPart)) {
                throw logger.unsupportedType(" PDPDescriptor");
            } else if (localPart.equals(JBossSAMLConstants.SIGNATURE.get())) {
                entityDescriptorType.setSignature(StaxParserUtil.getDOMElement(xmlEventReader));
            } else if (JBossSAMLConstants.ORGANIZATION.get().equals(localPart)) {
                OrganizationType organization = parseOrganization(xmlEventReader);

                entityDescriptorType.setOrganization(organization);
            } else if (JBossSAMLConstants.CONTACT_PERSON.get().equals(localPart)) {
                entityDescriptorType.addContactPerson(parseContactPerson(xmlEventReader));
            } else if (JBossSAMLConstants.ADDITIONAL_METADATA_LOCATION.get().equals(localPart)) {
                throw logger.unsupportedType("AdditionalMetadataLocation");
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                entityDescriptorType.setExtensions(parseExtensions(xmlEventReader));
            } else if (JBossSAMLConstants.ROLE_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                RoleDescriptorType roleDescriptor = parseRoleDescriptor(xmlEventReader);

                EntityDescriptorType.EDTDescriptorChoiceType edtDescChoice = new EntityDescriptorType.EDTDescriptorChoiceType(roleDescriptor);
                EntityDescriptorType.EDTChoiceType edtChoice = EntityDescriptorType.EDTChoiceType.oneValue(edtDescChoice);

                entityDescriptorType.addChoiceType(edtChoice);
            } else
                throw logger.parserUnknownStartElement(localPart, startElement.getLocation());
        }
        return entityDescriptorType;
    }

    public boolean supports(QName qname) {
        String nsURI = qname.getNamespaceURI();
        String localPart = qname.getLocalPart();

        return nsURI.equals(JBossSAMLURIConstants.ASSERTION_NSURI.get())
                && localPart.equals(JBossSAMLConstants.ENTITY_DESCRIPTOR.get());
    }

    private SPSSODescriptorType parseSPSSODescriptor(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.SP_SSO_DESCRIPTOR.get());

        List<String> protocolEnum = SAMLParserUtil.parseProtocolEnumeration(startElement);
        SPSSODescriptorType spSSODescriptor = new SPSSODescriptorType(protocolEnum);

        Attribute wantAssertionsSigned = startElement.getAttributeByName(new QName(JBossSAMLConstants.WANT_ASSERTIONS_SIGNED
                .get()));
        if (wantAssertionsSigned != null) {
            spSSODescriptor
                    .setWantAssertionsSigned(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(wantAssertionsSigned)));
        }
        Attribute wantAuthnSigned = startElement.getAttributeByName(new QName(JBossSAMLConstants.AUTHN_REQUESTS_SIGNED.get()));
        if (wantAuthnSigned != null) {
            spSSODescriptor.setAuthnRequestsSigned(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(wantAuthnSigned)));
        }

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.SP_SSO_DESCRIPTOR.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.ARTIFACT_RESOLUTION_SERVICE.get().equals(localPart)) {
                IndexedEndpointType endpoint = parseArtifactResolutionService(xmlEventReader, startElement);
                spSSODescriptor.addArtifactResolutionService(endpoint);
            } else if (JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE.get().equals(localPart)) {
                IndexedEndpointType endpoint = parseAssertionConsumerService(xmlEventReader, startElement);
                spSSODescriptor.addAssertionConsumerService(endpoint);
            } else if (JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE.get().equals(localPart)) {
                AttributeConsumingServiceType attributeConsumer = parseAttributeConsumingService(xmlEventReader, startElement);
                spSSODescriptor.addAttributeConsumerService(attributeConsumer);
            } else if (JBossSAMLConstants.SINGLE_LOGOUT_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.SINGLE_LOGOUT_SERVICE.get());

                spSSODescriptor.addSingleLogoutService(endpoint);
            } else if (JBossSAMLConstants.MANAGE_NAMEID_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.MANAGE_NAMEID_SERVICE.get());

                spSSODescriptor.addManageNameIDService(endpoint);
            } else if (JBossSAMLConstants.NAMEID_FORMAT.get().equalsIgnoreCase(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                spSSODescriptor.addNameIDFormat(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.KEY_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                spSSODescriptor.addKeyDescriptor(parseKeyDescriptor(xmlEventReader));
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                spSSODescriptor.setExtensions(parseExtensions(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }
        return spSSODescriptor;
    }

    private IDPSSODescriptorType parseIDPSSODescriptor(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.IDP_SSO_DESCRIPTOR.get());

        List<String> protocolEnum = SAMLParserUtil.parseProtocolEnumeration(startElement);
        IDPSSODescriptorType idpSSODescriptor = new IDPSSODescriptorType(protocolEnum);

        Attribute wantAuthnSigned = startElement.getAttributeByName(new QName(JBossSAMLConstants.WANT_AUTHN_REQUESTS_SIGNED
                .get()));
        if (wantAuthnSigned != null) {
            idpSSODescriptor
                    .setWantAuthnRequestsSigned(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(wantAuthnSigned)));
        }

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.IDP_SSO_DESCRIPTOR.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.ARTIFACT_RESOLUTION_SERVICE.get().equals(localPart)) {
                IndexedEndpointType endpoint = parseArtifactResolutionService(xmlEventReader, startElement);
                idpSSODescriptor.addArtifactResolutionService(endpoint);
            } else if (JBossSAMLConstants.ASSERTION_ID_REQUEST_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.ASSERTION_ID_REQUEST_SERVICE.get());

                idpSSODescriptor.addAssertionIDRequestService(endpoint);
            } else if (JBossSAMLConstants.SINGLE_LOGOUT_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.SINGLE_LOGOUT_SERVICE.get());

                idpSSODescriptor.addSingleLogoutService(endpoint);
            } else if (JBossSAMLConstants.SINGLE_SIGNON_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.SINGLE_SIGNON_SERVICE.get());

                idpSSODescriptor.addSingleSignOnService(endpoint);
            } else if (JBossSAMLConstants.MANAGE_NAMEID_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.MANAGE_NAMEID_SERVICE.get());

                idpSSODescriptor.addManageNameIDService(endpoint);
            } else if (JBossSAMLConstants.NAMEID_MAPPING_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                EndpointType endpoint = getEndpointType(startElement);

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.NAMEID_MAPPING_SERVICE.get());

                idpSSODescriptor.addNameIDMappingService(endpoint);
            } else if (JBossSAMLConstants.NAMEID_FORMAT.get().equalsIgnoreCase(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                idpSSODescriptor.addNameIDFormat(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.ATTRIBUTE.get().equalsIgnoreCase(localPart)) {
                AttributeType attribute = SAMLParserUtil.parseAttribute(xmlEventReader);
                idpSSODescriptor.addAttribute(attribute);
            } else if (JBossSAMLConstants.KEY_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                idpSSODescriptor.addKeyDescriptor(parseKeyDescriptor(xmlEventReader));
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                idpSSODescriptor.setExtensions(parseExtensions(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }
        return idpSSODescriptor;
    }

    private EndpointType getEndpointType(StartElement startElement) {
        Attribute bindingAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.BINDING.get()));
        String binding = StaxParserUtil.getAttributeValue(bindingAttr);

        Attribute locationAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.LOCATION.get()));
        String location = StaxParserUtil.getAttributeValue(locationAttr);

        EndpointType endpoint = new IndexedEndpointType(URI.create(binding), URI.create(location));
        Attribute responseLocation = startElement.getAttributeByName(new QName(JBossSAMLConstants.RESPONSE_LOCATION.get()));
        if (responseLocation != null) {
            endpoint.setResponseLocation(URI.create(StaxParserUtil.getAttributeValue(responseLocation)));
        }
        return endpoint;
    }

    private AttributeAuthorityDescriptorType parseAttributeAuthorityDescriptor(XMLEventReader xmlEventReader)
            throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ATTRIBUTE_AUTHORITY_DESCRIPTOR.get());
        List<String> protocolEnum = SAMLParserUtil.parseProtocolEnumeration(startElement);
        AttributeAuthorityDescriptorType attributeAuthority = new AttributeAuthorityDescriptorType(protocolEnum);

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.ATTRIBUTE_AUTHORITY_DESCRIPTOR.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.ATTRIBUTE_SERVICE.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                Attribute bindingAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.BINDING.get()));
                String binding = StaxParserUtil.getAttributeValue(bindingAttr);

                Attribute locationAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.LOCATION.get()));
                String location = StaxParserUtil.getAttributeValue(locationAttr);

                IndexedEndpointType endpoint = new IndexedEndpointType(URI.create(binding), URI.create(location));

                EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(endElement, JBossSAMLConstants.ATTRIBUTE_SERVICE.get());

                attributeAuthority.addAttributeService(endpoint);
            } else if (JBossSAMLConstants.KEY_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                attributeAuthority.addKeyDescriptor(parseKeyDescriptor(xmlEventReader));
            } else if (JBossSAMLConstants.NAMEID_FORMAT.get().equalsIgnoreCase(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                attributeAuthority.addNameIDFormat(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                attributeAuthority.setExtensions(parseExtensions(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());

        }
        return attributeAuthority;
    }

    private OrganizationType parseOrganization(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ORGANIZATION.get());

        OrganizationType org = new OrganizationType();

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.ORGANIZATION.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.ORGANIZATION_NAME.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                LocalizedNameType localName = getLocalizedName(xmlEventReader, startElement);
                org.addOrganizationName(localName);
            } else if (JBossSAMLConstants.ORGANIZATION_DISPLAY_NAME.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                LocalizedNameType localName = getLocalizedName(xmlEventReader, startElement);
                org.addOrganizationDisplayName(localName);
            } else if (JBossSAMLConstants.ORGANIZATION_URL.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                Attribute lang = startElement.getAttributeByName(new QName(JBossSAMLURIConstants.XML.get(), "lang"));
                String langVal = StaxParserUtil.getAttributeValue(lang);
                LocalizedURIType localName = new LocalizedURIType(langVal);
                localName.setValue(URI.create(StaxParserUtil.getElementText(xmlEventReader)));
                org.addOrganizationURL(localName);
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                org.setExtensions(parseExtensions(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }
        return org;
    }

    private KeyDescriptorType parseKeyDescriptor(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.KEY_DESCRIPTOR.get());

        KeyDescriptorType keyDescriptor = new KeyDescriptorType();

        String use = StaxParserUtil.getAttributeValue(startElement, "use");

        if (use != null && !use.isEmpty()) {
            keyDescriptor.setUse(KeyTypes.fromValue(use));
        }

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.KEY_DESCRIPTOR.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.KEY_INFO.get().equals(localPart)) {
                Element key = StaxParserUtil.getDOMElement(xmlEventReader);
                keyDescriptor.setKeyInfo(key);
            } else if (JBossSAMLConstants.ENCRYPTION_METHOD.get().equals(localPart)) {
                keyDescriptor.addEncryptionMethod(parseEncryptionMethod(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }

        return keyDescriptor;
    }

    private EncryptionMethodType parseEncryptionMethod(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ENCRYPTION_METHOD.get());
        Attribute algorithm = startElement.getAttributeByName(new QName("Algorithm"));
        EncryptionMethodType encryptionMethodType = new EncryptionMethodType(algorithm.getValue());

        BigInteger keySize = null;
        byte[] OAEPparams = null;

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.ENCRYPTION_METHOD.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if ("KeySize".equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                keySize = BigInteger.valueOf(Long.valueOf(StaxParserUtil.getElementText(xmlEventReader)));
            } else if ("OAEPparams".equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                OAEPparams = StaxParserUtil.getElementText(xmlEventReader).getBytes();
            } else {
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
            }
        }

        EncryptionMethodType.EncryptionMethod encryptionMethod = new EncryptionMethodType.EncryptionMethod(keySize, OAEPparams);

        encryptionMethodType.setEncryptionMethod(encryptionMethod);

        return encryptionMethodType;
    }

    private ContactType parseContactPerson(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.CONTACT_PERSON.get());

        Attribute attr = startElement.getAttributeByName(new QName(JBossSAMLConstants.CONTACT_TYPE.get()));
        if (attr == null)
            throw logger.parserRequiredAttribute("contactType");
        ContactType contactType = new ContactType(ContactTypeType.fromValue(StaxParserUtil.getAttributeValue(attr)));

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.CONTACT_PERSON.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.COMPANY.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                contactType.setCompany(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.GIVEN_NAME.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                contactType.setGivenName(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.SURNAME.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                contactType.setSurName(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.EMAIL_ADDRESS.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                contactType.addEmailAddress(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.TELEPHONE_NUMBER.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                contactType.addTelephone(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.EXTENSIONS.get().equalsIgnoreCase(localPart)) {
                contactType.setExtensions(parseExtensions(xmlEventReader));
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }
        return contactType;
    }

    private LocalizedNameType getLocalizedName(XMLEventReader xmlEventReader, StartElement startElement)
            throws ParsingException {
        Attribute lang = startElement.getAttributeByName(new QName(JBossSAMLURIConstants.XML.get(), "lang"));
        String langVal = StaxParserUtil.getAttributeValue(lang);
        LocalizedNameType localName = new LocalizedNameType(langVal);
        localName.setValue(StaxParserUtil.getElementText(xmlEventReader));
        return localName;
    }

    private IndexedEndpointType parseAssertionConsumerService(XMLEventReader xmlEventReader, StartElement startElement)
            throws ParsingException {
        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        IndexedEndpointType endpoint = parseIndexedEndpoint(xmlEventReader, startElement);

        EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
        StaxParserUtil.validate(endElement, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE.get());

        return endpoint;
    }

    private IndexedEndpointType parseArtifactResolutionService(XMLEventReader xmlEventReader, StartElement startElement)
            throws ParsingException {
        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        IndexedEndpointType endpoint = parseIndexedEndpoint(xmlEventReader, startElement);

        EndElement endElement = StaxParserUtil.getNextEndElement(xmlEventReader);
        StaxParserUtil.validate(endElement, JBossSAMLConstants.ARTIFACT_RESOLUTION_SERVICE.get());

        return endpoint;
    }

    private IndexedEndpointType parseIndexedEndpoint(XMLEventReader xmlEventReader, StartElement startElement) {
        Attribute bindingAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.BINDING.get()));
        String binding = StaxParserUtil.getAttributeValue(bindingAttr);

        Attribute locationAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.LOCATION.get()));
        String location = StaxParserUtil.getAttributeValue(locationAttr);

        IndexedEndpointType endpoint = new IndexedEndpointType(URI.create(binding), URI.create(location));
        Attribute isDefault = startElement.getAttributeByName(new QName(JBossSAMLConstants.ISDEFAULT.get()));
        if (isDefault != null) {
            endpoint.setIsDefault(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(isDefault)));
        }
        Attribute index = startElement.getAttributeByName(new QName(JBossSAMLConstants.INDEX.get()));
        if (index != null) {
            endpoint.setIndex(Integer.parseInt(StaxParserUtil.getAttributeValue(index)));
        }
        return endpoint;
    }

    private AttributeConsumingServiceType parseAttributeConsumingService(XMLEventReader xmlEventReader,
                                                                         StartElement startElement) throws ParsingException {
        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        Attribute indexAttr = startElement.getAttributeByName(new QName(JBossSAMLConstants.INDEX.get()));
        if (indexAttr == null)
            throw logger.parserRequiredAttribute("index");

        AttributeConsumingServiceType attributeConsumer = new AttributeConsumingServiceType(Integer.parseInt(StaxParserUtil
                .getAttributeValue(indexAttr)));
        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.SERVICE_NAME.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                LocalizedNameType localName = getLocalizedName(xmlEventReader, startElement);
                attributeConsumer.addServiceName(localName);
            } else if (JBossSAMLConstants.SERVICE_DESCRIPTION.get().equals(localPart)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                LocalizedNameType localName = getLocalizedName(xmlEventReader, startElement);
                attributeConsumer.addServiceDescription(localName);
            } else if (JBossSAMLConstants.REQUESTED_ATTRIBUTE.get().equals(localPart)) {
                RequestedAttributeType attType = parseRequestedAttributeType(xmlEventReader, startElement);
                attributeConsumer.addRequestedAttribute(attType);
            } else
                throw logger.parserUnknownTag(localPart, startElement.getLocation());
        }

        return attributeConsumer;
    }

    private RequestedAttributeType parseRequestedAttributeType(XMLEventReader xmlEventReader, StartElement startElement)
            throws ParsingException {
        startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.REQUESTED_ATTRIBUTE.get());
        RequestedAttributeType attributeType = null;

        Attribute name = startElement.getAttributeByName(new QName(JBossSAMLConstants.NAME.get()));
        if (name == null)
            throw logger.parserRequiredAttribute("Name");
        attributeType = new RequestedAttributeType(StaxParserUtil.getAttributeValue(name));

        Attribute isRequired = startElement.getAttributeByName(new QName(JBossSAMLConstants.IS_REQUIRED.get()));
        if (isRequired != null) {
            attributeType.setIsRequired(Boolean.parseBoolean(StaxParserUtil.getAttributeValue(isRequired)));
        }

        SAMLParserUtil.parseAttributeType(xmlEventReader, startElement, JBossSAMLConstants.REQUESTED_ATTRIBUTE.get(),
                attributeType);
        return attributeType;
    }

    private ExtensionsType parseExtensions(XMLEventReader xmlEventReader) throws ParsingException {
        ExtensionsType extensions = new ExtensionsType();
        Element extElement = StaxParserUtil.getDOMElement(xmlEventReader);
        extensions.setElement(extElement);
        return extensions;
    }

    private RoleDescriptorType parseRoleDescriptor(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ROLE_DESCRIPTOR.get());
        List<String> protocolEnum = SAMLParserUtil.parseProtocolEnumeration(startElement);
        RoleDescriptorType roleDescriptorType = new RoleDescriptorType(protocolEnum) {};

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = StaxParserUtil.peek(xmlEventReader);
            if (xmlEvent instanceof EndElement) {
                EndElement end = StaxParserUtil.getNextEndElement(xmlEventReader);
                StaxParserUtil.validate(end, JBossSAMLConstants.ROLE_DESCRIPTOR.get());
                break;
            }

            startElement = (StartElement) xmlEvent;
            String localPart = startElement.getName().getLocalPart();

            if (JBossSAMLConstants.KEY_DESCRIPTOR.get().equalsIgnoreCase(localPart)) {
                KeyDescriptorType keyDescriptor = parseKeyDescriptor(xmlEventReader);
                roleDescriptorType.addKeyDescriptor(keyDescriptor);
            } else {
                StaxParserUtil.bypassElementBlock(xmlEventReader, localPart);
            }
        }

        return roleDescriptorType;
    }
}