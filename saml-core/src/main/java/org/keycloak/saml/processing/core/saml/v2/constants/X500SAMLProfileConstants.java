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
package org.keycloak.saml.processing.core.saml.v2.constants;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * X500 SAML Profile Constants Adapted from
 * http://code.google.com/p/simplesamlphp/source/browse/trunk/attributemap/name2oid.php?r=2654
 *
 * @author Anil.Saldhana@redhat.com
 * @since Sep 11, 2009
 */
public enum X500SAMLProfileConstants {
    A_RECORD("aRecord", "urn:oid:0.9.2342.19200300.100.1.26"), ALIASED_ENTRY_NAME("aliasedEntryName", "urn:oid:2.5.4.1"), ALIASED_OBJECT_NAME(
            "aliasedObjectName", "urn:oid:2.5.4.1"), ASSOCIATED_DOMAIN("associatedDomain", "urn:oid:0.9.2342.19200300.100.1.37"), ASSOCIATED_NAME(
            "associatedName", "urn:oid:0.9.2342.19200300.100.1.38"), AUDIO("audio", "urn:oid:0.9.2342.19200300.100.1.55"), AUTHORITY_REVOCATION_LIST(
            "authorityRevocationList", "urn:oid:2.5.4.38"), BINDING_NAME("buildingName", "urn:oid:0.9.2342.19200300.100.1.48"), BUSINESS_CATEGORY(
            "businessCategory", "urn:oid:2.5.4.15"), C("c", "urn:oid:2.5.4.6"), CA_CERTIFICATE("cACertificate",
            "urn:oid:2.5.4.37"), CNAME_RECORD("cNAMERecord", "urn:oid:0.9.2342.19200300.100.1.31"), CAR_LICENSE("carLicense",
            "urn:oid:2.16.840.1.113730.3.1.1"), CRL("certificateRevocationList", "urn:oid:2.5.4.39"), CN("cn",
            "urn:oid:2.5.4.3"), CO("co", "urn:oid:0.9.2342.19200300.100.1.43"), COMMON_NAME("commonName", "urn:oid:2.5.4.3"), COUNTRY_NAME(
            "countryName", "urn:oid:2.5.4.6"), CROSS_CERTIFICATE_PAIR("crossCertificatePair", "urn:oid:2.5.4.40"), D_IT_REDIRECT(
            "dITRedirect", "urn:oid:0.9.2342.19200300.100.1.54"), D_SA_QUALITY("dSAQuality",
            "urn:oid:0.9.2342.19200300.100.1.49"), DC("dc", "urn:oid:0.9.2342.19200300.100.1.25"), DELTA_REVOCATION_LIST(
            "deltaRevocationList", "urn:oid:2.5.4.53"), DEPARTMENT_NUMBER("departmentNumber", "urn:oid:2.16.840.1.113730.3.1.2"), DESCRIPTION(
            "description", "urn:oid:2.5.4.13"), DESTINATION_INDICATOR("destinationIndicator", "urn:oid:2.5.4.27"), DISPLAY_NAME(
            "displayName", "urn:oid:2.16.840.1.113730.3.1.241"), DISTINGUISHED_NAME("distinguishedName", "urn:oid:2.5.4.49"), DMD_NAME(
            "dmdName", "urn:oid:2.5.4.54"), DN_QUALIFIER("dnQualifier", "urn:oid:2.5.4.46"), DOCUMENT_AUTHOR("documentAuthor",
            "urn:oid:0.9.2342.19200300.100.1.14"), DOCUMENT_IDENTIFIER("documentIdentifier",
            "urn:oid:0.9.2342.19200300.100.1.11"), DOCUMENT_LOCATION("documentLocation", "urn:oid:0.9.2342.19200300.100.1.15"), DOCUMENT_PUBLISHER(
            "documentPublisher", "urn:oid:0.9.2342.19200300.100.1.56"), DOCUMENT_TITLE("documentTitle",
            "urn:oid:0.9.2342.19200300.100.1.12"), DOCUMENT_VERSION("documentVersion", "urn:oid:0.9.2342.19200300.100.1.13"), DOMAIN_COMPONENT(
            "domainComponent", "urn:oid:0.9.2342.19200300.100.1.25"), DRINK("drink", "urn:oid:0.9.2342.19200300.100.1.5"), EDU_ORG_HOMEPAGEURI(
            "eduOrgHomePageURI", "urn:oid:1.3.6.1.4.1.5923.1.2.1.2"), EDU_ORG_IDENTITY_AUTHN_POLICYURI(
            "eduOrgIdentityAuthNPolicyURI", "urn:oid:1.3.6.1.4.1.5923.1.2.1.3"), EDU_ORG_LEGALNAME("eduOrgLegalName",
            "urn:oid:1.3.6.1.4.1.5923.1.2.1.4"), EDU_ORG_SUPERIORURI("eduOrgSuperiorURI", "urn:oid:1.3.6.1.4.1.5923.1.2.1.5"), EDU_ORG_WHITEPAGESURI(
            "eduOrgWhitePagesURI", "urn:oid:1.3.6.1.4.1.5923.1.2.1.6"), EDU_PERSON_AFFLIATION("eduPersonAffiliation",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.1"), EDU_PERSON_ENTITLEMENT("eduPersonEntitlement",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.7"), EDU_PERSON_NICKNAME("eduPersonNickname", "urn:oid:1.3.6.1.4.1.5923.1.1.1.2"), EDU_PERSON_ORG_DN(
            "eduPersonOrgDN", "urn:oid:1.3.6.1.4.1.5923.1.1.1.3"), EDU_PERSION_ORG_UNIT_DN("eduPersonOrgUnitDN",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.4"), EDU_PERSON_PRIMARY_AFFLIATION("eduPersonPrimaryAffiliation",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.5"), EDU_PERSON_PRIMARY_ORG_UNIT_DN("eduPersonPrimaryOrgUnitDN",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.8"), EDU_PERSON_PRINCIPAL_NAME("eduPersonPrincipalName",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.6"), EDU_PERSON_SCOPED_AFFLIATION("eduPersonScopedAffiliation",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.9"), EDU_PERSON_TARGETED_ID("eduPersonTargetedID",
            "urn:oid:1.3.6.1.4.1.5923.1.1.1.10"), EMAIL("email", "urn:oid:1.2.840.113549.1.9.1"), EMAIL_ADDRESS("emailAddress",
            "urn:oid:1.2.840.113549.1.9.1"), EMPLOYEE_NUMBER("employeeNumber", "urn:oid:2.16.840.1.113730.3.1.3"), EMPLOYEE_TYPE(
            "employeeType", "urn:oid:2.16.840.1.113730.3.1.4"), ENHANCED_SEARCH_GUIDE("enhancedSearchGuide", "urn:oid:2.5.4.47"), FAX_TELEPHONE_NUMBER(
            "facsimileTelephoneNumber", "urn:oid:2.5.4.23"), FAVORITE_DRINK("favouriteDrink",
            "urn:oid:0.9.2342.19200300.100.1.5"), FAX("fax", "urn:oid:2.5.4.23"), FEDERATION_FEIDE_SCHEMA_VERSION(
            "federationFeideSchemaVersion", "urn:oid:1.3.6.1.4.1.2428.90.1.9"), FRIENDLY_COUNTRY_NAME("friendlyCountryName",
            "urn:oid:0.9.2342.19200300.100.1.43"), GENERATION_QUALIFIER("generationQualifier", "urn:oid:2.5.4.44"), GIVEN_NAME(
            "givenName", "urn:oid:2.5.4.42"), GN("gn", "urn:oid:2.5.4.42"), HOME_PHONE("homePhone",
            "urn:oid:0.9.2342.19200300.100.1.20"), HOME_POSTAL_ADDRESS("homePostalAddress",
            "urn:oid:0.9.2342.19200300.100.1.39"), HOME_TELEPHONE_NUMBER("homeTelephoneNumber",
            "urn:oid:0.9.2342.19200300.100.1.20"), HOST("host", "urn:oid:0.9.2342.19200300.100.1.9"), HOUSE_IDENTIFIER(
            "houseIdentifier", "urn:oid:2.5.4.51"), INFO("info", "urn:oid:0.9.2342.19200300.100.1.4"), INITIALS("initials",
            "urn:oid:2.5.4.43"), INTERNATIONAL_ISDN_NUMBER("internationaliSDNNumber", "urn:oid:2.5.4.25"), IS_MEMBEROF(
            "isMemberOf", "urn:oid:1.3.6.1.4.1.5923.1.5.1.1"), JANET_MAILBOX("janetMailbox",
            "urn:oid:0.9.2342.19200300.100.1.46"), JPEG_PHOTO("jpegPhoto", "urn:oid:0.9.2342.19200300.100.1.60"), KNOWLEDGE_INFORMATION(
            "knowledgeInformation", "urn:oid:2.5.4.2"), L("l", "urn:oid:2.5.4.7"), LABELED_URI("labeledURI",
            "urn:oid:1.3.6.1.4.1.250.1.57"), LOCALITY_NAME("localityName", "urn:oid:2.5.4.7"), M_DRECORD("mDRecord",
            "urn:oid:0.9.2342.19200300.100.1.27"), M_XRECORD("mXRecord", "urn:oid:0.9.2342.19200300.100.1.28"), MAIL("mail",
            "urn:oid:0.9.2342.19200300.100.1.3"), MAIL_PREFERENCEOPTION("mailPreferenceOption",
            "urn:oid:0.9.2342.19200300.100.1.47"), MANAGER("manager", "urn:oid:0.9.2342.19200300.100.1.10"), MEMBER("member",
            "urn:oid:2.5.4.31"), MOBILE("mobile", "urn:oid:0.9.2342.19200300.100.1.41"), MOBILE_TELEPHONE_NUMBER(
            "mobileTelephoneNumber", "urn:oid:0.9.2342.19200300.100.1.41"), N_SRECORD("nSRecord",
            "urn:oid:0.9.2342.19200300.100.1.29"), NAME("name", "urn:oid:2.5.4.41"), NOR_EDU_ORG_ACRONYM("norEduOrgAcronym",
            "urn:oid:1.3.6.1.4.1.2428.90.1.6"), NOR_EDU_ORG_NIN("norEduOrgNIN", "urn:oid:1.3.6.1.4.1.2428.90.1.12"), NOR_EDU_ORG_SCHEMA_VERSION(
            "norEduOrgSchemaVersion", "urn:oid:1.3.6.1.4.1.2428.90.1.11"), NOR_EDU_ORG_UNIQUE_IDENTIFIER(
            "norEduOrgUniqueIdentifier", "urn:oid:1.3.6.1.4.1.2428.90.1.7"), NOR_EDU_ORG_UNIQUE_NUMBER("norEduOrgUniqueNumber",
            "urn:oid:1.3.6.1.4.1.2428.90.1.1"), NOR_EDU_ORG_UNIT_UNIQUE_IDENTIFIER("norEduOrgUnitUniqueIdentifier",
            "urn:oid:1.3.6.1.4.1.2428.90.1.8"), NOR_EDU_ORG_UNIT_UNIQUE_NUMBER("norEduOrgUnitUniqueNumber",
            "urn:oid:1.3.6.1.4.1.2428.90.1.2"), NOR_EDU_PERSON_BIRTH_DATE("norEduPersonBirthDate",
            "urn:oid:1.3.6.1.4.1.2428.90.1.3"), NOR_EDU_PERSON_LIN("norEduPersonLIN", "urn:oid:1.3.6.1.4.1.2428.90.1.4"), NOR_EDU_PERSON_NIN(
            "norEduPersonNIN", "urn:oid:1.3.6.1.4.1.2428.90.1.5"), O("o", "urn:oid:2.5.4.10"), OBJECT_CLASS("objectClass",
            "urn:oid:2.5.4.0"), ORGANIZATION_NAME("organizationName", "urn:oid:2.5.4.10"), ORGANIZATIONAL_STATUS(
            "organizationalStatus", "urn:oid:0.9.2342.19200300.100.1.45"), ORGANIZATIONAL_UNIT_NAME("organizationalUnitName",
            "urn:oid:2.5.4.11"), OTHER_MAIL_BOX("otherMailbox", "urn:oid:0.9.2342.19200300.100.1.22"), OU("ou",
            "urn:oid:2.5.4.11"), OWNER("owner", "urn:oid:2.5.4.32"), PAGER("pager", "urn:oid:0.9.2342.19200300.100.1.42"), PAGER_TELEPHONE_NUMBER(
            "pagerTelephoneNumber", "urn:oid:0.9.2342.19200300.100.1.42"), PERSONAL_SIGNATURE("personalSignature",
            "urn:oid:0.9.2342.19200300.100.1.53"), PERSONAL_TITLE("personalTitle", "urn:oid:0.9.2342.19200300.100.1.40"), PHOTO(
            "photo", "urn:oid:0.9.2342.19200300.100.1.7"), PHYSICAL_DELIVERY_OFFICE_NAME("physicalDeliveryOfficeName",
            "urn:oid:2.5.4.19"), PKCS_9_EMAIL("pkcs9email", "urn:oid:1.2.840.113549.1.9.1"), POST_OFFICE_BOX("postOfficeBox",
            "urn:oid:2.5.4.18"), POSTAL_ADDRESS("postalAddress", "urn:oid:2.5.4.16"), POSTAL_CODE("postalCode",
            "urn:oid:2.5.4.17"), PREFERRED_DELIVERY_METHOD("preferredDeliveryMethod", "urn:oid:2.5.4.28"), PREFERRED_LANGUAGE(
            "preferredLanguage", "urn:oid:2.16.840.1.113730.3.1.39"), PRESENTATION_ADDRESS("presentationAddress",
            "urn:oid:2.5.4.29"), PROTOCOL_INFORMATION("protocolInformation", "urn:oid:2.5.4.48"), PSEUDONYM("pseudonym",
            "urn:oid:2.5.4.65"), REGISTERED_ADDRESS("registeredAddress", "urn:oid:2.5.4.26"), RFC_822_MAILBOX("rfc822Mailbox",
            "urn:oid:0.9.2342.19200300.100.1.3"), ROLE_OCCUPANT("roleOccupant", "urn:oid:2.5.4.33"), ROOM_NUMBER("roomNumber",
            "urn:oid:0.9.2342.19200300.100.1.6"), S_OAR_RECORD("sOARecord", "urn:oid:0.9.2342.19200300.100.1.30"), SEARCH_GUIDE(
            "searchGuide", "urn:oid:2.5.4.14"), SECRETARY("secretary", "urn:oid:0.9.2342.19200300.100.1.21"), SEE_ALSO(
            "seeAlso", "urn:oid:2.5.4.34"), SERIAL_NUMBER("serialNumber", "urn:oid:2.5.4.5"), SINGLE_LEVEL_QUALITY(
            "singleLevelQuality", "urn:oid:0.9.2342.19200300.100.1.50"), SN("sn", "urn:oid:2.5.4.4"), ST("st",
            "urn:oid:2.5.4.8"), STATE_OR_PROVINCE_NAME("stateOrProvinceName", "urn:oid:2.5.4.8"), STREET("street",
            "urn:oid:2.5.4.9"), STREET_ADDRESS("streetAddress", "urn:oid:2.5.4.9"), SUBTREE_MAXIMUM_QUALITY(
            "subtreeMaximumQuality", "urn:oid:0.9.2342.19200300.100.1.52"), SUBTREE_MINIMUM_QUALITY("subtreeMinimumQuality",
            "urn:oid:0.9.2342.19200300.100.1.51"), SUPPORTED_ALGORITHMS("supportedAlgorithms", "urn:oid:2.5.4.52"), SUPPORTED_APPLICATION_CONTEXT(
            "supportedApplicationContext", "urn:oid:2.5.4.30"), SURNAME("surname", "urn:oid:2.5.4.4"), TELEPHONE_NUMBER(
            "telephoneNumber", "urn:oid:2.5.4.20"), TELETEX_TERMINAL_IDENTIFIER("teletexTerminalIdentifier", "urn:oid:2.5.4.22"), TELEX_NUMBER(
            "telexNumber", "urn:oid:2.5.4.21"), TEXT_ENCODED_OR_ADDRESS("textEncodedORAddress",
            "urn:oid:0.9.2342.19200300.100.1.2"), TITLE("title", "urn:oid:2.5.4.12"), UID("uid",
            "urn:oid:0.9.2342.19200300.100.1.1"), UNIQUE_IDENTIFIER("uniqueIdentifier", "urn:oid:0.9.2342.19200300.100.1.44"), UNIQUE_MEMBER(
            "uniqueMember", "urn:oid:2.5.4.50"), USER_CERTIFICATE("userCertificate", "urn:oid:2.5.4.36"), USER_CLASS(
            "userClass", "urn:oid:0.9.2342.19200300.100.1.8"), USER_PKCS12("userPKCS12", "urn:oid:2.16.840.1.113730.3.1.216"), USER_PASSWORD(
            "userPassword", "urn:oid:2.5.4.35"), USER_SMIME_CERTIFICATE("userSMIMECertificate",
            "urn:oid:2.16.840.1.113730.3.1.40"), USERID("userid", "urn:oid:0.9.2342.19200300.100.1.1"), X121_ADDRESS(
            "x121Address", "urn:oid:2.5.4.24"), X500_UNIQUE_IDENTIFIER("x500UniqueIdentifier", "urn:oid:2.5.4.45");

    private String friendlyName = null;
    private String uri = null;

    private static final Map<String, String> lookup = new HashMap<>();

    static {
        for (X500SAMLProfileConstants s : EnumSet.allOf(X500SAMLProfileConstants.class))
            lookup.put(s.friendlyName, s.uri);
    }

    X500SAMLProfileConstants(String friendlyName, String uristr) {
        this.uri = uristr;
        this.friendlyName = friendlyName;
    }

    public String get() {
        return this.uri;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public boolean correspondsTo(AttributeType attribute) {
        return attribute != null
            ? Objects.equals(this.uri, attribute.getName()) || Objects.equals(this.friendlyName, attribute.getFriendlyName())
            : false;
    }

    public static String getOID(final String key) {
        return lookup.get(key);
    }
}