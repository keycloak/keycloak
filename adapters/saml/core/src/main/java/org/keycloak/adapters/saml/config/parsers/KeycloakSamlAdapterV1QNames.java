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
package org.keycloak.adapters.saml.config.parsers;

import org.keycloak.saml.processing.core.parsers.util.HasQName;
import javax.xml.namespace.QName;

/**
 *
 * @author hmlnarik
 */
public enum KeycloakSamlAdapterV1QNames implements HasQName {

    ALLOWED_CLOCK_SKEW("AllowedClockSkew"),
    ATTRIBUTE("Attribute"),
    CERTIFICATE("Certificate"),
    CERTIFICATE_PEM("CertificatePem"),
    HTTP_CLIENT("HttpClient"),
    IDP("IDP"),
    KEY("Key"),
    KEYCLOAK_SAML_ADAPTER("keycloak-saml-adapter"),
    KEYS("Keys"),
    KEY_STORE("KeyStore"),
    PRINCIPAL_NAME_MAPPING("PrincipalNameMapping"),
    PRIVATE_KEY("PrivateKey"),
    PRIVATE_KEY_PEM("PrivateKeyPem"),
    PROPERTY("Property"),
    PUBLIC_KEY_PEM("PublicKeyPem"),
    ROLE_IDENTIFIERS("RoleIdentifiers"),
    ROLE_MAPPINGS_PROVIDER("RoleMappingsProvider"),
    SINGLE_LOGOUT_SERVICE("SingleLogoutService"),
    SINGLE_SIGN_ON_SERVICE("SingleSignOnService"),
    SP("SP"),

    ATTR_ALIAS(null, "alias"),
    ATTR_ALLOW_ANY_HOSTNAME(null, "allowAnyHostname"),
    ATTR_ASSERTION_CONSUMER_SERVICE_URL(null, "assertionConsumerServiceUrl"),
    ATTR_ATTRIBUTE(null, "attribute"),
    ATTR_AUTODETECT_BEARER_ONLY(null, "autodetectBearerOnly"),
    ATTR_BINDING_URL(null, "bindingUrl"),
    ATTR_CLIENT_KEYSTORE(null, "clientKeystore"),
    ATTR_CLIENT_KEYSTORE_PASSWORD(null, "clientKeystorePassword"),
    ATTR_CONNECTION_POOL_SIZE(null, "connectionPoolSize"),
    ATTR_DISABLE_TRUST_MANAGER(null, "disableTrustManager"),
    ATTR_ENCRYPTION(null, "encryption"),
    ATTR_ENTITY_ID(null, "entityID"),
    ATTR_FILE(null, "file"),
    ATTR_FORCE_AUTHENTICATION(null, "forceAuthentication"),
    ATTR_ID(null, "id"),
    ATTR_IS_PASSIVE(null, "isPassive"),
    ATTR_LOGOUT_PAGE(null, "logoutPage"),
    ATTR_METADATA_URL(null, "metadataUrl"),
    ATTR_NAME(null, "name"),
    ATTR_NAME_ID_POLICY_FORMAT(null, "nameIDPolicyFormat"),
    ATTR_PASSWORD(null, "password"),
    ATTR_POLICY(null, "policy"),
    ATTR_POST_BINDING_URL(null, "postBindingUrl"),
    ATTR_PROXY_URL(null, "proxyUrl"),
    ATTR_REDIRECT_BINDING_URL(null, "redirectBindingUrl"),
    ATTR_REQUEST_BINDING(null, "requestBinding"),
    ATTR_RESOURCE(null, "resource"),
    ATTR_RESPONSE_BINDING(null, "responseBinding"),
    ATTR_SIGNATURES_REQUIRED(null, "signaturesRequired"),
    ATTR_SIGNATURE_ALGORITHM(null, "signatureAlgorithm"),
    ATTR_SIGNATURE_CANONICALIZATION_METHOD(null, "signatureCanonicalizationMethod"),
    ATTR_SIGNING(null, "signing"),
    ATTR_SIGN_REQUEST(null, "signRequest"),
    ATTR_SIGN_RESPONSE(null, "signResponse"),
    ATTR_SSL_POLICY(null, "sslPolicy"),
    ATTR_TRUSTSTORE(null, "truststore"),
    ATTR_TRUSTSTORE_PASSWORD(null, "truststorePassword"),
    ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN(null, "turnOffChangeSessionIdOnLogin"),
    ATTR_TYPE(null, "type"),
    ATTR_UNIT(null, "unit"),
    ATTR_VALIDATE_ASSERTION_SIGNATURE(null, "validateAssertionSignature"),
    ATTR_VALIDATE_REQUEST_SIGNATURE(null, "validateRequestSignature"),
    ATTR_VALIDATE_RESPONSE_SIGNATURE(null, "validateResponseSignature"),
    ATTR_VALUE(null, "value"),
    ATTR_KEEP_DOM_ASSERTION(null, "keepDOMAssertion"),
    ATTR_SOCKET_TIMEOUT(null, "socketTimeout"),
    ATTR_CONNECTION_TIMEOUT(null, "connectionTimeout"),
    ATTR_CONNECTION_TTL(null, "connectionTtl"),

    UNKNOWN_ELEMENT("");

    public static final String NS_URI = "urn:keycloak:saml:adapter";

    private final QName qName;

    private KeycloakSamlAdapterV1QNames(String localName) {
        this(NS_URI, localName);
    }

    private KeycloakSamlAdapterV1QNames(HasQName source) {
        this.qName = source.getQName();
    }

    private KeycloakSamlAdapterV1QNames(String nsUri, String localName) {
        this.qName = new QName(nsUri == null ? null : nsUri, localName);
    }

    @Override
    public QName getQName() {
        return qName;
    }

    public QName getQName(String prefix) {
        return new QName(this.qName.getNamespaceURI(), this.qName.getLocalPart(), prefix);
    }
}
