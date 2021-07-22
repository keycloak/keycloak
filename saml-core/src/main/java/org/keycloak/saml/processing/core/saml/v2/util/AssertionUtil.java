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
package org.keycloak.saml.processing.core.saml.v2.util;

import org.keycloak.dom.saml.v1.assertion.SAML11AssertionType;
import org.keycloak.dom.saml.v1.assertion.SAML11AttributeStatementType;
import org.keycloak.dom.saml.v1.assertion.SAML11AttributeType;
import org.keycloak.dom.saml.v1.assertion.SAML11ConditionsType;
import org.keycloak.dom.saml.v1.assertion.SAML11StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.EncryptedAssertionType;
import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.assertion.SubjectType.STSubType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.rotation.HardcodedKeyLocator;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.exceptions.fed.IssueInstantMissingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLAssertionWriter;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility to deal with assertions
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 3, 2009
 */
public class AssertionUtil {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    /**
     * Given {@code AssertionType}, convert it into a String
     *
     * @param assertion
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static String asString(AssertionType assertion) throws ProcessingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SAMLAssertionWriter writer = new SAMLAssertionWriter(StaxUtil.getXMLStreamWriter(baos));
        writer.write(assertion);
        return new String(baos.toByteArray(), GeneralConstants.SAML_CHARSET);
    }

    /**
     * Given {@code AssertionType}, convert it into a DOM Document.
     *
     * @param assertion
     *
     * @return
     *
     * @throws ProcessingException
     */
    public static Document asDocument(AssertionType assertion) throws ProcessingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SAMLAssertionWriter writer = new SAMLAssertionWriter(StaxUtil.getXMLStreamWriter(baos));

        writer.write(assertion);

        try {
            return DocumentUtil.getDocument(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            throw logger.processingError(e);
        }
    }

    /**
     * Create an assertion
     *
     * @param id
     * @param issuer
     *
     * @return
     */
    public static SAML11AssertionType createSAML11Assertion(String id, XMLGregorianCalendar issueInstant, String issuer) {
        SAML11AssertionType assertion = new SAML11AssertionType(id, issueInstant);
        assertion.setIssuer(issuer);
        return assertion;
    }

    /**
     * Create an assertion
     *
     * @param id
     * @param issuer
     *
     * @return
     */
    public static AssertionType createAssertion(String id, NameIDType issuer) {
        XMLGregorianCalendar issueInstant = XMLTimeUtil.getIssueInstant();
        AssertionType assertion = new AssertionType(id, issueInstant);
        assertion.setIssuer(issuer);
        return assertion;
    }

    /**
     * Given a user name, create a {@code SubjectType} that can then be inserted into an assertion
     *
     * @param userName
     *
     * @return
     */
    public static SubjectType createAssertionSubject(String userName) {
        SubjectType assertionSubject = new SubjectType();
        STSubType subType = new STSubType();
        NameIDType anil = new NameIDType();
        anil.setValue(userName);
        subType.addBaseID(anil);
        assertionSubject.setSubType(subType);
        return assertionSubject;
    }

    /**
     * Create an attribute type
     *
     * @param name Name of the attribute
     * @param nameFormat name format uri
     * @param attributeValues an object array of attribute values
     *
     * @return
     */
    public static AttributeType createAttribute(String name, String nameFormat, Object... attributeValues) {
        AttributeType att = new AttributeType(name);
        att.setNameFormat(nameFormat);
        if (attributeValues != null && attributeValues.length > 0) {
            for (Object attributeValue : attributeValues) {
                att.addAttributeValue(attributeValue);
            }
        }

        return att;
    }

    /**
     * <p>
     * Add validity conditions to the SAML2 Assertion
     * </p>
     * <p>
     * There is no clock skew added.
     *
     * @param assertion
     * @param durationInMilis
     *
     * @throws ConfigurationException
     * @throws IssueInstantMissingException
     * @see {{@link #createTimedConditions(AssertionType, long, long)}
     *      </p>
     */
    public static void createTimedConditions(AssertionType assertion, long durationInMilis) throws ConfigurationException,
            IssueInstantMissingException {
        XMLGregorianCalendar issueInstant = assertion.getIssueInstant();
        if (issueInstant == null)
            throw new IssueInstantMissingException(ErrorCodes.NULL_ISSUE_INSTANT);
        XMLGregorianCalendar assertionValidityLength = XMLTimeUtil.add(issueInstant, durationInMilis);
        ConditionsType conditionsType = new ConditionsType();
        conditionsType.setNotBefore(issueInstant);
        conditionsType.setNotOnOrAfter(assertionValidityLength);

        assertion.setConditions(conditionsType);
    }

    /**
     * Add validity conditions to the SAML2 Assertion
     *
     * @param assertion
     * @param durationInMilis
     *
     * @throws ConfigurationException
     * @throws IssueInstantMissingException
     */
    public static void createTimedConditions(AssertionType assertion, long durationInMilis, long clockSkew)
            throws ConfigurationException, IssueInstantMissingException {
        XMLGregorianCalendar issueInstant = assertion.getIssueInstant();
        if (issueInstant == null)
            throw logger.samlIssueInstantMissingError();
        XMLGregorianCalendar assertionValidityLength = XMLTimeUtil.add(issueInstant, durationInMilis + clockSkew);

        ConditionsType conditionsType = new ConditionsType();

        XMLGregorianCalendar beforeInstant = XMLTimeUtil.subtract(issueInstant, clockSkew);

        conditionsType.setNotBefore(beforeInstant);
        conditionsType.setNotOnOrAfter(assertionValidityLength);

        assertion.setConditions(conditionsType);
    }

    /**
     * Add validity conditions to the SAML2 Assertion
     *
     * @param assertion
     * @param durationInMilis
     *
     * @throws ConfigurationException
     * @throws IssueInstantMissingException
     */
    public static void createSAML11TimedConditions(SAML11AssertionType assertion, long durationInMilis, long clockSkew)
            throws ConfigurationException, IssueInstantMissingException {
        XMLGregorianCalendar issueInstant = assertion.getIssueInstant();
        if (issueInstant == null)
            throw new IssueInstantMissingException(ErrorCodes.NULL_ISSUE_INSTANT);
        XMLGregorianCalendar assertionValidityLength = XMLTimeUtil.add(issueInstant, durationInMilis + clockSkew);

        SAML11ConditionsType conditionsType = new SAML11ConditionsType();

        XMLGregorianCalendar beforeInstant = XMLTimeUtil.subtract(issueInstant, clockSkew);

        conditionsType.setNotBefore(beforeInstant);
        conditionsType.setNotOnOrAfter(assertionValidityLength);
        assertion.setConditions(conditionsType);
    }

    /**
     * Given an {@linkplain Element}, validate the Signature direct child element
     *
     * @param element parent {@linkplain Element}
     * @param publicKey the {@link PublicKey}
     *
     * @return true if signature is present and valid
     */
    public static boolean isSignatureValid(Element element, PublicKey publicKey) {
        return isSignatureValid(element, new HardcodedKeyLocator(publicKey));
    }

    /**
     * Given an {@linkplain Element}, validate the Signature direct child element
     *
     * @param element parent {@linkplain Element}
     * @param keyLocator the {@link KeyLocator}
     *
     * @return true if signature is present and valid
     */

    public static boolean isSignatureValid(Element element, KeyLocator keyLocator) {
        try {
            SAML2Signature.configureIdAttribute(element);

            Element signature = getSignature(element);
            if(signature != null) {
                return XMLSignatureUtil.validateSingleNode(signature, keyLocator);
            }
        } catch (Exception e) {
            logger.signatureAssertionValidationError(e);
        }
        return false;
    }

    /**
     *
     * Given an {@linkplain Element}, check if there is a Signature direct child element
     *
     * @param element parent {@linkplain Element}
     * @return true if signature is present
     */

    public static boolean isSignedElement(Element element) {
        return getSignature(element) != null;
    }

    protected static Element getSignature(Element element) {
        return DocumentUtil.getDirectChildElement(element, XMLSignature.XMLNS, "Signature");
    }

    /**
     * Check whether the assertion has expired.
     * Processing rules defined in Section 2.5.1.2 of saml-core-2.0-os.pdf.
     *
     * @param assertion
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static boolean hasExpired(AssertionType assertion) throws ConfigurationException {
        boolean expiry = false;

        // Check for validity of assertion
        ConditionsType conditionsType = assertion.getConditions();
        if (conditionsType != null) {
            XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
            XMLGregorianCalendar notBefore = conditionsType.getNotBefore();
            XMLGregorianCalendar notOnOrAfter = conditionsType.getNotOnOrAfter();

            if (notBefore != null) {
                logger.trace("Assertion: " + assertion.getID() + " ::Now=" + now.toXMLFormat() + " ::notBefore=" + notBefore.toXMLFormat());
            }

            if (notOnOrAfter != null) {
                logger.trace("Assertion: " + assertion.getID() + " ::Now=" + now.toXMLFormat() + " ::notOnOrAfter=" + notOnOrAfter);
            }

            expiry = !XMLTimeUtil.isValid(now, notBefore, notOnOrAfter);

            if (expiry) {
                logger.samlAssertionExpired(assertion.getID());
            }
        }

        // TODO: if conditions do not exist, assume the assertion to be everlasting?
        return expiry;
    }

    /**
     * Verify whether the assertion has expired. You can add in a clock skew to adapt to conditions where in the IDP and
     * SP are
     * out of sync.
     *
     * @param assertion
     * @param clockSkewInMilis in miliseconds
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static boolean hasExpired(AssertionType assertion, long clockSkewInMilis) throws ConfigurationException {
        boolean expiry = false;

        // Check for validity of assertion
        ConditionsType conditionsType = assertion.getConditions();
        if (conditionsType != null) {
            XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
            XMLGregorianCalendar notBefore = conditionsType.getNotBefore();
            XMLGregorianCalendar updatedNotBefore = XMLTimeUtil.subtract(notBefore, clockSkewInMilis);
            XMLGregorianCalendar notOnOrAfter = conditionsType.getNotOnOrAfter();
            XMLGregorianCalendar updatedOnOrAfter = XMLTimeUtil.add(notOnOrAfter, clockSkewInMilis);

            logger.trace("Now=" + now.toXMLFormat() + " ::notBefore=" + notBefore.toXMLFormat() + " ::notOnOrAfter=" + notOnOrAfter);
            expiry = !XMLTimeUtil.isValid(now, updatedNotBefore, updatedOnOrAfter);
            if (expiry) {
                logger.samlAssertionExpired(assertion.getID());
            }
        }

        // TODO: if conditions do not exist, assume the assertion to be everlasting?
        return expiry;
    }

    /**
     * Check whether the assertion has expired
     *
     * @param assertion
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static boolean hasExpired(SAML11AssertionType assertion) throws ConfigurationException {
        boolean expiry = false;

        // Check for validity of assertion
        SAML11ConditionsType conditionsType = assertion.getConditions();
        if (conditionsType != null) {
            XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
            XMLGregorianCalendar notBefore = conditionsType.getNotBefore();
            XMLGregorianCalendar notOnOrAfter = conditionsType.getNotOnOrAfter();

            logger.trace("Now=" + now.toXMLFormat() + " ::notBefore=" + notBefore.toXMLFormat() + " ::notOnOrAfter=" + notOnOrAfter);

            expiry = !XMLTimeUtil.isValid(now, notBefore, notOnOrAfter);
            if (expiry) {
                logger.samlAssertionExpired(assertion.getID());
            }
        }

        // TODO: if conditions do not exist, assume the assertion to be everlasting?
        return expiry;
    }

    /**
     * Verify whether the assertion has expired. You can add in a clock skew to adapt to conditions where in the IDP and
     * SP are
     * out of sync.
     *
     * @param assertion
     * @param clockSkewInMilis in miliseconds
     *
     * @return
     *
     * @throws ConfigurationException
     */
    public static boolean hasExpired(SAML11AssertionType assertion, long clockSkewInMilis) throws ConfigurationException {
        boolean expiry = false;

        // Check for validity of assertion
        SAML11ConditionsType conditionsType = assertion.getConditions();
        if (conditionsType != null) {
            XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();
            XMLGregorianCalendar notBefore = conditionsType.getNotBefore();
            XMLGregorianCalendar updatedNotBefore = XMLTimeUtil.subtract(notBefore, clockSkewInMilis);
            XMLGregorianCalendar notOnOrAfter = conditionsType.getNotOnOrAfter();
            XMLGregorianCalendar updatedOnOrAfter = XMLTimeUtil.add(notOnOrAfter, clockSkewInMilis);

            logger.trace("Now=" + now.toXMLFormat() + " ::notBefore=" + notBefore.toXMLFormat() + " ::notOnOrAfter=" + notOnOrAfter);

            expiry = !XMLTimeUtil.isValid(now, updatedNotBefore, updatedOnOrAfter);
            if (expiry) {
                logger.samlAssertionExpired(assertion.getID());
            }
        }

        // TODO: if conditions do not exist, assume the assertion to be everlasting?
        return expiry;
    }

    /**
     * Extract the expiration time from an {@link AssertionType}
     *
     * @param assertion
     *
     * @return
     */
    public static XMLGregorianCalendar getExpiration(AssertionType assertion) {
        XMLGregorianCalendar expiry = null;

        ConditionsType conditionsType = assertion.getConditions();
        if (conditionsType != null) {
            expiry = conditionsType.getNotOnOrAfter();
        }
        return expiry;
    }

    /**
     * Given an assertion, return the list of roles it may have
     *
     * @param assertion The {@link AssertionType}
     * @param roleKeys a list of string values representing the role keys. The list can be null.
     *
     * @return
     */
    public static List<String> getRoles(AssertionType assertion, List<String> roleKeys) {
        List<String> roles = new ArrayList<>();
        Set<StatementAbstractType> statements = assertion.getStatements();
        for (StatementAbstractType statement : statements) {
            if (statement instanceof AttributeStatementType) {
                AttributeStatementType attributeStatement = (AttributeStatementType) statement;
                List<ASTChoiceType> attList = attributeStatement.getAttributes();
                for (ASTChoiceType obj : attList) {
                    AttributeType attr = obj.getAttribute();
                    if (roleKeys != null && roleKeys.size() > 0) {
                        if (!roleKeys.contains(attr.getName()))
                            continue;
                    }
                    List<Object> attributeValues = attr.getAttributeValue();
                    if (attributeValues != null) {
                        for (Object attrValue : attributeValues) {
                            if (attrValue instanceof String) {
                                roles.add((String) attrValue);
                            } else if (attrValue instanceof Node) {
                                Node roleNode = (Node) attrValue;
                                roles.add(roleNode.getFirstChild().getNodeValue());
                            } else
                                throw logger.unknownObjectType(attrValue);
                        }
                    }
                }
            }
        }
        return roles;
    }

    /**
     * Given an assertion, return the list of roles it may have
     *
     * @param assertion The {@link SAML11AssertionType}
     * @param roleKeys a list of string values representing the role keys. The list can be null.
     *
     * @return
     */
    public static List<String> getRoles(SAML11AssertionType assertion, List<String> roleKeys) {
        List<String> roles = new ArrayList<>();
        List<SAML11StatementAbstractType> statements = assertion.getStatements();
        for (SAML11StatementAbstractType statement : statements) {
            if (statement instanceof SAML11AttributeStatementType) {
                SAML11AttributeStatementType attributeStatement = (SAML11AttributeStatementType) statement;
                List<SAML11AttributeType> attributes = attributeStatement.get();
                for (SAML11AttributeType attr : attributes) {
                    if (roleKeys != null && roleKeys.size() > 0) {
                        if (!roleKeys.contains(attr.getAttributeName()))
                            continue;
                    }
                    List<Object> attributeValues = attr.get();
                    if (attributeValues != null) {
                        for (Object attrValue : attributeValues) {
                            if (attrValue instanceof String) {
                                roles.add((String) attrValue);
                            } else if (attrValue instanceof Node) {
                                Node roleNode = (Node) attrValue;
                                roles.add(roleNode.getFirstChild().getNodeValue());
                            } else
                                throw logger.unknownObjectType(attrValue);
                        }
                    }
                }
            }
        }
        return roles;
    }

    public static AssertionType getAssertion(SAMLDocumentHolder holder, ResponseType responseType, PrivateKey privateKey) throws ParsingException, ProcessingException, ConfigurationException {
        List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();

        if (assertions.isEmpty()) {
            throw new ProcessingException("No assertion from response.");
        }

        ResponseType.RTChoiceType rtChoiceType = assertions.get(0);
        EncryptedAssertionType encryptedAssertion = rtChoiceType.getEncryptedAssertion();

        if (encryptedAssertion != null) {
            if (privateKey == null) {
                throw new ProcessingException("Encryptd assertion and decrypt private key is null");
            }
            decryptAssertion(holder, responseType, privateKey);

        }
        return responseType.getAssertions().get(0).getAssertion();
    }

    public static boolean isAssertionEncrypted(ResponseType responseType) throws ProcessingException {
        List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();

        if (assertions.isEmpty()) {
            throw new ProcessingException("No assertion from response.");
        }

        ResponseType.RTChoiceType rtChoiceType = assertions.get(0);
        return rtChoiceType.getEncryptedAssertion() != null;
    }

    /**
     * This method modifies the given responseType, and replaces the encrypted assertion with a decrypted version.
     * @param responseType a response containg an encrypted assertion
     * @return the assertion element as it was decrypted. This can be used in signature verification.
     */
    public static Element decryptAssertion(SAMLDocumentHolder holder, ResponseType responseType, PrivateKey privateKey) throws ParsingException, ProcessingException, ConfigurationException {
        Document doc = holder.getSamlDocument();
        Element enc = DocumentUtil.getElement(doc, new QName(JBossSAMLConstants.ENCRYPTED_ASSERTION.get()));

        if (enc == null) {
            throw new ProcessingException("No encrypted assertion found.");
        }

        String oldID = enc.getAttribute(JBossSAMLConstants.ID.get());
        Document newDoc = DocumentUtil.createDocument();
        Node importedNode = newDoc.importNode(enc, true);
        newDoc.appendChild(importedNode);

        Element decryptedDocumentElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, privateKey);
        SAMLParser parser = SAMLParser.getInstance();

        JAXPValidationUtil.checkSchemaValidation(decryptedDocumentElement);
        AssertionType assertion = (AssertionType) parser.parse(parser.createEventReader(DocumentUtil
                .getNodeAsStream(decryptedDocumentElement)));

        responseType.replaceAssertion(oldID, new ResponseType.RTChoiceType(assertion));

        return decryptedDocumentElement;
    }

    public static boolean isIdEncrypted(final ResponseType responseType) {
        final STSubType subTypeElement = getSubTypeElement(responseType);
        return subTypeElement != null && subTypeElement.getEncryptedID() != null;
    }

    public static void decryptId(final ResponseType responseType, final PrivateKey privateKey) throws ConfigurationException, ProcessingException, ParsingException {
        final STSubType subTypeElement = getSubTypeElement(responseType);
        if(subTypeElement == null) {
            return;
        }
        final EncryptedElementType encryptedID = subTypeElement.getEncryptedID();
        if (encryptedID == null) {
            return;
        }
        Element encryptedElement = encryptedID.getEncryptedElement();
        Document newDoc = DocumentUtil.createDocument();
        Node importedNode = newDoc.importNode(encryptedElement, true);
        newDoc.appendChild(importedNode);
        Element decryptedNameIdElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, privateKey);

        final XMLEventReader xmlEventReader = StaxParserUtil.getXMLEventReader(DocumentUtil.getNodeAsStream(decryptedNameIdElement));
        NameIDType nameIDType = SAMLParserUtil.parseNameIDType(xmlEventReader);

        // Add unencrypted id, remove encrypted
        subTypeElement.addBaseID(nameIDType);
        subTypeElement.setEncryptedID(null);
    }

    private static STSubType getSubTypeElement(final ResponseType responseType) {
        final List<ResponseType.RTChoiceType> assertions = responseType.getAssertions();
        if (assertions.isEmpty()) {
            return null;
        }
        final AssertionType assertion = assertions.get(0).getAssertion();
        if (assertion.getSubject() == null) {
            return null;
        }
        return assertion.getSubject().getSubType();
    }
}