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
import org.keycloak.dom.saml.v2.assertion.BaseIDAbstractType;
import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.KeyInfoConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationDataType;
import org.keycloak.dom.saml.v2.assertion.SubjectConfirmationType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.xmlsec.w3.xmldsig.KeyInfoType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.processing.core.saml.v2.util.StaxWriterUtil;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.keycloak.dom.saml.v2.protocol.ExtensionsType;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.ASSERTION_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import org.w3c.dom.Node;

/**
 * Base Class for the Stax writers for SAML
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 2, 2010
 */
public class BaseWriter {

    protected static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    protected static String PROTOCOL_PREFIX = "samlp";

    protected static String ASSERTION_PREFIX = "saml";

    protected XMLStreamWriter writer = null;

    public BaseWriter(XMLStreamWriter writer) {
        this.writer = writer;
    }

    /**
     * Write {@code NameIDType} to stream
     *
     * @param nameIDType
     * @param tag
     * @param out
     *
     * @throws org.keycloak.saml.common.exceptions.ProcessingException
     */
    public void write(NameIDType nameIDType, QName tag) throws ProcessingException {
        StaxUtil.writeStartElement(writer, tag.getPrefix(), tag.getLocalPart(), tag.getNamespaceURI());

        StaxUtil.writeNameSpace(writer, ASSERTION_PREFIX, ASSERTION_NSURI.get());

        URI format = nameIDType.getFormat();
        if (format != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.FORMAT.get(), format.toASCIIString());
        }

        String spProvidedID = nameIDType.getSPProvidedID();
        if (StringUtil.isNotNull(spProvidedID)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.SP_PROVIDED_ID.get(), spProvidedID);
        }

        String spNameQualifier = nameIDType.getSPNameQualifier();
        if (StringUtil.isNotNull(spNameQualifier)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.SP_NAME_QUALIFIER.get(), spNameQualifier);
        }

        String nameQualifier = nameIDType.getNameQualifier();
        if (StringUtil.isNotNull(nameQualifier)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NAME_QUALIFIER.get(), nameQualifier);
        }

        String value = nameIDType.getValue();
        if (StringUtil.isNotNull(value)) {
            StaxUtil.writeCharacters(writer, value);
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Write an {@code AttributeType} to stream
     *
     * @param attributeType
     * @param out
     *
     * @throws ProcessingException
     */
    public void write(AttributeType attributeType) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.ATTRIBUTE.get(), ASSERTION_NSURI.get());

        writeAttributeTypeWithoutRootTag(attributeType);

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void writeAttributeTypeWithoutRootTag(AttributeType attributeType) throws ProcessingException {
        String attributeName = attributeType.getName();
        if (attributeName != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NAME.get(), attributeName);
        }

        String friendlyName = attributeType.getFriendlyName();
        if (StringUtil.isNotNull(friendlyName)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.FRIENDLY_NAME.get(), friendlyName);
        }

        String nameFormat = attributeType.getNameFormat();
        if (StringUtil.isNotNull(nameFormat)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NAME_FORMAT.get(), nameFormat);
        }

        // Take care of other attributes such as x500:encoding
        Map<QName, String> otherAttribs = attributeType.getOtherAttributes();
        if (otherAttribs != null) {
            List<String> nameSpacesDealt = new ArrayList<>();

            Iterator<QName> keySet = otherAttribs.keySet().iterator();
            while (keySet != null && keySet.hasNext()) {
                QName qname = keySet.next();
                String ns = qname.getNamespaceURI();
                if (!nameSpacesDealt.contains(ns)) {
                    StaxUtil.writeNameSpace(writer, qname.getPrefix(), ns);
                    nameSpacesDealt.add(ns);
                }
                String attribValue = otherAttribs.get(qname);
                StaxUtil.writeAttribute(writer, qname, attribValue);
            }
        }

        List<Object> attributeValues = attributeType.getAttributeValue();
        if (attributeValues != null) {
            for (Object attributeValue : attributeValues) {
                if (attributeValue != null) {
                    if (attributeValue instanceof String) {
                        writeStringAttributeValue((String) attributeValue);
                    } else if (attributeValue instanceof NameIDType) {
                    	writeNameIDTypeAttributeValue((NameIDType) attributeValue);
                    } else
                        throw logger.writerUnsupportedAttributeValueError(attributeValue.getClass().getName());
                } else {
                    writeStringAttributeValue(null);
                }
            }
        }
    }

    public void writeNameIDTypeAttributeValue(NameIDType attributeValue) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.ATTRIBUTE_VALUE.get(), ASSERTION_NSURI.get());
    	write((NameIDType)attributeValue, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.NAMEID.get(), ASSERTION_PREFIX));
        StaxUtil.writeEndElement(writer);
    }

    public void writeStringAttributeValue(String attributeValue) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.ATTRIBUTE_VALUE.get(), ASSERTION_NSURI.get());

        StaxUtil.writeNameSpace(writer, JBossSAMLURIConstants.XSI_PREFIX.get(), JBossSAMLURIConstants.XSI_NSURI.get());
        StaxUtil.writeNameSpace(writer, "xs", JBossSAMLURIConstants.XMLSCHEMA_NSURI.get());
        StaxUtil.writeAttribute(writer, "xsi", JBossSAMLURIConstants.XSI_NSURI.get(), "type", "xs:string");

        if (attributeValue == null) {
            StaxUtil.writeAttribute(writer, "xsi", JBossSAMLURIConstants.XSI_NSURI.get(), "nil", "true");
        } else {
            StaxUtil.writeCharacters(writer, attributeValue);
        }

        StaxUtil.writeEndElement(writer);
    }

    public void writeLocalizedNameType(LocalizedNameType localizedNameType, QName startElement) throws ProcessingException {
        StaxUtil.writeStartElement(writer, startElement.getPrefix(), startElement.getLocalPart(),
                startElement.getNamespaceURI());
        StaxUtil.writeAttribute(writer, new QName(JBossSAMLURIConstants.XML.get(), "lang", "xml"), localizedNameType.getLang());
        StaxUtil.writeCharacters(writer, localizedNameType.getValue());
        StaxUtil.writeEndElement(writer);
    }

    /**
     * write an {@code SubjectType} to stream
     *
     * @param subject
     * @param out
     *
     * @throws ProcessingException
     */
    public void write(SubjectType subject) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.SUBJECT.get(), ASSERTION_NSURI.get());

        SubjectType.STSubType subType = subject.getSubType();
        if (subType != null) {
            BaseIDAbstractType baseID = subType.getBaseID();
            if (baseID instanceof NameIDType) {
                NameIDType nameIDType = (NameIDType) baseID;
                write(nameIDType, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.NAMEID.get(), ASSERTION_PREFIX));
            }
            EncryptedElementType enc = subType.getEncryptedID();
            if (enc != null)
                throw new RuntimeException("NYI");
            List<SubjectConfirmationType> confirmations = subType.getConfirmation();
            if (confirmations != null) {
                for (SubjectConfirmationType confirmation : confirmations) {
                    write(confirmation);
                }
            }
        }
        List<SubjectConfirmationType> subjectConfirmations = subject.getConfirmation();
        if (subjectConfirmations != null) {
            for (SubjectConfirmationType subjectConfirmationType : subjectConfirmations) {
                write(subjectConfirmationType);
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    public void write(ExtensionsType extensions) throws ProcessingException {
        if (extensions.getAny().isEmpty()) {
            return;
        }

        StaxUtil.writeStartElement(writer, PROTOCOL_PREFIX, JBossSAMLConstants.EXTENSIONS__PROTOCOL.get(), PROTOCOL_NSURI.get());

        for (Object o : extensions.getAny()) {
            if (o instanceof Node) {
                StaxUtil.writeDOMNode(writer, (Node) o);
            } else if (o instanceof SamlProtocolExtensionsAwareBuilder.NodeGenerator) {
                SamlProtocolExtensionsAwareBuilder.NodeGenerator ng = (SamlProtocolExtensionsAwareBuilder.NodeGenerator) o;
                ng.write(writer);
            } else {
                throw logger.samlExtensionUnknownChild(o == null ? null : o.getClass());
            }
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    private void write(SubjectConfirmationType subjectConfirmationType) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.SUBJECT_CONFIRMATION.get(),
                ASSERTION_NSURI.get());

        StaxUtil.writeAttribute(writer, JBossSAMLConstants.METHOD.get(), subjectConfirmationType.getMethod());

        BaseIDAbstractType baseID = subjectConfirmationType.getBaseID();
        if (baseID != null) {
            write(baseID);
        }
        NameIDType nameIDType = subjectConfirmationType.getNameID();
        if (nameIDType != null) {
            write(nameIDType, new QName(ASSERTION_NSURI.get(), JBossSAMLConstants.NAMEID.get(), ASSERTION_PREFIX));
        }
        SubjectConfirmationDataType subjectConfirmationData = subjectConfirmationType.getSubjectConfirmationData();
        if (subjectConfirmationData != null) {
            write(subjectConfirmationData);
        }
        StaxUtil.writeEndElement(writer);
    }

    private void write(SubjectConfirmationDataType subjectConfirmationData) throws ProcessingException {
        StaxUtil.writeStartElement(writer, ASSERTION_PREFIX, JBossSAMLConstants.SUBJECT_CONFIRMATION_DATA.get(),
                ASSERTION_NSURI.get());

        // Let us look at attributes
        String inResponseTo = subjectConfirmationData.getInResponseTo();
        if (StringUtil.isNotNull(inResponseTo)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.IN_RESPONSE_TO.get(), inResponseTo);
        }

        XMLGregorianCalendar notBefore = subjectConfirmationData.getNotBefore();
        if (notBefore != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NOT_BEFORE.get(), notBefore.toString());
        }

        XMLGregorianCalendar notOnOrAfter = subjectConfirmationData.getNotOnOrAfter();
        if (notOnOrAfter != null) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.NOT_ON_OR_AFTER.get(), notOnOrAfter.toString());
        }

        String recipient = subjectConfirmationData.getRecipient();
        if (StringUtil.isNotNull(recipient)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.RECIPIENT.get(), recipient);
        }

        String address = subjectConfirmationData.getAddress();
        if (StringUtil.isNotNull(address)) {
            StaxUtil.writeAttribute(writer, JBossSAMLConstants.ADDRESS.get(), address);
        }

        if (subjectConfirmationData instanceof KeyInfoConfirmationDataType) {
            KeyInfoConfirmationDataType kicd = (KeyInfoConfirmationDataType) subjectConfirmationData;
            KeyInfoType keyInfo = (KeyInfoType) kicd.getAnyType();
            StaxWriterUtil.writeKeyInfo(writer, keyInfo);
            /*
             * if (keyInfo.getContent() == null || keyInfo.getContent().size() == 0) throw new
             * ProcessingException(ErrorCodes.WRITER_INVALID_KEYINFO_NULL_CONTENT); StaxUtil.writeStartElement(this.writer,
             * WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.KEYINFO, WSTrustConstants.XMLDSig.DSIG_NS);
             * StaxUtil.writeNameSpace(this.writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.DSIG_NS); //
             * write the keyInfo content. Object content = keyInfo.getContent().get(0); if (content instanceof Element) {
             * Element element = (Element) keyInfo.getContent().get(0); StaxUtil.writeDOMNode(this.writer, element); } else if
             * (content instanceof X509DataType) { X509DataType type = (X509DataType) content; if (type.getDataObjects().size()
             * == 0) throw new ProcessingException(ErrorCodes.WRITER_NULL_VALUE + "X509Data");
             * StaxUtil.writeStartElement(this.writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.X509DATA,
             * WSTrustConstants.XMLDSig.DSIG_NS); Object obj = type.getDataObjects().get(0); if (obj instanceof Element) {
             * Element element = (Element) obj; StaxUtil.writeDOMElement(this.writer, element); } else if (obj instanceof
             * X509CertificateType) { X509CertificateType cert = (X509CertificateType) obj;
             * StaxUtil.writeStartElement(this.writer, WSTrustConstants.XMLDSig.DSIG_PREFIX, WSTrustConstants.XMLDSig.X509CERT,
             * WSTrustConstants.XMLDSig.DSIG_NS); StaxUtil.writeCharacters(this.writer, new
             * String(cert.getEncodedCertificate())); StaxUtil.writeEndElement(this.writer); }
             * StaxUtil.writeEndElement(this.writer); } StaxUtil.writeEndElement(this.writer);
             */
        }

        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    private void write(BaseIDAbstractType baseId) throws ProcessingException {
        throw logger.notImplementedYet("Method not implemented.");
    }
}