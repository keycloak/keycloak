package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import org.keycloak.saml.processing.core.parsers.saml.xmldsig.XmlDSigQNames;
import org.keycloak.saml.processing.core.parsers.util.HasQName;

import javax.xml.namespace.QName;


/**
 * @author mhajas
 */
public enum SAMLMetadataQNames implements HasQName {
    ADDITIONAL_METADATA_LOCATION("AdditionalMetadataLocation"),
    AFFILIATE_MEMBER("AffiliateMember"),
    AFFILIATION_DESCRIPTOR("AffiliationDescriptor"),
    ARTIFACT_RESOLUTION_SERVICE("ArtifactResolutionService"),
    ASSERTION_CONSUMER_SERVICE("AssertionConsumerService"),
    ASSERTION_ID_REQUEST_SERVICE("AssertionIDRequestService"),
    ATTRIBUTE_AUTHORITY_DESCRIPTOR("AttributeAuthorityDescriptor"),
    ATTRIBUTE_CONSUMING_SERVICE("AttributeConsumingService"),
    ATTRIBUTE_PROFILE("AttributeProfile"),
    ATTRIBUTE_SERVICE("AttributeService"),
    ATTRIBUTE_VALUE("AttributeValue"),
    AUTHN_AUTHORITY_DESCRIPTOR("AuthnAuthorityDescriptor"),
    AUTHN_QUERY_SERVICE("AuthnQueryService"),
    AUTHZ_SERVICE("AuthzService"),
    COMPANY("Company"),
    CONTACT_PERSON("ContactPerson"),
    EMAIL_ADDRESS("EmailAddress"),
    ENCRYPTION_METHOD("EncryptionMethod"),
    ENTITIES_DESCRIPTOR("EntitiesDescriptor"),
    ENTITY_DESCRIPTOR("EntityDescriptor"),
    EXTENSIONS("Extensions"),
    GIVEN_NAME("GivenName"),
    IDP_SSO_DESCRIPTOR("IDPSSODescriptor"),
    KEY_DESCRIPTOR("KeyDescriptor"),
    MANAGE_NAMEID_SERVICE("ManageNameIDService"),
    NAMEID_FORMAT("NameIDFormat"),
    NAMEID_MAPPING_SERVICE("NameIDMappingService"),
    ORGANIZATION_DISPLAY_NAME("OrganizationDisplayName"),
    ORGANIZATION_NAME("OrganizationName"),
    ORGANIZATION("Organization"),
    ORGANIZATION_URL("OrganizationURL"),
    ORGANIZATION_URL_ALT("OrganizationUrl"),    // non-standard: KEYCLOAK-4040,
    PDP_DESCRIPTOR("PDPDescriptor"),
    REQUESTED_ATTRIBUTE("RequestedAttribute"),
    ROLE_DESCRIPTOR("RoleDescriptor"),
    SERVICE_DESCRIPTION("ServiceDescription"),
    SERVICE_NAME("ServiceName"),
    SINGLE_LOGOUT_SERVICE("SingleLogoutService"),
    SINGLE_SIGNON_SERVICE("SingleSignOnService"),
    SP_SSO_DESCRIPTOR("SPSSODescriptor"),
    SURNAME("SurName"),
    TELEPHONE_NUMBER("TelephoneNumber"),

    //mdui elements
    DESCRIPTION(JBossSAMLURIConstants.METADATA_UI, "Description"),
    DISPLAY_NAME(JBossSAMLURIConstants.METADATA_UI, "DisplayName"),
    INFORMATION_URL(JBossSAMLURIConstants.METADATA_UI, "InformationURL"),
    KEYWORDS(JBossSAMLURIConstants.METADATA_UI, "Keywords"),
    LOGO(JBossSAMLURIConstants.METADATA_UI, "Logo"),
    PRIVACY_STATEMENT_URL(JBossSAMLURIConstants.METADATA_UI, "PrivacyStatementURL"),
    UIINFO(JBossSAMLURIConstants.METADATA_UI, "UIInfo"),

    // Attribute names
    ATTR_ENTITY_ID(null, "entityID"),
    ATTR_ID(null, "ID"),
    ATTR_VALID_UNTIL(null, "validUntil"),
    ATTR_CACHE_DURATION(null, "cacheDuration"),
    ATTR_PROTOCOL_SUPPORT_ENUMERATION(null, "protocolSupportEnumeration"),
    ATTR_USE(null, "use"),
    ATTR_ALGORITHM(null, "Algorithm"),
    ATTR_LANG(JBossSAMLURIConstants.XML, "lang"),
    ATTR_CONTACT_TYPE(null, "contactType"),
    ATTR_AUTHN_REQUESTS_SIGNED(null, "AuthnRequestsSigned"),
    ATTR_WANT_ASSERTIONS_SIGNED(null, "WantAssertionsSigned"),
    ATTR_WANT_AUTHN_REQUESTS_SIGNED(null, "WantAuthnRequestsSigned"),
    ATTR_BINDING(null, "Binding"),
    ATTR_LOCATION(null, "Location"),
    ATTR_IS_DEFAULT(null, "isDefault"),
    ATTR_INDEX(null, "index"),
    ATTR_RESPONSE_LOCATION(null, "ResponseLocation"),
    ATTR_FRIENDLY_NAME(null, "FriendlyName"),
    ATTR_IS_REQUIRED(null, "isRequired"),
    ATTR_NAME(null, "Name"),
    ATTR_NAME_FORMAT(null, "NameFormat"),
    ATTR_WIDTH(null, "width"),
    ATTR_HEIGHT(null, "height"),
    // Elements from other namespaces that can be direct subelements of this namespace's elements
    SIGNATURE(XmlDSigQNames.SIGNATURE),
    KEY_INFO(XmlDSigQNames.KEY_INFO),
    KEY_SIZE(JBossSAMLURIConstants.XMLENC_NSURI, "KeySize"),
    OAEP_PARAMS(JBossSAMLURIConstants.XMLENC_NSURI, "OAEPparams"),
    ATTR_X500_ENCODING(JBossSAMLURIConstants.X500_NSURI, "Encoding"),
    ATTRIBUTE(SAMLAssertionQNames.ATTRIBUTE),
    ASSERTION(SAMLAssertionQNames.ASSERTION),
    ENTITY_ATTRIBUTES(JBossSAMLURIConstants.METADATA_ENTITY_ATTRIBUTES_NSURI, "EntityAttributes"),

    UNKNOWN_ELEMENT("");

    private final QName qName;

    SAMLMetadataQNames(String localName) {
        this.qName = new QName(JBossSAMLURIConstants.METADATA_NSURI.get(), localName);
    }

    SAMLMetadataQNames(HasQName source) {
        this.qName = source.getQName();
    }

    SAMLMetadataQNames(JBossSAMLURIConstants nsUri, String localName) {
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
