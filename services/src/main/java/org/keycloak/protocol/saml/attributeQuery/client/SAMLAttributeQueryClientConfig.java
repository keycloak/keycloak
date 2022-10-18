/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.attributeQuery.client;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.attributeQuery.BaseSamlAttributeQueryConfig;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * An extended configuration to support attribute query client configuration.
 */
public class SAMLAttributeQueryClientConfig extends BaseSamlAttributeQueryConfig {
    // configuration values
    private String soapEndpoint;
    private boolean requireAssertionSignature;
    private String subjectFormat;
    private String issuer;

    /**
     * Get the SOAP endpoint to send the attribute query request to.
     * @return the soap endpoint
     */
    public String getSoapEndpoint() {
        return soapEndpoint;
    }

    /**
     * Set the SOAP endpoint to send the attribute query request to.
     * @param soapEndpoint the soap endpoint to set
     */
    public void setSoapEndpoint(String soapEndpoint) {
        this.soapEndpoint = soapEndpoint;
    }

    /**
     * Checks if the assertion received from the SOAP endpoint must have a signed assertion.
     * @return True if the assertion received from the SOAP endpoint must have a signed assertion.
     */
    public boolean isRequireAssertionSignature() {
        return requireAssertionSignature;
    }

    /**
     * Set if the assertion received from the SOAP endpoint must have a signed assertion.
     * @param requireAssertionSignature Whether to require signed assertions in the response from the SOAP endpoint
     */
    public void setRequireAssertionSignature(boolean requireAssertionSignature) {
        this.requireAssertionSignature = requireAssertionSignature;
    }

    /**
     * Get the name ID format to use in the SAML Attribute Query request
     * @return The name ID format
     */
    public String getSubjectFormat() {
        return subjectFormat;
    }

    /**
     * Set the name ID format to use in the SAML attribute query request
     * @param subjectFormat The name ID format
     */
    public void setSubjectFormat(String subjectFormat) {
        this.subjectFormat = subjectFormat;
    }

    /**
     * Get the issuer used for the SAML2 request.
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Set the issuer that is used for the SAML2 request.
     * @param issuer the issuer to set on the request
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }


    /**
     * Load the attribute query client config from the component model
     * @param model the component model representing the client config
     * @return the initialized client config
     */
    public static SAMLAttributeQueryClientConfig load(ComponentModel model){
        SAMLAttributeQueryClientConfig config = new SAMLAttributeQueryClientConfig();
        config.setSoapEndpoint(model.getConfig().getFirst(ProviderConfig.SOAP_ENDPOINT));
        config.setIdpSigningCert(model.getConfig().getFirst(ProviderConfig.IDP_SIGNING_CERT));
        config.setIdpEncryptionCert(model.getConfig().getFirst(ProviderConfig.IDP_ENCRYPTION_CERT));
        config.setClientId(model.getConfig().getFirst(ProviderConfig.ATTRIBUTE_LOOKUP_CLIENT));
        config.setExpectedIssuer(model.getConfig().getFirst(ProviderConfig.EXPECTED_ISSUER));
        config.setIssuer(model.getConfig().getFirst(ProviderConfig.ISSUER));

        config.setRequireAssertionSignature(Boolean.parseBoolean(model.getConfig().getFirst(ProviderConfig.REQUIRE_ASSERTION_SIGNATURE)));
        config.setRequireDocumentSignature(Boolean.parseBoolean(model.getConfig().getFirst(ProviderConfig.REQUIRE_DOCUMENT_SIGNATURE)));
        config.setRequireEncryption(Boolean.parseBoolean(model.getConfig().getFirst(ProviderConfig.REQUIRE_ENCRYPTED_ASSERTION)));

        config.setSignDocument(Boolean.parseBoolean(model.getConfig().getFirst(ProviderConfig.SIGN_DOCUMENT)));
        config.setEncryptSubject(Boolean.parseBoolean(model.getConfig().getFirst(ProviderConfig.ENCRYPT_SUBJECT)));

        config.setSubjectAttribute(model.getConfig().getFirst(ProviderConfig.SUBJECT_ATTRIBUTE));
        config.setSubjectFormat(model.getConfig().getFirst(ProviderConfig.SUBJECT_FORMAT));

        return config;
    }

    @Override
    public List<String> verify(KeycloakSession session){
        // run the base verification
        List<String> errors = super.verify(session);

        // ensure required attributes are set
        if (getSoapEndpoint() == null || getSoapEndpoint().isEmpty()){
            errors.add("soap endpoint cannot be empty");
        }
        if (getIssuer() == null || getIssuer().isEmpty()){
            errors.add("issuer cannot be empty");
        }
        if (getSubjectFormat() == null || getSubjectFormat().isEmpty()){
            errors.add("Subject format cannot be empty");
        }

        return errors;
    }

    /**
     * Generate the provider config for UI display
     * @return the provider config
     */
    public static List<ProviderConfigProperty> buildProviderConfig(){
        return ProviderConfigurationBuilder.create()
                .property().name("displayName")
                .label("Display Name")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The display name of this provider instance.")
                .required(true)
                .add()
                .property().name(ProviderConfig.SOAP_ENDPOINT)
                .label("SOAP Endpoint")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The SOAP endpoint of the external IdP. For the SAML attribute query provider, this will be the SAML attribute endpoint.")
                .required(true)
                .add()
                .property().name(ProviderConfig.IDP_SIGNING_CERT)
                .label("Response Signing Certificate")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .helpText("The certificate the authoritative attribute source uses to sign attribute query responses. This will be used to verify the signatures on the attribute query response document.")
                .add()
                .property().name(ProviderConfig.IDP_ENCRYPTION_CERT)
                .label("Request Encryption Certificate")
                .type(ProviderConfigProperty.TEXT_TYPE)
                .helpText("The certificate this provider will use to encrypt requests to the authoritative attribute source.")
                .add()
                .property().name(ProviderConfig.ATTRIBUTE_LOOKUP_CLIENT)
                .label("Attribute Lookup Client")
                .type(ProviderConfigProperty.CLIENT_LIST_TYPE)
                .helpText("The SAML client that should be used to perform the attribute lookup request. The signing and encryption certificates of this client will be used to sign requests to the authoritative attribute source and decrypt encrypted responses from the authoritative attribute source.")
                .required(true)
                .add()
                .property().name(ProviderConfig.ISSUER)
                .label("Response Issuer")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The URI of the authoritative attribute source. This will be used to verify the issuer value on the response received from the external IdP.")
                .required(true)
                .add()
                .property().name(ProviderConfig.EXPECTED_ISSUER)
                .label("Request Issuer")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The URI to set in SAML attribute query request to the authoritative attribute source.")
                .required(true)
                .add()
                .property().name(ProviderConfig.SUBJECT_ATTRIBUTE)
                .label("Request Subject Attribute")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The attribute on the user that should be used to populate the attribute query request subject. If no attribute is specified, the `username` of the user will be used.")
                .add()
                .property().name(ProviderConfig.SUBJECT_FORMAT)
                .label("Request Subject Format")
                .type(ProviderConfigProperty.STRING_TYPE)
                .helpText("The name ID format to set on the attribute query request.")
                .required(true)
                .defaultValue("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified")
                .add()
                .property().name(ProviderConfig.REQUIRE_ASSERTION_SIGNATURE)
                .label("Require Assertion Signature")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Require the assertions in the response document from the external IdP to be signed. If the assertions are not signed, the request will fail.")
                .defaultValue(false)
                .add()
                .property().name(ProviderConfig.REQUIRE_DOCUMENT_SIGNATURE)
                .label("Require Document Signature")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Require the document in the response from the external IdP to be signed. If the document is not signed, the request will fail.")
                .defaultValue(false)
                .add()
                .property().name(ProviderConfig.REQUIRE_ENCRYPTED_ASSERTION)
                .label("Require Encrypted Assertion")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Require the assertions in the response from the external IdP to be encrypted. If the assertions are not encrypted, the request will fail.")
                .defaultValue(false)
                .add()
                .property().name(ProviderConfig.SIGN_DOCUMENT)
                .label("Sign Document")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Sign the attribute query request sent to the external IdP.")
                .defaultValue(false)
                .add()
                .property().name(ProviderConfig.ENCRYPT_SUBJECT)
                .label("Encrypt Subject")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .helpText("Encrypt name ID in the attribute query request to the external IdP.")
                .defaultValue(false)
                .add()
                .build();
    }

    /**
     * A class for storing provider config constants
     */
    public static class ProviderConfig {
        public static final String SOAP_ENDPOINT = "soapEndpoint";
        public static final String IDP_SIGNING_CERT = "idpSigningCert";
        public static final String IDP_ENCRYPTION_CERT = "idpEncryptionCert";
        public static final String ISSUER = "issuer";
        public static final String EXPECTED_ISSUER = "expectedIssuer";
        public static final String SUBJECT_ATTRIBUTE = "subjectAttribute";
        public static final String SUBJECT_FORMAT = "subjectFormat";
        public static final String ATTRIBUTE_LOOKUP_CLIENT = "attributeLookupClient";

        // security settings
        public static final String REQUIRE_ASSERTION_SIGNATURE = "requireAssertionSignature";
        public static final String REQUIRE_DOCUMENT_SIGNATURE = "requireDocumentSignature";
        public static final String REQUIRE_ENCRYPTED_ASSERTION = "requireEncryptedAssertion";
        public static final String SIGN_DOCUMENT = "signDocument";
        public static final String ENCRYPT_SUBJECT = "encryptSubject";
    }
}
