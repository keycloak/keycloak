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
package org.keycloak.saml.processing.core.saml.v2.writers;

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.AdditionalMetadataLocationType;
import org.keycloak.dom.saml.v2.metadata.AffiliationDescriptorType;
import org.keycloak.dom.saml.v2.metadata.AttributeAuthorityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.AuthnAuthorityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.OrganizationType;
import org.keycloak.dom.saml.v2.metadata.PDPDescriptorType;
import org.keycloak.dom.saml.v2.metadata.RequestedAttributeType;
import org.keycloak.dom.saml.v2.metadata.RoleDescriptorType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.SSODescriptorType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import java.net.URI;
import java.util.List;

/**
 * Write the SAML metadata elements
 *
 * @author Anil.Saldhana@redhat.com
 * @since Dec 14, 2010
 */
public class SAMLMetadataWriter extends BaseWriter {

    private final String METADATA_PREFIX = "md";

    public SAMLMetadataWriter(XMLStreamWriter writer) {
        super(writer);
    }

    public void writeEntitiesDescriptor(EntitiesDescriptorType entities) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ENTITIES_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        StaxUtil.writeDefaultNameSpace(writer, JBossSAMLURIConstants.METADATA_NSURI.get());
        StaxUtil.writeNameSpace(writer, "md", JBossSAMLURIConstants.METADATA_NSURI.get());
        StaxUtil.writeNameSpace(writer, "saml", JBossSAMLURIConstants.ASSERTION_NSURI.get());
        StaxUtil.writeNameSpace(writer, "ds", JBossSAMLURIConstants.XMLDSIG_NSURI.get());

        if (entities.getValidUntil() != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.VALID_UNTIL.get(), entities.getValidUntil().toString());
        }
        if (entities.getID() != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), entities.getID());
        }

        if (entities.getName() != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NAME.get(), entities.getName());
        }

        Element signature = entities.getSignature();
        if (signature != null) {
            StaxUtil.writeDOMElement(writer, signature);
        }
        ExtensionsType extensions = entities.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        List<Object> entityDescriptors = entities.getEntityDescriptor();
        for (Object ed : entityDescriptors) {
            if (ed instanceof EntityDescriptorType) {
                writeEntityDescriptor((EntityDescriptorType) ed);
            } else
                writeEntitiesDescriptor((EntitiesDescriptorType) ed);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeEntityDescriptor(EntityDescriptorType entityDescriptor) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ENTITY_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
        StaxUtil.writeDefaultNameSpace(writer, JBossSAMLURIConstants.METADATA_NSURI.get());
        StaxUtil.writeNameSpace(writer, "md", JBossSAMLURIConstants.METADATA_NSURI.get());
        StaxUtil.writeNameSpace(writer, "saml", JBossSAMLURIConstants.ASSERTION_NSURI.get());
        StaxUtil.writeNameSpace(writer, "ds", JBossSAMLURIConstants.XMLDSIG_NSURI.get());

        StaxUtil.writeAttribute(writer, JBossSAMLConstants.ENTITY_ID.get(), entityDescriptor.getEntityID());
        if (entityDescriptor.getValidUntil() != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.VALID_UNTIL.get(), entityDescriptor.getValidUntil().toString());
        }
        if (entityDescriptor.getID() != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ID.get(), entityDescriptor.getID());
        }

        Element signature = entityDescriptor.getSignature();
        if (signature != null) {
            StaxUtil.writeDOMElement(writer, signature);
        }
        ExtensionsType extensions = entityDescriptor.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        List<EntityDescriptorType.EDTChoiceType> choiceTypes = entityDescriptor.getChoiceType();
        for (EntityDescriptorType.EDTChoiceType edtChoice : choiceTypes) {
            AffiliationDescriptorType affliationDesc = edtChoice.getAffiliationDescriptor();
            if (affliationDesc != null)
                throw logger.notImplementedYet("affliation"); // TODO: affiliation

            List<EntityDescriptorType.EDTDescriptorChoiceType> edtDescChoices = edtChoice.getDescriptors();
            for (EntityDescriptorType.EDTDescriptorChoiceType edtDescChoice : edtDescChoices) {
                RoleDescriptorType roleDesc = edtDescChoice.getRoleDescriptor();

                if (roleDesc != null)
                    throw logger.notImplementedYet("Role Descriptor type");

                IDPSSODescriptorType idpSSO = edtDescChoice.getIdpDescriptor();
                if (idpSSO != null)
                    write(idpSSO);

                SPSSODescriptorType spSSO = edtDescChoice.getSpDescriptor();
                if (spSSO != null)
                    write(spSSO);

                AttributeAuthorityDescriptorType attribAuth = edtDescChoice.getAttribDescriptor();
                if (attribAuth != null)
                    writeAttributeAuthorityDescriptor(attribAuth);

                AuthnAuthorityDescriptorType authNDesc = edtDescChoice.getAuthnDescriptor();
                if (authNDesc != null)
                    throw logger.notImplementedYet("AuthnAuthorityDescriptorType");

                PDPDescriptorType pdpDesc = edtDescChoice.getPdpDescriptor();
                if (pdpDesc != null)
                    throw logger.notImplementedYet("PDPDescriptorType");
            }
        }
        OrganizationType organization = entityDescriptor.getOrganization();
        if (organization != null) {
            writeOrganization(organization);
        }

        List<ContactType> contactPersons = entityDescriptor.getContactPerson();
        for (ContactType contact : contactPersons) {
            write(contact);
        }

        List<AdditionalMetadataLocationType> addl = entityDescriptor.getAdditionalMetadataLocation();
        if (addl.size() > 0)
            throw logger.notImplementedYet("AdditionalMetadataLocationType");

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(SSODescriptorType ssoDescriptor) throws ProcessingException {
        throw new RuntimeException("should not be called");
    }

    public void write(SPSSODescriptorType spSSODescriptor) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.SP_SSO_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
        writeProtocolSupportEnumeration(spSSODescriptor.getProtocolSupportEnumeration());

        // Write the attributes
        Boolean authnSigned = spSSODescriptor.isAuthnRequestsSigned();
        if (authnSigned != null) {
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.AUTHN_REQUESTS_SIGNED.get()),
                    authnSigned.toString());
        }
        Boolean wantAssertionsSigned = spSSODescriptor.isWantAssertionsSigned();
        if (wantAssertionsSigned != null) {
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.WANT_ASSERTIONS_SIGNED.get()),
                    wantAssertionsSigned.toString());
        }

        // Get the key descriptors
        List<KeyDescriptorType> keyDescriptors = spSSODescriptor.getKeyDescriptor();
        for (KeyDescriptorType keyDescriptor : keyDescriptors) {
            writeKeyDescriptor(keyDescriptor);
        }

        List<EndpointType> sloServices = spSSODescriptor.getSingleLogoutService();
        for (EndpointType endpoint : sloServices) {
            writeSingleLogoutService(endpoint);
        }

        List<IndexedEndpointType> artifactResolutions = spSSODescriptor.getArtifactResolutionService();
        for (IndexedEndpointType artifactResolution : artifactResolutions) {
            writeArtifactResolutionService(artifactResolution);
        }

        List<String> nameIDFormats = spSSODescriptor.getNameIDFormat();
        for (String nameIDFormat : nameIDFormats) {
            writeNameIDFormat(nameIDFormat);
        }

        List<IndexedEndpointType> assertionConsumers = spSSODescriptor.getAssertionConsumerService();
        for (IndexedEndpointType assertionConsumer : assertionConsumers) {
            writeAssertionConsumerService(assertionConsumer);
        }

        List<AttributeConsumingServiceType> attributeConsumers = spSSODescriptor.getAttributeConsumingService();
        for (AttributeConsumingServiceType attributeConsumer : attributeConsumers) {
            writeAttributeConsumingService(attributeConsumer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(IDPSSODescriptorType idpSSODescriptor) throws ProcessingException {
        if (idpSSODescriptor == null)
            throw new ProcessingException(logger.nullArgumentError("IDPSSODescriptorType"));

        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.IDP_SSO_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        Boolean wantsAuthnRequestsSigned = idpSSODescriptor.isWantAuthnRequestsSigned();
        if (wantsAuthnRequestsSigned != null) {
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.WANT_AUTHN_REQUESTS_SIGNED.get()),
                    wantsAuthnRequestsSigned.toString());
        }
        writeProtocolSupportEnumeration(idpSSODescriptor.getProtocolSupportEnumeration());

        // Get the key descriptors
        List<KeyDescriptorType> keyDescriptors = idpSSODescriptor.getKeyDescriptor();
        for (KeyDescriptorType keyDescriptor : keyDescriptors) {
            writeKeyDescriptor(keyDescriptor);
        }

        List<IndexedEndpointType> artifactResolutionServices = idpSSODescriptor.getArtifactResolutionService();
        for (IndexedEndpointType indexedEndpoint : artifactResolutionServices) {
            writeArtifactResolutionService(indexedEndpoint);
        }

        List<EndpointType> sloServices = idpSSODescriptor.getSingleLogoutService();
        for (EndpointType endpoint : sloServices) {
            writeSingleLogoutService(endpoint);
        }

        List<String> nameIDFormats = idpSSODescriptor.getNameIDFormat();
        for (String nameIDFormat : nameIDFormats) {
            writeNameIDFormat(nameIDFormat);
        }

        List<EndpointType> ssoServices = idpSSODescriptor.getSingleSignOnService();
        for (EndpointType endpoint : ssoServices) {
            writeSingleSignOnService(endpoint);
        }

        List<AttributeType> attributes = idpSSODescriptor.getAttribute();
        for (AttributeType attribType : attributes) {
            write(attribType);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeAttributeAuthorityDescriptor(AttributeAuthorityDescriptorType attributeAuthority)
            throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ATTRIBUTE_AUTHORITY_DESCRIPTOR.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());

        writeProtocolSupportEnumeration(attributeAuthority.getProtocolSupportEnumeration());

        Element signature = attributeAuthority.getSignature();
        if (signature != null) {
            StaxUtil.writeDOMElement(writer, signature);
        }
        ExtensionsType extensions = attributeAuthority.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        List<KeyDescriptorType> keyDescriptorList = attributeAuthority.getKeyDescriptor();
        for (KeyDescriptorType keyDescriptor : keyDescriptorList) {
            writeKeyDescriptor(keyDescriptor);
        }

        List<EndpointType> attributeServices = attributeAuthority.getAttributeService();
        for (EndpointType endpoint : attributeServices) {
            writeAttributeService(endpoint);
        }

        List<String> nameIDFormats = attributeAuthority.getNameIDFormat();
        for (String nameIDFormat : nameIDFormats) {
            writeNameIDFormat(nameIDFormat);
        }

        List<AttributeType> attributes = attributeAuthority.getAttribute();
        for (AttributeType attributeType : attributes) {
            write(attributeType);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeArtifactResolutionService(IndexedEndpointType indexedEndpoint) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ARTIFACT_RESOLUTION_SERVICE.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());

        writeIndexedEndpointType(indexedEndpoint);
    }

    public void writeAssertionConsumerService(IndexedEndpointType indexedEndpoint) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());
        writeIndexedEndpointType(indexedEndpoint);
    }

    public void writeIndexedEndpointType(IndexedEndpointType indexedEndpoint) throws ProcessingException {
        writeEndpointType(indexedEndpoint);
        if (indexedEndpoint.isIsDefault() != null)
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISDEFAULT.get(), "" + indexedEndpoint.isIsDefault());

        StaxUtil.writeAttribute(writer, JBossSAMLConstants.INDEX.get(), "" + indexedEndpoint.getIndex());

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeAttributeConsumingService(AttributeConsumingServiceType attributeConsumer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ATTRIBUTE_CONSUMING_SERVICE.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());

        if (attributeConsumer.isIsDefault() != null)
           StaxUtil.writeAttribute(writer, JBossSAMLConstants.ISDEFAULT.get(), "" + attributeConsumer.isIsDefault());

        StaxUtil.writeAttribute(writer, JBossSAMLConstants.INDEX.get(), "" + attributeConsumer.getIndex());

        // Service Name
        List<LocalizedNameType> serviceNames = attributeConsumer.getServiceName();
        for (LocalizedNameType serviceName : serviceNames) {
            writeLocalizedNameType(serviceName, new QName(JBossSAMLURIConstants.METADATA_NSURI.get(), JBossSAMLConstants.SERVICE_NAME.get(),
                    METADATA_PREFIX));
        }

        List<LocalizedNameType> serviceDescriptions = attributeConsumer.getServiceDescription();
        for (LocalizedNameType serviceDescription : serviceDescriptions) {
            writeLocalizedNameType(serviceDescription,
                    new QName(JBossSAMLURIConstants.METADATA_NSURI.get(), JBossSAMLConstants.SERVICE_DESCRIPTION.get(), METADATA_PREFIX));
        }

        List<RequestedAttributeType> requestedAttributes = attributeConsumer.getRequestedAttribute();
        for (RequestedAttributeType requestedAttribute : requestedAttributes) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.REQUESTED_ATTRIBUTE.get(),
                    JBossSAMLURIConstants.METADATA_NSURI.get());
            Boolean isRequired = requestedAttribute.isIsRequired();
            if (isRequired != null) {
                StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.IS_REQUIRED.get()), isRequired.toString());
            }
            writeAttributeTypeWithoutRootTag(requestedAttribute);
            StaxUtil.writeEndElement(writer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeOrganization(OrganizationType org) throws ProcessingException {
        if (org == null)
            throw new ProcessingException(logger.nullArgumentError("Organization"));
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ORGANIZATION.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        ExtensionsType extensions = org.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        // Write the name
        List<LocalizedNameType> nameList = org.getOrganizationName();
        for (LocalizedNameType localName : nameList) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ORGANIZATION_NAME.get(),
                    JBossSAMLURIConstants.METADATA_NSURI.get());

            writeLocalizedType(localName);
        }

        // Write the display name
        List<LocalizedNameType> displayNameList = org.getOrganizationDisplayName();
        for (LocalizedNameType localName : displayNameList) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ORGANIZATION_DISPLAY_NAME.get(),
                    JBossSAMLURIConstants.METADATA_NSURI.get());
            writeLocalizedType(localName);
        }

        // Write the url
        List<LocalizedURIType> uriList = org.getOrganizationURL();
        for (LocalizedURIType uri : uriList) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ORGANIZATION_URL.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

            String lang = uri.getLang();
            String val = uri.getValue().toString();
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLURIConstants.XML.get(), JBossSAMLConstants.LANG.get(), "xml"),
                    lang);

            StaxUtil.writeCharacters(writer, val);

            StaxUtil.writeEndElement(writer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(ContactType contact) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.CONTACT_PERSON.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        ContactTypeType attribs = contact.getContactType();
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.CONTACT_TYPE.get(), attribs.value());

        ExtensionsType extensions = contact.getExtensions();
        if (extensions != null) {
            write(extensions);
        }

        // Write the name
        String company = contact.getCompany();
        if (company != null) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.COMPANY.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
            StaxUtil.writeCharacters(writer, company);
            StaxUtil.writeEndElement(writer);
        }
        String givenName = contact.getGivenName();
        if (givenName != null) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.GIVEN_NAME.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
            StaxUtil.writeCharacters(writer, givenName);
            StaxUtil.writeEndElement(writer);
        }

        String surName = contact.getSurName();
        if (surName != null) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.SURNAME.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
            StaxUtil.writeCharacters(writer, surName);
            StaxUtil.writeEndElement(writer);
        }

        List<String> emailAddresses = contact.getEmailAddress();
        for (String email : emailAddresses) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.EMAIL_ADDRESS.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
            StaxUtil.writeCharacters(writer, email);
            StaxUtil.writeEndElement(writer);
        }

        List<String> tels = contact.getTelephoneNumber();
        for (String telephone : tels) {
            StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.TELEPHONE_NUMBER.get(), JBossSAMLURIConstants.METADATA_NSURI.get());
            StaxUtil.writeCharacters(writer, telephone);
            StaxUtil.writeEndElement(writer);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(ExtensionsType extensions) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.EXTENSIONS__METADATA.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        for (Element extension : extensions.getDomElements())
            StaxUtil.writeDOMElement(writer, extension);

        StaxUtil.writeEndElement(writer);
    }

    public void writeKeyDescriptor(KeyDescriptorType keyDescriptor) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.KEY_DESCRIPTOR.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        KeyTypes keyTypes = keyDescriptor.getUse();
        if (keyTypes != null)
            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.USE.get()), keyTypes.value());

        Element keyInfo = keyDescriptor.getKeyInfo();
        StaxUtil.writeDOMElement(writer, keyInfo);
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeAttributeService(EndpointType endpoint) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.ATTRIBUTE_SERVICE.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        writeEndpointType(endpoint);

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeSingleLogoutService(EndpointType endpoint) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.SINGLE_LOGOUT_SERVICE.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());

        writeEndpointType(endpoint);

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeSingleSignOnService(EndpointType endpoint) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.SINGLE_SIGNON_SERVICE.get(),
                JBossSAMLURIConstants.METADATA_NSURI.get());

        writeEndpointType(endpoint);

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    private void writeProtocolSupportEnumeration(List<String> protoEnum) throws ProcessingException {
        if (protoEnum.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String str : protoEnum) {
                sb.append(str).append(" ");
            }

            StaxUtil.writeAttribute(writer, new QName(JBossSAMLConstants.PROTOCOL_SUPPORT_ENUMERATION.get()), sb.toString()
                    .trim());
        }
    }

    private void writeEndpointType(EndpointType endpoint) throws ProcessingException {
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.BINDING.get(), endpoint.getBinding().toString());
        StaxUtil.writeAttribute(writer, JBossSAMLConstants.LOCATION.get(), endpoint.getLocation().toString());

        URI responseLocation = endpoint.getResponseLocation();
        if (responseLocation != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.RESPONSE_LOCATION.get(), responseLocation.toString());
        }
    }

    private void writeLocalizedType(LocalizedNameType localName) throws ProcessingException {
        String lang = localName.getLang();
        String val = localName.getValue();
        StaxUtil.writeAttribute(writer, new QName(JBossSAMLURIConstants.XML.get(), JBossSAMLConstants.LANG.get(), "xml"), lang);

        StaxUtil.writeCharacters(writer, val);

        StaxUtil.writeEndElement(writer);
    }

    private void writeNameIDFormat(String nameIDFormat) throws ProcessingException {
        StaxUtil.writeStartElement(writer, METADATA_PREFIX, JBossSAMLConstants.NAMEID_FORMAT.get(), JBossSAMLURIConstants.METADATA_NSURI.get());

        if (nameIDFormat != null) {
            StaxUtil.writeCharacters(writer, nameIDFormat);
        }

        StaxUtil.writeEndElement(writer);
    }
}
