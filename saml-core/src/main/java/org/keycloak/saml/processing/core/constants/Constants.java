package org.keycloak.saml.processing.core.constants;

/**
 * Constants class of OneLogin's Java Toolkit.
 *
 * A class that contains several constants related to the SAML protocol
 */
public final class Constants {
    /**
     * Value added to the current time in time condition validations.
     */
    public static final Integer ALOWED_CLOCK_DRIFT = 180; // 3 min in seconds

    // NameID Formats
    public static final String NAMEID_EMAIL_ADDRESS = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
    public static final String NAMEID_X509_SUBJECT_NAME = "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
    public static final String NAMEID_WINDOWS_DOMAIN_QUALIFIED_NAME = "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
    public static final String NAMEID_UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
    public static final String NAMEID_KERBEROS = "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos";
    public static final String NAMEID_ENTITY = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
    public static final String NAMEID_TRANSIENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
    public static final String NAMEID_PERSISTENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
    public static final String NAMEID_ENCRYPTED = "urn:oasis:names:tc:SAML:2.0:nameid-format:encrypted";

    // Attribute Name Formats
    public static final String ATTRNAME_FORMAT_UNSPECIFIED = "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified";
    public static final String ATTRNAME_FORMAT_URI = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
    public static final String ATTRNAME_FORMAT_BASIC = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    // Namespaces
    public static final String NS_SAML = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String NS_SAMLP = "urn:oasis:names:tc:SAML:2.0:protocol";
    public static final String NS_SOAP = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String NS_MD = "urn:oasis:names:tc:SAML:2.0:metadata";
    public static final String NS_XS = "http://www.w3.org/2001/XMLSchema";
    public static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String NS_XENC = "http://www.w3.org/2001/04/xmlenc#";
    public static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";

    // Bindings
    public static final String BINDING_HTTP_POST = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    public static final String BINDING_HTTP_REDIRECT = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
    public static final String BINDING_HTTP_ARTIFACT = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";
    public static final String BINDING_SOAP = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";
    public static final String BINDING_DEFLATE = "urn:oasis:names:tc:SAML:2.0:bindings:URL-Encoding:DEFLATE";

    // Auth Context Class
    public static final String AC_UNSPECIFIED = "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified";
    public static final String AC_PASSWORD = "urn:oasis:names:tc:SAML:2.0:ac:classes:Password";
    public static final String AC_X509 = "urn:oasis:names:tc:SAML:2.0:ac:classes:X509";
    public static final String AC_SMARTCARD = "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard";
    public static final String AC_KERBEROS = "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos";

    // Subject Confirmation
    public static final String CM_BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
    public static final String CM_HOLDER_KEY = "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    public static final String CM_SENDER_VOUCHES = "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";

    // Status Codes
    public static final String STATUS_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
    public static final String STATUS_REQUESTER = "urn:oasis:names:tc:SAML:2.0:status:Requester";
    public static final String STATUS_RESPONDER = "urn:oasis:names:tc:SAML:2.0:status:Responder";
    public static final String STATUS_VERSION_MISMATCH = "urn:oasis:names:tc:SAML:2.0:status:VersionMismatch";

    // Status Second-level Codes
    public static final String STATUS_AUTHNFAILED = "urn:oasis:names:tc:SAML:2.0:status:AuthnFailed";
    public static final String STATUS_INVALID_ATTRNAME_OR_VALUE =  "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";
    public static final String STATUS_INVALID_NAMEIDPOLICY = "urn:oasis:names:tc:SAML:2.0:status:InvalidNameIDPolicy";
    public static final String STATUS_NO_AUTHNCONTEXT = "urn:oasis:names:tc:SAML:2.0:status:NoAuthnContext";
    public static final String STATUS_NO_AVAILABLE_IDP = "urn:oasis:names:tc:SAML:2.0:status:NoAvailableIDP";
    public static final String STATUS_NO_PASSIVE = "urn:oasis:names:tc:SAML:2.0:status:NoPassive";
    public static final String STATUS_NO_SUPPORTED_IDP = "urn:oasis:names:tc:SAML:2.0:status:NoSupportedIDP";
    public static final String STATUS_PARTIAL_LOGOUT = "urn:oasis:names:tc:SAML:2.0:status:PartialLogout";
    public static final String STATUS_PROXY_COUNT_EXCEEDED = "urn:oasis:names:tc:SAML:2.0:status:ProxyCountExceeded";
    public static final String STATUS_REQUEST_DENIED = "urn:oasis:names:tc:SAML:2.0:status:RequestDenied";
    public static final String STATUS_REQUEST_UNSUPPORTED = "urn:oasis:names:tc:SAML:2.0:status:RequestUnsupported";
    public static final String STATUS_REQUEST_VERSION_DEPRECATED = "urn:oasis:names:tc:SAML:2.0:status:RequestVersionDeprecated";
    public static final String STATUS_REQUEST_VERSION_TOO_HIGH = "urn:oasis:names:tc:SAML:2.0:status:RequestVersionTooHigh";
    public static final String STATUS_REQUEST_VERSION_TOO_LOW = "urn:oasis:names:tc:SAML:2.0:status:RequestVersionTooLow";
    public static final String STATUS_RESOURCE_NOT_RECOGNIZED = "urn:oasis:names:tc:SAML:2.0:status:ResourceNotRecognized";
    public static final String STATUS_TOO_MANY_RESPONSES = "urn:oasis:names:tc:SAML:2.0:status:TooManyResponses";
    public static final String STATUS_UNKNOWN_ATTR_PROFILE = "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";
    public static final String STATUS_UNKNOWN_PRINCIPAL = "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";
    public static final String STATUS_UNSUPPORTED_BINDING = "urn:oasis:names:tc:SAML:2.0:status:UnsupportedBinding";

    // Contact types
    public static final String CONTACT_TYPE_TECHNICAL = "technical";
    public static final String CONTACT_TYPE_SUPPORT = "support";
    public static final String CONTACT_TYPE_ADMINISTRATIVE = "administrative";
    public static final String CONTACT_TYPE_BILLING = "billing";
    public static final String CONTACT_TYPE_OTHER = "other";

    // Canonization
    public static final String C14N = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
    public static final String C14N_WC = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";
    public static final String C14N11 = "http://www.w3.org/2006/12/xml-c14n11";
    public static final String C14N11_WC = "http://www.w3.org/2006/12/xml-c14n11#WithComments";
    public static final String C14NEXC = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String C14NEXC_WC = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    // Sign & Crypt
    // https://www.w3.org/TR/xmlenc-core/#sec-Alg-MessageDigest
    // https://www.w3.org/TR/xmlsec-algorithms/#signature-method-uris
    // https://tools.ietf.org/html/rfc6931
    public static final String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";
    public static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";
    public static final String SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";
    public static final String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    public static final String DSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";
    public static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    public static final String RSA_SHA256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    public static final String RSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";
    public static final String RSA_SHA512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

    public static final String TRIPLEDES_CBC = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
    public static final String AES128_CBC = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    public static final String AES192_CBC = "http://www.w3.org/2001/04/xmlenc#aes192-cbc";
    public static final String AES256_CBC = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
    public static final String A128KW = "http://www.w3.org/2001/04/xmlenc#kw-aes128";
    public static final String A192KW = "http://www.w3.org/2001/04/xmlenc#kw-aes192";
    public static final String A256KW = "http://www.w3.org/2001/04/xmlenc#kw-aes256";
    public static final String RSA_1_5 = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String RSA_OAEP_MGF1P = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";

    public static final String ENVSIG = "http://www.w3.org/2000/09/xmldsig#enveloped-signature";

    private Constants() {
        //not called
    }

}
