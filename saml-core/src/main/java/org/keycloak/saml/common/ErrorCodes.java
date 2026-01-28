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
package org.keycloak.saml.common;

/**
 * Error Codes for PicketLink https://docs.jboss.org/author/display/PLINK/PicketLink+Error+Codes
 *
 * @author Anil.Saldhana@redhat.com
 * @since Aug 4, 2011
 */
public interface ErrorCodes {

    String ASSERTION_RENEWAL_EXCEPTION = "PL00103:Assertion Renewal Exception:";

    String AUDIT_MANAGER_NULL = "PL00028: Audit Manager Is Not Set";

    String AUTHN_REQUEST_ID_VERIFICATION_FAILED = "PL00104:Authn Request ID verification failed:";

    String CLASS_NOT_LOADED = "PL00085: Class Not Loaded:";

    String CANNOT_CREATE_INSTANCE = "PL00086: Cannot create instance of:";

    String DOM_MISSING_DOC_ELEMENT = "PL00098: Missing Document Element:";

    String DOM_MISSING_ELEMENT = "PL00099: Missing Element:";

    String EXPIRED_ASSERTION = "PL00079: Assertion has expired:";

    String EXPECTED_XSI = "PL00072: Parser: Expected xsi:type";

    String EXPECTED_TAG = "PL00066: Parser : Expected start tag:";

    String EXPECTED_NAMESPACE = "PL00107: Parser : Expected start element namespace:";

    String EXPECTED_TEXT_VALUE = "PL00071: Parser: Expected text value:";

    String EXPECTED_END_TAG = "PL00066: Parser : Expected end tag:";

    String FAILED_PARSING = "PL00067: Parsing has failed:";

    String FILE_NOT_LOCATED = "PL00075: File could not be located :";

    String IDP_AUTH_FAILED = "PL00015: IDP Authentication Failed:";

    String IDP_WEBBROWSER_VALVE_CONF_FILE_MISSING = "PL00017: Configuration File missing:";

    String INVALID_ASSERTION = "PL00080: Invalid Assertion:";

    String INVALID_DIGITAL_SIGNATURE = "PL00009: Invalid Digital Signature:";

    String INJECTED_VALUE_MISSING = "PL00077: Injected Value Missing:";

    String ISSUER_INFO_MISSING_STATUS_CODE = "PL00085: IssuerInfo missing status code :";

    String KEYSTOREKEYMGR_DOMAIN_ALIAS_MISSING = "PL00058: KeyStoreKeyManager : Domain Alias missing for :";

    String KEYSTOREKEYMGR_NULL_ALIAS = "PL00059: KeyStoreKeyManager : Alias is null";

    String KEYSTOREKEYMGR_NULL_KEYSTORE = "PL00055: KeyStoreKeyManager : KeyStore is null";

    String KEYSTOREKEYMGR_NULL_SIGNING_KEYPASS = "PL00057: KeyStoreKeyManager :: Signing Key Pass is null";

    String KEYSTOREKEYMGR_NULL_ENCRYPTION_KEYPASS = "PL00189: KeyStoreKeyManager :: Encryption Key Pass is null";

    String KEYSTOREKEYMGR_KEYSTORE_NOT_LOCATED = "PL00056: KeyStoreKeyManager: Keystore not located:";

    String NOT_EQUAL = "PL00094: Not equal:";

    String NOT_IMPLEMENTED_YET = "PL00082: Not Implemented Yet: ";

    String NOT_SERIALIZABLE = "PL00093: Not Serializable:";

    String NULL_ARGUMENT = "PL00078: Null Parameter:";

    String NULL_ISSUE_INSTANT = "PL00088: Null IssueInstant";

    String NULL_START_ELEMENT = "PL00068: Parser : Start Element is null";

    String NULL_VALUE = "PL00092: Null Value:";

    String OPTION_NOT_SET = "PL00076: Option not set:";

    String PARSING_ERROR = "PL00074: Parsing Error:";

    String PRINCIPAL_NOT_FOUND = "PL00022: Principal Not Found";

    String PROCESSING_EXCEPTION = "PL00102: Processing Exception:";

    String REQD_ATTRIBUTE = "PL00063: Parser: Required attribute missing: ";

    String RESOURCE_NOT_FOUND = "PL00018: Resource not found:";

    String SAML2STSLM_CONF_FILE_MISSING = "PL00039: SAML2STSLoginModule: Failed to validate assertion: STS configuration file not specified";

    String SERVICE_PROVIDER_NOT_CATALINA_RESPONSE = "PL00026: Response was not of type catalina response";

    String SERVICE_PROVIDER_SERVER_EXCEPTION = "PL00032: Service Provider :: Server Exception";

    String SHOULD_NOT_BE_THE_SAME = "PL00016: Should not be the same:";

    String SIGNING_PROCESS_FAILURE = "PL00100: Signing Process Failure:";

    String STS_CLIENT_PUBLIC_KEY_ERROR = "PL00008: Unable to locate client public key";

    String STS_CONFIGURATION_FILE_PARSING_ERROR = "PL00005: Error parsing the configuration file:";

    String STS_CONFIGURATION_EXCEPTION = "PL00002: Encountered configuration exception:";

    String STS_COMBINED_SECRET_KEY_ERROR = "PL00006: Error generating combined secret key:";

    String STS_EXCEPTION_HANDLING_TOKEN_REQ = "PL00003: Exception in handling token request: ";

    String STS_NO_TOKEN_PROVIDER = "PL00013: No Security Token Provider found in configuration:[";

    String STS_INVALID_TOKEN_REQUEST = "PL00001: Invalid security token request";

    String STS_INVALID_REQUEST_TYPE = "PL00001: Invalid request type: ";

    String STS_PUBLIC_KEY_ERROR = "PL00010: Error obtaining public key for service: ";

    String STS_PUBLIC_KEY_CERT = "PL00012: Error obtaining public key certificate:";

    String STS_RESPONSE_WRITING_ERROR = "PL00004: Error writing response: ";

    String STS_SIGNING_KEYPAIR_ERROR = "PL00011: Error obtaining signing key pair:";

    String STS_UNABLE_TO_CONSTRUCT_KEYMGR = "PL00007: Unable to construct the key manager:";

    String SYSTEM_PROPERTY_MISSING = "PL00087: System Property missing:";

    String TRUST_MANAGER_MISSING = "PL000023: Trust Key Manager Missing";

    String UNABLE_PARSING_NULL_TOKEN = "PL00073: Parser: Unable to parse token request: security token is null";

    String UNABLE_LOCAL_AUTH = "PL00035: Unable to fallback on local auth:";

    String UNKNOWN_END_ELEMENT = "PL00061: Parser: Unknown End Element:";

    String UNKNOWN_OBJECT_TYPE = "PL00089: Unknown Object Type:";

    String UNKNOWN_START_ELEMENT = "PL00064: Parser: Unknown Start Element: ";

    String UNKNOWN_SIG_ALGO = "PL00090: Unknown Signature Algorithm:";

    String UNKNOWN_ENC_ALGO = "PL00097: Unknown Encryption Algorithm:";

    String UNKNOWN_TAG = "PL00062: Parser : Unknown tag:";

    String UNKNOWN_XSI = "PL0065: Parser : Unknown xsi:type=";

    String UNSUPPORTED_TYPE = "PL00069: Parser: Type not supported:";

    String VALIDATION_CHECK_FAILED = "PL00019: Validation check failed";

    String WRITER_INVALID_KEYINFO_NULL_CONTENT = "PL00091: Writer: Invalid KeyInfo object: content cannot be empty";

    String WRITER_NULL_VALUE = "PL00083: Writer: Null Value:";

    String WRITER_SHOULD_START_ELEMENT = "PL00096: Writer: Should have been a StartElement";

    String WRITER_UNKNOWN_TYPE = "PL00081: Writer: Unknown Type:";

    String WRITER_UNSUPPORTED_ATTRIB_VALUE = "PL00084: Writer: Unsupported Attribute Value:";

    String WRONG_TYPE = "PL00095: Wrong type:";

}