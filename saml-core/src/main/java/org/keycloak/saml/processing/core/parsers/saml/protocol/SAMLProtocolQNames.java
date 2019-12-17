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
package org.keycloak.saml.processing.core.parsers.saml.protocol;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.xmldsig.XmlDSigQNames;
import javax.xml.namespace.QName;
import org.keycloak.saml.processing.core.parsers.util.HasQName;

/**
 * Elements from saml-schema-protocol-2.0.xsd
 * @author hmlnarik
 */
public enum SAMLProtocolQNames implements HasQName {

    ARTIFACT("Artifact"),
    ARTIFACT_RESOLVE("ArtifactResolve"),
    ARTIFACT_RESPONSE("ArtifactResponse"),
    ASSERTION_ID_REQUEST("AssertionIDRequest"),
    ATTRIBUTE_QUERY("AttributeQuery"),
    AUTHN_QUERY("AuthnQuery"),
    AUTHN_REQUEST("AuthnRequest"),
    AUTHZ_DECISION_QUERY("AuthzDecisionQuery"),
    EXTENSIONS("Extensions"),
    GET_COMPLETE("GetComplete"),
    IDP_ENTRY("IDPEntry"),
    IDP_LIST("IDPList"),
    LOGOUT_REQUEST("LogoutRequest"),
    LOGOUT_RESPONSE("LogoutResponse"),
    MANAGE_NAMEID_REQUEST("ManageNameIDRequest"),
    MANAGE_NAMEID_RESPONSE("ManageNameIDResponse"),
    NAMEID_MAPPING_REQUEST("NameIDMappingRequest"),
    NAMEID_MAPPING_RESPONSE("NameIDMappingResponse"),
    NAMEID_POLICY("NameIDPolicy"),
    NEW_ENCRYPTEDID("NewEncryptedID"),
    NEWID("NewID"),
    REQUESTED_AUTHN_CONTEXT("RequestedAuthnContext"),
    REQUESTERID("RequesterID"),
    RESPONSE("Response"),
    SCOPING("Scoping"),
    SESSION_INDEX("SessionIndex"),
    STATUS_CODE("StatusCode"),
    STATUS_DETAIL("StatusDetail"),
    STATUS_MESSAGE("StatusMessage"),
    STATUS("Status"),
    SUBJECT_QUERY("SubjectQuery"),
    TERMINATE("Terminate"),

    // Attribute names
    ATTR_ALLOW_CREATE(null, "AllowCreate"),
    ATTR_ASSERTION_CONSUMER_SERVICE_URL(null, "AssertionConsumerServiceURL"),
    ATTR_ASSERTION_CONSUMER_SERVICE_INDEX(null, "AssertionConsumerServiceIndex"),
    ATTR_ATTRIBUTE_CONSUMING_SERVICE_INDEX(null, "AttributeConsumingServiceIndex"),
    ATTR_COMPARISON(null, "Comparison"),
    ATTR_CONSENT(null, "Consent"),
    ATTR_DESTINATION(null, "Destination"),
    ATTR_FORCE_AUTHN(null, "ForceAuthn"),
    ATTR_FORMAT(null, "Format"),
    ATTR_ID(null, "ID"),
    ATTR_IN_RESPONSE_TO(null, "InResponseTo"),
    ATTR_IS_PASSIVE(null, "IsPassive"),
    ATTR_ISSUE_INSTANT(null, "IssueInstant"),
    ATTR_NOT_BEFORE(null, "NotBefore"),
    ATTR_NOT_ON_OR_AFTER(null, "NotOnOrAfter"),
    ATTR_PROTOCOL_BINDING(null, "ProtocolBinding"),
    ATTR_PROVIDER_NAME(null, "ProviderName"),
    ATTR_REASON(null, "Reason"),
    ATTR_VALUE(null, "Value"),
    ATTR_VERSION(null, "Version"),

    // Elements from other namespaces that can be direct subelements of this namespace's elements
    ATTRIBUTE(SAMLAssertionQNames.ATTRIBUTE),
    ASSERTION(SAMLAssertionQNames.ASSERTION),
    AUTHN_CONTEXT_CLASS_REF(SAMLAssertionQNames.AUTHN_CONTEXT_CLASS_REF),
    AUTHN_CONTEXT_DECL_REF(SAMLAssertionQNames.AUTHN_CONTEXT_DECL_REF),
    BASEID(SAMLAssertionQNames.BASEID),
    CONDITIONS(SAMLAssertionQNames.CONDITIONS),
    ENCRYPTED_ASSERTION(SAMLAssertionQNames.ENCRYPTED_ASSERTION),
    ISSUER(SAMLAssertionQNames.ISSUER),
    NAMEID(SAMLAssertionQNames.NAMEID),
    SIGNATURE(XmlDSigQNames.SIGNATURE),
    ENCRYPTED_ID(SAMLAssertionQNames.ENCRYPTED_ID),
    SUBJECT(SAMLAssertionQNames.SUBJECT),

    UNKNOWN_ELEMENT("")
    ;

    private final QName qName;

    SAMLProtocolQNames(String localName) {
        this(JBossSAMLURIConstants.PROTOCOL_NSURI, localName);
    }

    SAMLProtocolQNames(HasQName source) {
        this.qName = source.getQName();
    }

    SAMLProtocolQNames(JBossSAMLURIConstants nsUri, String localName) {
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
