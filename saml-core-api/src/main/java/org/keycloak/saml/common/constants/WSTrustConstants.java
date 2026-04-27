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

import javax.xml.namespace.QName;

/**
 * <p> This class defines the constants used throughout the WS-Trust implementation code. </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:asaldhan@redhat.com">Anil Saldhana</a>
 */
public interface WSTrustConstants {

    String BASE_NAMESPACE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";

    String PREFIX = "wst";

    // WS-Trust request types
    String BATCH_ISSUE_REQUEST = BASE_NAMESPACE + "/BatchIssue";

    String ISSUE_REQUEST = BASE_NAMESPACE + "/Issue";

    String RENEW_REQUEST = BASE_NAMESPACE + "/Renew";

    String CANCEL_REQUEST = BASE_NAMESPACE + "/Cancel";

    String VALIDATE_REQUEST = BASE_NAMESPACE + "/Validate";

    String BATCH_VALIDATE_REQUEST = BASE_NAMESPACE + "/BatchValidate";

    // WS-Trust validation constants.
    String STATUS_TYPE = BASE_NAMESPACE + "/RSTR/Status";

    String STATUS_CODE_VALID = BASE_NAMESPACE + "/status/valid";

    String STATUS_CODE_INVALID = BASE_NAMESPACE + "/status/invalid";

    // WS-Trust key types.
    String KEY_TYPE_BEARER = BASE_NAMESPACE + "/Bearer";

    String KEY_TYPE_SYMMETRIC = BASE_NAMESPACE + "/SymmetricKey";

    String KEY_TYPE_PUBLIC = BASE_NAMESPACE + "/PublicKey";

    // WS-Trust binary secret types.
    String BS_TYPE_ASYMMETRIC = BASE_NAMESPACE + "/AsymmetricKey";

    String BS_TYPE_SYMMETRIC = BASE_NAMESPACE + "/SymmetricKey";

    String BS_TYPE_NONCE = BASE_NAMESPACE + "/Nonce";

    // WS-Trust computed key types.
    String CK_PSHA1 = BASE_NAMESPACE + "/CK/PSHA1";

    // WSS namespaces values.
    String WSA_NS = "http://www.w3.org/2005/08/addressing";

    String WSP_NS = "http://schemas.xmlsoap.org/ws/2004/09/policy";

    String WSP_15_NS = "http://www.w3.org/ns/ws-policy";

    String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";

    String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    String WSSE11_NS = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";

    String XENC_NS = "http://www.w3.org/2001/04/xmlenc#";

    String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#";

    String SAML2_ASSERTION_NS = "urn:oasis:names:tc:SAML:2.0:assertion";

    // WSS Fault codes
    QName SECURITY_TOKEN_UNAVAILABLE = new QName(WSSE_NS, "SecurityTokenUnavailable");

    QName INVALID_SECURITY_TOKEN = new QName(WSSE_NS, "InvalidSecurityToken");

    QName INVALID_SECURITY = new QName(WSSE_NS, "InvalidSecurity");

    QName FAILED_AUTHENTICATION = new QName(WSSE_NS, "FailedAuthentication");

    // Token Types
    String RSTR_STATUS_TOKEN_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/Status";

    // Element Names
    String BINARY_SECRET = "BinarySecret";

    String CREATED = "Created";

    String COMPUTED_KEY_ALGORITHM = "ComputedKeyAlgorithm";

    String ENTROPY = "Entropy";

    String EXPIRES = "Expires";

    String ISSUER = "Issuer";

    String ON_BEHALF_OF = "OnBehalfOf";

    String COMPUTED_KEY = "ComputedKey";

    String KEY_SIZE = "KeySize";

    String KEY_TYPE = "KeyType";

    String LIFETIME = "Lifetime";

    String RENEWING = "Renewing";

    String RST = "RequestSecurityToken";

    String RSTR = "RequestSecurityTokenResponse";

    String RST_COLLECTION = "RequestSecurityTokenCollection";

    String RSTR_COLLECTION = "RequestSecurityTokenResponseCollection";

    String REQUESTED_TOKEN = "RequestedSecurityToken";

    String REQUESTED_TOKEN_CANCELLED = "RequestedTokenCancelled";

    String REQUESTED_PROOF_TOKEN = "RequestedProofToken";

    String REQUESTED_ATTACHED_REFERENCE = "RequestedAttachedReference";

    String REQUESTED_UNATTACHED_REFERENCE = "RequestedUnattachedReference";

    String REQUEST_TYPE = "RequestType";

    String TOKEN_TYPE = "TokenType";

    String CANCEL_TARGET = "CancelTarget";

    String RENEW_TARGET = "RenewTarget";

    String SECONDARY_PARAMETERS = "SecondaryParameters";

    String VALIDATE_TARGET = "ValidateTarget";

    String USE_KEY = "UseKey";

    String STATUS = "Status";

    String CODE = "Code";

    String REASON = "Reason";

    // Attribute Names
    String ALLOW = "Allow";

    String OK = "OK";

    String RST_CONTEXT = "Context";

    String TYPE = "Type";

    String VALUE_TYPE = "ValueType";

    public interface XMLDSig {

        String DSIG_NS = "http://www.w3.org/2000/09/xmldsig#";

        String EXPONENT = "Exponent";

        String KEYINFO = "KeyInfo";

        String KEYVALUE = "KeyValue";

        String MODULUS = "Modulus";

        String DSIG_PREFIX = "ds";

        String RSA_KEYVALUE = "RSAKeyValue";

        String DSA_KEYVALUE = "DSAKeyValue";

        String X509DATA = "X509Data";

        String X509CERT = "X509Certificate";

        String P = "P";
        String Q = "Q";
        String G = "G";
        String Y = "Y";
        String SEED = "Seed";
        String PGEN_COUNTER = "PgenCounter";
    }

    public interface XMLEnc {

        String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";

        String ENCRYPTED_KEY = "EncryptedKey";
    }

    public interface WSSE {

        String ID = "Id";

        String KEY_IDENTIFIER = "KeyIdentifier";

        String KEY_IDENTIFIER_VALUETYPE_SAML = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";

        String REFERENCE = "Reference";

        String PREFIX = "wsse";

        String PREFIX_11 = "wsse11";

        // http://www.ws-i.org/Profiles/KerberosTokenProfile-1.0.html#Kerberos_Security_Token_URI
        String KERBEROS = "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-kerberos-token-profile-1.1#GSS_Kerberosv5_AP_REQ";

        String SECURITY_TOKEN_REFERENCE = "SecurityTokenReference";

        String BINARY_SECURITY_TOKEN = "BinarySecurityToken";

        String USERNAME_TOKEN = "UsernameToken";

        String URI = "URI";

        String VALUE_TYPE = "ValueType";

        String ENCODING_TYPE = "EncodingType";
    }
}