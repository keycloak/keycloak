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
package org.keycloak.saml.common.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.*;

/**
 * SAML Constants
 *
 * @since Dec 10, 2008
 */
public enum JBossSAMLConstants {
    // saml-schema-protocol-2.0.xsd
    ARTIFACT(PROTOCOL_NSURI, "Artifact"),
    ARTIFACT_RESOLVE(PROTOCOL_NSURI, "ArtifactResolve"),
    ARTIFACT_RESPONSE(PROTOCOL_NSURI, "ArtifactResponse"),
    ASSERTION_ID_REQUEST(PROTOCOL_NSURI, "AssertionIDRequest"),
    ATTRIBUTE_QUERY(PROTOCOL_NSURI, "AttributeQuery"),
    AUTHN_QUERY(PROTOCOL_NSURI, "AuthnQuery"),
    AUTHN_REQUEST(PROTOCOL_NSURI, "AuthnRequest"),
    AUTHZ_DECISION_QUERY(PROTOCOL_NSURI, "AuthzDecisionQuery"),
    EXTENSIONS__PROTOCOL(PROTOCOL_NSURI, "Extensions"),
    GET_COMPLETE(PROTOCOL_NSURI, "GetComplete"),
    IDP_ENTRY(PROTOCOL_NSURI, "IDPEntry"),
    IDP_LIST(PROTOCOL_NSURI, "IDPList"),
    LOGOUT_REQUEST(PROTOCOL_NSURI, "LogoutRequest"),
    LOGOUT_RESPONSE(PROTOCOL_NSURI, "LogoutResponse"),
    MANAGE_NAMEID_REQUEST(PROTOCOL_NSURI, "ManageNameIDRequest"),
    MANAGE_NAMEID_RESPONSE(PROTOCOL_NSURI, "ManageNameIDResponse"),
    NAMEID_MAPPING_REQUEST(PROTOCOL_NSURI, "NameIDMappingRequest"),
    NAMEID_MAPPING_RESPONSE(PROTOCOL_NSURI, "NameIDMappingResponse"),
    NAMEID_POLICY(PROTOCOL_NSURI, "NameIDPolicy"),
    NEW_ENCRYPTEDID(PROTOCOL_NSURI, "NewEncryptedID"),
    NEWID(PROTOCOL_NSURI, "NewID"),
    REQUESTED_AUTHN_CONTEXT(PROTOCOL_NSURI, "RequestedAuthnContext"),
    REQUESTERID(PROTOCOL_NSURI, "RequesterID"),
    RESPONSE__PROTOCOL(PROTOCOL_NSURI, "Response"),
    SCOPING(PROTOCOL_NSURI, "Scoping"),
    SESSION_INDEX(PROTOCOL_NSURI, "SessionIndex"),
    STATUS_CODE(PROTOCOL_NSURI, "StatusCode"),
    STATUS_DETAIL(PROTOCOL_NSURI, "StatusDetail"),
    STATUS_MESSAGE(PROTOCOL_NSURI, "StatusMessage"),
    STATUS(PROTOCOL_NSURI, "Status"),
    SUBJECT_QUERY(PROTOCOL_NSURI, "SubjectQuery"),
    TERMINATE(PROTOCOL_NSURI, "Terminate"),

    // saml-schema-assertion-2.0.xsd
    ACTION(ASSERTION_NSURI, "Action"),
    ADVICE(ASSERTION_NSURI, "Advice"),
    ASSERTION(ASSERTION_NSURI, "Assertion"),
    ASSERTION_ID_REF(ASSERTION_NSURI, "AssertionIDRef"),
    ASSERTION_URI_REF(ASSERTION_NSURI, "AssertionURIRef"),
    ATTRIBUTE(ASSERTION_NSURI, "Attribute"),
    ATTRIBUTE_STATEMENT(ASSERTION_NSURI, "AttributeStatement"),
    ATTRIBUTE_VALUE(ASSERTION_NSURI, "AttributeValue"),
    AUDIENCE(ASSERTION_NSURI, "Audience"),
    AUDIENCE_RESTRICTION(ASSERTION_NSURI, "AudienceRestriction"),
    AUTHENTICATING_AUTHORITY(ASSERTION_NSURI, "AuthenticatingAuthority"),
    AUTHN_CONTEXT(ASSERTION_NSURI, "AuthnContext"),
    AUTHN_CONTEXT_CLASS_REF(ASSERTION_NSURI, "AuthnContextClassRef"),
    AUTHN_CONTEXT_DECL(ASSERTION_NSURI, "AuthnContextDecl"),
    AUTHN_CONTEXT_DECL_REF(ASSERTION_NSURI, "AuthnContextDeclRef"),
    AUTHN_STATEMENT(ASSERTION_NSURI, "AuthnStatement"),
    AUTHZ_DECISION_STATEMENT(ASSERTION_NSURI, "AuthzDecisionStatement"),
    BASEID(ASSERTION_NSURI, "BaseID"),
    CONDITION(ASSERTION_NSURI, "Condition"),
    CONDITIONS(ASSERTION_NSURI, "Conditions"),
    ENCRYPTED_ASSERTION(ASSERTION_NSURI, "EncryptedAssertion"),
    ENCRYPTED_ATTRIBUTE(ASSERTION_NSURI, "EncryptedAttribute"),
    ENCRYPTED_ID(ASSERTION_NSURI, "EncryptedID"),
    EVIDENCE(ASSERTION_NSURI, "Evidence"),
    ISSUER(ASSERTION_NSURI, "Issuer"),
    NAMEID(ASSERTION_NSURI, "NameID"),
    ONE_TIME_USE(ASSERTION_NSURI, "OneTimeUse"),
    PROXY_RESTRICTION(ASSERTION_NSURI, "ProxyRestriction"),
    STATEMENT(ASSERTION_NSURI, "Statement"),
    SUBJECT_CONFIRMATION_DATA(ASSERTION_NSURI, "SubjectConfirmationData"),
    SUBJECT_CONFIRMATION(ASSERTION_NSURI, "SubjectConfirmation"),
    SUBJECT_LOCALITY(ASSERTION_NSURI, "SubjectLocality"),
    SUBJECT(ASSERTION_NSURI, "Subject"),

    // saml-schema-metadata-2.0.xsd
    ADDITIONAL_METADATA_LOCATION(METADATA_NSURI, "AdditionalMetadataLocation"),
    AFFILIATE_MEMBER(METADATA_NSURI, "AffiliateMember"),
    AFFILIATION_DESCRIPTOR(METADATA_NSURI, "AffiliationDescriptor"),
    ARTIFACT_RESOLUTION_SERVICE(METADATA_NSURI, "ArtifactResolutionService"),
    ASSERTION_CONSUMER_SERVICE(METADATA_NSURI, "AssertionConsumerService"),
    ASSERTION_ID_REQUEST_SERVICE(METADATA_NSURI, "AssertionIDRequestService"),
    ATTRIBUTE_AUTHORITY_DESCRIPTOR(METADATA_NSURI, "AttributeAuthorityDescriptor"),
    ATTRIBUTE_CONSUMING_SERVICE(METADATA_NSURI, "AttributeConsumingService"),
    ATTRIBUTE_PROFILE(METADATA_NSURI, "AttributeProfile"),
    ATTRIBUTE_SERVICE(METADATA_NSURI, "AttributeService"),
    AUTHN_AUTHORITY_DESCRIPTOR(METADATA_NSURI, "AuthnAuthorityDescriptor"),
    AUTHN_QUERY_SERVICE(METADATA_NSURI, "AuthnQueryService"),
    AUTHZ_SERVICE(METADATA_NSURI, "AuthzService"),
    COMPANY(METADATA_NSURI, "Company"),
    CONTACT_PERSON(METADATA_NSURI, "ContactPerson"),
    EMAIL_ADDRESS(METADATA_NSURI, "EmailAddress"),
    ENCRYPTION_METHOD(METADATA_NSURI, "EncryptionMethod"),
    ENTITIES_DESCRIPTOR(METADATA_NSURI, "EntitiesDescriptor"),
    ENTITY_DESCRIPTOR(METADATA_NSURI, "EntityDescriptor"),
    EXTENSIONS__METADATA(METADATA_NSURI, "Extensions"),
    GIVEN_NAME(METADATA_NSURI, "GivenName"),
    IDP_SSO_DESCRIPTOR(METADATA_NSURI, "IDPSSODescriptor"),
    KEY_DESCRIPTOR(METADATA_NSURI, "KeyDescriptor"),
    MANAGE_NAMEID_SERVICE(METADATA_NSURI, "ManageNameIDService"),
    NAMEID_FORMAT(METADATA_NSURI, "NameIDFormat"),
    NAMEID_MAPPING_SERVICE(METADATA_NSURI, "NameIDMappingService"),
    ORGANIZATION_DISPLAY_NAME(METADATA_NSURI, "OrganizationDisplayName"),
    ORGANIZATION_NAME(METADATA_NSURI, "OrganizationName"),
    ORGANIZATION(METADATA_NSURI, "Organization"),
    ORGANIZATION_URL(METADATA_NSURI, "OrganizationURL"),
    ORGANIZATION_URL_ALT(METADATA_NSURI, "OrganizationUrl"),    // non-standard: KEYCLOAK-4040
    PDP_DESCRIPTOR(METADATA_NSURI, "PDPDescriptor"),
    REQUESTED_ATTRIBUTE(METADATA_NSURI, "RequestedAttribute"),
    ROLE_DESCRIPTOR(METADATA_NSURI, "RoleDescriptor"),
    SERVICE_DESCRIPTION(METADATA_NSURI, "ServiceDescription"),
    SERVICE_NAME(METADATA_NSURI, "ServiceName"),
    SINGLE_LOGOUT_SERVICE(METADATA_NSURI, "SingleLogoutService"),
    SINGLE_SIGNON_SERVICE(METADATA_NSURI, "SingleSignOnService"),
    SP_SSO_DESCRIPTOR(METADATA_NSURI, "SPSSODescriptor"),
    SURNAME(METADATA_NSURI, "SurName"),
    TELEPHONE_NUMBER(METADATA_NSURI, "TelephoneNumber"),

    // saml-schema-ecp-2.0.xsd
    RELAY_STATE(ECP_PROFILE, "RelayState"),
    REQUEST(ECP_PROFILE, "Request"),
    RESPONSE__ECP(ECP_PROFILE, "Response"),

    SIGNATURE(XMLDSIG_NSURI, "Signature"),
    DSA_KEY_VALUE(XMLDSIG_NSURI, "DSAKeyValue"),
    KEY_INFO(XMLDSIG_NSURI, "KeyInfo"),
    KEY_VALUE(XMLDSIG_NSURI, "KeyValue"),
    RSA_KEY_VALUE(XMLDSIG_NSURI, "RSAKeyValue"),
    X509_CERT(XMLDSIG_NSURI, "X509Certificate"),
    X509_DATA(XMLDSIG_NSURI, "X509Data"),

    // Attribute names and other constants
    ADDRESS("Address"),
    ALLOW_CREATE("AllowCreate"),
    ASSERTION_CONSUMER_SERVICE_URL("AssertionConsumerServiceURL"),
    ASSERTION_CONSUMER_SERVICE_INDEX("AssertionConsumerServiceIndex"),
    ATTRIBUTE_CONSUMING_SERVICE_INDEX("AttributeConsumingServiceIndex"),
    AUTHN_INSTANT("AuthnInstant"),
    AUTHN_REQUESTS_SIGNED("AuthnRequestsSigned"),
    BINDING("Binding"),
    CACHE_DURATION("cacheDuration"),
    COMPARISON("Comparison"),
    CONSENT("Consent"),
    CONTACT_TYPE("contactType"),
    DESTINATION("Destination"),
    DNS_NAME("DNSName"),
    ENCODING("Encoding"),
    ENCRYPTED_KEY("EncryptedKey"),
    ENTITY_ID("entityID"),
    FORMAT("Format"),
    FRIENDLY_NAME("FriendlyName"),
    FORCE_AUTHN("ForceAuthn"),
    ID("ID"),
    INDEX("index"),
    INPUT_CONTEXT_ONLY("InputContextOnly"),
    IN_RESPONSE_TO("InResponseTo"),
    ISDEFAULT("isDefault"),
    IS_REQUIRED("isRequired"),
    IS_PASSIVE("IsPassive"),
    ISSUE_INSTANT("IssueInstant"),
    LOCATION("Location"),
    METHOD("Method"),
    NAME("Name"),
    NAME_FORMAT("NameFormat"),
    NAME_QUALIFIER("NameQualifier"),
    NOT_BEFORE("NotBefore"),
    NOT_ON_OR_AFTER("NotOnOrAfter"),
    PROTOCOL_BINDING("ProtocolBinding"),
    PROTOCOL_SUPPORT_ENUMERATION("protocolSupportEnumeration"),
    PROVIDER_NAME("ProviderName"),
    REASON("Reason"),
    RECIPIENT("Recipient"),
    REQUEST_ABSTRACT("RequestAbstract"),
    RESPONSE_LOCATION("ResponseLocation"),
    RETURN_CONTEXT("ReturnContext"),
    SP_PROVIDED_ID("SPProvidedID"),
    SP_NAME_QUALIFIER("SPNameQualifier"),
    STATUS_RESPONSE_TYPE("StatusResponseType"),
    TYPE("type"),
    USE("use"),
    VALUE("Value"),
    VALID_UNTIL("validUntil"),
    VERSION("Version"),
    WANT_AUTHN_REQUESTS_SIGNED("WantAuthnRequestsSigned"),
    WANT_ASSERTIONS_SIGNED("WantAssertionsSigned"),
    XACML_AUTHZ_DECISION_QUERY("XACMLAuthzDecisionQuery"),
    XACML_AUTHZ_DECISION_QUERY_TYPE("XACMLAuthzDecisionQueryType"),
    XACML_AUTHZ_DECISION_STATEMENT_TYPE("XACMLAuthzDecisionStatementType"),
    REQUEST_AUTHENTICATED("RequestAuthenticated"),

    UNSOLICITED_RESPONSE_TARGET("TARGET"),
    UNSOLICITED_RESPONSE_SAML_VERSION("SAML_VERSION"),
    UNSOLICITED_RESPONSE_SAML_BINDING("SAML_BINDING"),

    LANG("lang"),
    LANG_EN("en"),
    METADATA_MIME("application/samlmetadata+xml"),
    SIGNATURE_SHA1_WITH_DSA("http://www.w3.org/2000/09/xmldsig#dsa-sha1"),
    SIGNATURE_SHA1_WITH_RSA("http://www.w3.org/2000/09/xmldsig#rsa-sha1"),
    VERSION_2_0("2.0"),

    /** @deprecated Use namespace-aware variant instead */
    RESPONSE("Response"),
    /** @deprecated Use namespace-aware variant instead */
    EXTENSIONS("Extensions"),

    UNKNOWN_VALUE(null)
    ;

    private final QName asQName;
    private final JBossSAMLURIConstants nsUri;

    private static class ReverseLookup {
        // Private class to make sure JBossSAMLURIConstants is fully initialized
        private static final Map<QName, JBossSAMLConstants> QNAME_CONSTANTS;
        private static final Map<String, JBossSAMLConstants> CONSTANTS;

        static {
            HashMap<QName, JBossSAMLConstants> q = new HashMap<>(JBossSAMLConstants.values().length);
            HashMap<String, JBossSAMLConstants> m = new HashMap<>(JBossSAMLConstants.values().length);
            JBossSAMLConstants old;
            for (JBossSAMLConstants c : JBossSAMLConstants.values()) {
                if ((old = q.put(c.getAsQName(), c)) != null) {
                    throw new IllegalStateException("Same name " + c.getAsQName() + " used for two distinct constants: " + c + ", " + old);
                }

                String key = c.get();
                if ((old = m.put(key, c)) != null) {
//                    System.out.println("WARNING: " + old);
                    if (old != null && c.getAsQName().equals(old.getAsQName())) {
                        throw new IllegalStateException("Same name " + key + " used for two distinct constants: " + c + ", " + old);
                    }
                    m.put(key, null);
                }
            }
            QNAME_CONSTANTS = Collections.unmodifiableMap(q);
            CONSTANTS = Collections.unmodifiableMap(m);
        }

        public JBossSAMLConstants from(String key) {
            return CONSTANTS.get(key);
        }

        public JBossSAMLConstants from(QName key) {
            return QNAME_CONSTANTS.get(key);
        }
    }
    private static final ReverseLookup REVERSE_LOOKUP = new ReverseLookup();

    private JBossSAMLConstants(String name) {
        this.asQName = name == null ? null : new QName(name);
        this.nsUri = null;
    }

    private JBossSAMLConstants(JBossSAMLURIConstants namespaceUri, String name) {
        this.nsUri = namespaceUri;
        this.asQName = name == null ? null : new QName(namespaceUri.get(), name);
    }

    public String get() {
        return this.asQName == null ? null : this.asQName.getLocalPart();
    }

    public QName getAsQName() {
        return asQName;
    }

    public JBossSAMLURIConstants getNsUri() {
        return nsUri;
    }

    /**
     * Returns an enum constant based if known for the given {@code key}, or the {@code defaultValue} otherwise.
     * @param key
     * @return
     */
    public static JBossSAMLConstants from(String key, JBossSAMLConstants defaultValue) {
        final JBossSAMLConstants res = REVERSE_LOOKUP.from(key);
        return res == null ? defaultValue : res;
    }

    /**
     * Returns an enum constant based if known for the given {@code key}, or the {@code UNKNOWN_VALUE} otherwise.
     * @param key
     * @return
     */
    public static JBossSAMLConstants from(String key) {
        return from(key, UNKNOWN_VALUE);
    }

    /**
     * Returns an enum constant based if known for the given {@code name} (namespace-aware), or the {@code UNKNOWN_VALUE} otherwise.
     * @param key
     * @return
     */
    public static JBossSAMLConstants from(QName name) {
        final JBossSAMLConstants res = REVERSE_LOOKUP.from(name);
        return res == null ? UNKNOWN_VALUE : res;
    }
}