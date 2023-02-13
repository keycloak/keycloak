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

import java.nio.charset.Charset;

/**
 * Constants
 *
 * @author Anil.Saldhana@redhat.com
 * @since Oct 8, 2009
 */
public interface GeneralConstants {
    String ASSERTIONS_VALIDITY = "ASSERTIONS_VALIDITY";
    String CLOCK_SKEW = "CLOCK_SKEW";

    String ASSERTION_ID = "ASSERTION_ID";

    String ASSERTION = "ASSERTION";

    String ATTRIBUTES = "ATTRIBUTES";

    String ATTRIBUTE_KEYS = "ATTRIBUTE_KEYS";

    String ATTRIBUTE_CHOOSE_FRIENDLY_NAME = "ATTRIBUTE_CHOOSE_FRIENDLY_NAME";

    String ATTIBUTE_MANAGER = "ATTRIBUTE_MANAGER";

    String AUDIT_ENABLE = "picketlink.audit.enable";

    String AUDIT_HELPER = "AUDIT_HELPER";

    String AUDIT_SECURITY_DOMAIN = "picketlink.audit.securitydomain";

    String CONFIGURATION = "CONFIGURATION";

    String CONFIG_FILE_LOCATION = "/WEB-INF/picketlink.xml";

    String CONFIG_PROVIDER = "CONFIG_PROVIDER";

    String CONTEXT_PATH = "CONTEXT_PATH";

    String DEPRECATED_CONFIG_FILE_LOCATION = "/WEB-INF/picketlink-idfed.xml";

    String LOCAL_LOGOUT = "LLO";

    String GLOBAL_LOGOUT = "GLO";

    String HANDLER_CONFIG_FILE_LOCATION = "/WEB-INF/picketlink-handlers.xml";

    String IDENTITY_SERVER = "IDENTITY_SERVER";

    String IDENTITY_PARTICIPANT_STACK = "IDENTITY_PARTICIPANT_STACK";

    String IGNORE_SIGNATURES = "IGNORE_SIGNATURES";

    String KEYPAIR = "KEYPAIR";

    String LOGIN_TYPE = "LOGIN_TYPE";

    String LOGOUT_PAGE = "LOGOUT_PAGE";

    String LOGOUT_PAGE_NAME = "/logout.jsp";

    String NAMEID_FORMAT = "NAMEID_FORMAT";

    String PRINCIPAL_ID = "picketlink.principal";

    String RELAY_STATE = "RelayState";

    String ROLES = "ROLES";

    String ROLES_ID = "picketlink.roles";

    String ROLE_GENERATOR = "ROLE_GENERATOR";

    String ROLE_VALIDATOR = "ROLE_VALIDATOR";

    String ROLE_VALIDATOR_IGNORE = "ROLE_VALIDATOR_IGNORE";

    String URL = "url";

    String SAML_REQUEST_KEY = "SAMLRequest";

    String SAML_RESPONSE_KEY = "SAMLResponse";

    String SAML_ARTIFACT_KEY = "SAMLart";

    String SAML_SIG_ALG_REQUEST_KEY = "SigAlg";

    String SAML_SIGNATURE_REQUEST_KEY = "Signature";

    String SAML_IDP_STRICT_POST_BINDING = "SAML_IDP_STRICT_POST_BINDING";

    // Should JAXP Factory operations cache the TCCL and revert after operation?
    String TCCL_JAXP = "picketlink.jaxp.tccl";

    String TIMEZONE = "picketlink.timezone";

    String TIMEZONE_DEFAULT = "TIMEZONE_DEFAULT";

    String DECRYPTING_KEY = "DECRYPTING_KEY";

    String SP_SSO_METADATA_DESCRIPTOR = "SP_SSO_METADATA_DESCRIPTOR";

    String IDP_SSO_METADATA_DESCRIPTOR = "IDP_SSO_METADATA_DESCRIPTOR";

    String SSO_METADATA_DESCRIPTOR = "SSO_METADATA_DESCRIPTOR";

    String SENDER_PUBLIC_KEY = "SENDER_PUBLIC_KEY";

    String SIGN_OUTGOING_MESSAGES = "SIGN_OUTGOING_MESSAGES";

    String SUPPORTS_SIGNATURES = "SUPPORTS_SIGNATURES";

    String SESSION_ATTRIBUTE_MAP = "SESSION_ATTRIBUTE_MAP";

    String USERNAME_FIELD = "JBID_USERNAME";

    String PASS_FIELD = "JBID_PASSWORD";

    String AUTH_REQUEST_ID = "AUTH_REQUEST_ID";
    String ERROR_PAGE_NAME = "/error.jsp";
    String SAML_ENC_KEY_SIZE = "SAML_ENC_KEY_SIZE";
    String SAML_ENC_ALGORITHM = "SAML_ENC_ALGORITHM";

    /**
     * <p>{@link SAML2AuthenticationHandler} configuration option to set the assertion into the {@link
     * HttpSession}.</p>
     */
    String ASSERTION_SESSION_ATTRIBUTE_NAME = "ASSERTION_SESSION_ATTRIBUTE_NAME";

    String X509CERTIFICATE = "X509CERTIFICATE";

    String AUTHN_CONTEXT_CLASSES = "AUTHN_CONTEXT_CLASSES";
    String REQUESTED_AUTHN_CONTEXT_COMPARISON = "REQUESTED_AUTHN_CONTEXT_COMPARISON";

    String BASE64_ENCODE_WSTRUST_SECRET_KEY = "picketlink.wstrust.base64_encode_wstrust_secret_key";

    String HTTP_HEADER_X_REQUESTED_WITH = "X-Requested-With";

    public static final String  SAML_CHARSET_NAME = System.getProperty("keycloak.saml.saml_message_charset", "UTF-8");
    public static final Charset SAML_CHARSET = Charset.forName(SAML_CHARSET_NAME);
}
