/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.parsers.saml.xmldsig.XmlDSigQNames;
import org.keycloak.saml.processing.core.parsers.util.HasQName;
import javax.xml.namespace.QName;

/**
 * Elements and attribute names from saml-schema-assertion-2.0.xsd
 * @author hmlnarik
 */
public enum SAMLAssertionQNames implements HasQName {

    ACTION("Action"),
    ADVICE("Advice"),
    ASSERTION("Assertion"),
    ASSERTION_ID_REF("AssertionIDRef"),
    ASSERTION_URI_REF("AssertionURIRef"),
    ATTRIBUTE("Attribute"),
    ATTRIBUTE_STATEMENT("AttributeStatement"),
    ATTRIBUTE_VALUE("AttributeValue"),
    AUDIENCE("Audience"),
    AUDIENCE_RESTRICTION("AudienceRestriction"),
    AUTHENTICATING_AUTHORITY("AuthenticatingAuthority"),
    AUTHN_CONTEXT("AuthnContext"),
    AUTHN_CONTEXT_CLASS_REF("AuthnContextClassRef"),
    AUTHN_CONTEXT_DECL("AuthnContextDecl"),
    AUTHN_CONTEXT_DECL_REF("AuthnContextDeclRef"),
    AUTHN_STATEMENT("AuthnStatement"),
    AUTHZ_DECISION_STATEMENT("AuthzDecisionStatement"),
    BASEID("BaseID"),
    CONDITION("Condition"),
    CONDITIONS("Conditions"),
    ENCRYPTED_ASSERTION("EncryptedAssertion"),
    ENCRYPTED_ATTRIBUTE("EncryptedAttribute"),
    ENCRYPTED_ID("EncryptedID"),
    EVIDENCE("Evidence"),
    ISSUER("Issuer"),
    NAMEID("NameID"),
    ONE_TIME_USE("OneTimeUse"),
    PROXY_RESTRICTION("ProxyRestriction"),
    STATEMENT("Statement"),
    SUBJECT_CONFIRMATION_DATA("SubjectConfirmationData"),
    SUBJECT_CONFIRMATION("SubjectConfirmation"),
    SUBJECT_LOCALITY("SubjectLocality"),
    SUBJECT("Subject"),

    // Attribute names
    ATTR_ADDRESS(null, "Address"),
    ATTR_AUTHN_INSTANT(null, "AuthnInstant"),
    ATTR_COUNT(null, "Count"),
    ATTR_DNS_NAME(null, "DNSName"),
    ATTR_FORMAT(null, "Format"),
    ATTR_FRIENDLY_NAME(null, "FriendlyName"),
    ATTR_ID(null, "ID"),
    ATTR_IN_RESPONSE_TO(null, "InResponseTo"),
    ATTR_ISSUE_INSTANT(null, "IssueInstant"),
    ATTR_METHOD(null, "Method"),
    ATTR_NAME(null, "Name"),
    ATTR_NAME_FORMAT(null, "NameFormat"),
    ATTR_NAME_QUALIFIER(null, "NameQualifier"),
    ATTR_NOT_BEFORE(null, "NotBefore"),
    ATTR_NOT_ON_OR_AFTER(null, "NotOnOrAfter"),
    ATTR_RECIPIENT(null, "Recipient"),
    ATTR_SESSION_INDEX(null, "SessionIndex"),
    ATTR_SESSION_NOT_ON_OR_AFTER(null, "SessionNotOnOrAfter"),
    ATTR_SP_PROVIDED_ID(null, "SPProvidedID"),
    ATTR_SP_NAME_QUALIFIER(null, "SPNameQualifier"),
    ATTR_VERSION(null, "Version"),

    // Elements from other namespaces that can be direct subelements of this namespace's elements
    KEY_INFO(XmlDSigQNames.KEY_INFO),
    SIGNATURE(XmlDSigQNames.SIGNATURE),

    ATTR_X500_ENCODING(JBossSAMLURIConstants.X500_NSURI, "Encoding"),

    UNKNOWN_ELEMENT("")
    ;

    private final QName qName;

    SAMLAssertionQNames(String localName) {
        this(JBossSAMLURIConstants.ASSERTION_NSURI, localName);
    }

    SAMLAssertionQNames(HasQName source) {
        this.qName = source.getQName();
    }

    SAMLAssertionQNames(JBossSAMLURIConstants nsUri, String localName) {
        this.qName = new QName(nsUri == null ? null : nsUri.get(), localName);
    }

    @Override
    public QName getQName() {
        return qName;
    }

    public QName getQName(String prefix) {
        return new QName(this.qName.getNamespaceURI(), this.qName.getLocalPart(), prefix);
    }
}
