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

package org.keycloak.adapters.saml.config.parsers;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConfigXmlConstants {
    public static final String KEYCLOAK_SAML_ADAPTER = "keycloak-saml-adapter";
    public static final String SP_ELEMENT = "SP";
    public static final String ENTITY_ID_ATTR = "entityID";
    public static final String SSL_POLICY_ATTR = "sslPolicy";
    public static final String NAME_ID_POLICY_FORMAT_ATTR = "nameIDPolicyFormat";
    public static final String FORCE_AUTHENTICATION_ATTR = "forceAuthentication";
    public static final String IS_PASSIVE_ATTR = "isPassive";
    public static final String TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN_ATTR = "turnOffChangeSessionIdOnLogin";
    public static final String SIGNATURE_ALGORITHM_ATTR = "signatureAlgorithm";
    public static final String SIGNATURE_CANONICALIZATION_METHOD_ATTR = "signatureCanonicalizationMethod";
    public static final String LOGOUT_PAGE_ATTR = "logoutPage";

    public static final String KEYS_ELEMENT = "Keys";
    public static final String KEY_ELEMENT = "Key";
    public static final String SIGNING_ATTR = "signing";
    public static final String ENCRYPTION_ATTR = "encryption";
    public static final String CERTIFICATE_PEM_ELEMENT = "CertificatePem";
    public static final String PRIVATE_KEY_PEM_ELEMENT = "PrivateKeyPem";
    public static final String PUBLIC_KEY_PEM_ELEMENT = "PublicKeyPem";
    public static final String FILE_ATTR = "file";
    public static final String TYPE_ATTR = "type";
    public static final String RESOURCE_ATTR = "resource";
    public static final String PASSWORD_ATTR = "password";
    public static final String ALIAS_ATTR = "alias";
    public static final String KEYS_STORE_ELEMENT = "KeyStore";
    public static final String CERTIFICATE_ELEMENT = "Certificate";
    public static final String PRIVATE_KEY_ELEMENT = "PrivateKey";

    public static final String PRINCIPAL_NAME_MAPPING_ELEMENT = "PrincipalNameMapping";
    public static final String POLICY_ATTR = "policy";
    public static final String ATTRIBUTE_ATTR = "attribute";

    public static final String ROLE_IDENTIFIERS_ELEMENT = "RoleIdentifiers";
    public static final String ATTRIBUTE_ELEMENT = "Attribute";
    public static final String NAME_ATTR = "name";

    public static final String IDP_ELEMENT = "IDP";
    public static final String SIGNATURES_REQUIRED_ATTR = "signaturesRequired";
    public static final String SINGLE_SIGN_ON_SERVICE_ELEMENT = "SingleSignOnService";
    public static final String SINGLE_LOGOUT_SERVICE_ELEMENT = "SingleLogoutService";
    public static final String SIGN_REQUEST_ATTR = "signRequest";
    public static final String SIGN_RESPONSE_ATTR = "signResponse";
    public static final String REQUEST_BINDING_ATTR = "requestBinding";
    public static final String RESPONSE_BINDING_ATTR = "responseBinding";
    public static final String BINDING_URL_ATTR = "bindingUrl";
    public static final String VALIDATE_RESPONSE_SIGNATURE_ATTR = "validateResponseSignature";
    public static final String VALIDATE_ASSERTION_SIGNATURE_ATTR = "validateAssertionSignature";
    public static final String VALIDATE_REQUEST_SIGNATURE_ATTR = "validateRequestSignature";
    public static final String POST_BINDING_URL_ATTR = "postBindingUrl";
    public static final String REDIRECT_BINDING_URL_ATTR = "redirectBindingUrl";
}
