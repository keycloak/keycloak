/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.attributeQuery;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlProtocolUtils;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Base configuration for the SAML attribute query requests and responses. These are common configuration shared by both
 * the client and server attribute query provider implementations.
 */
public class BaseSamlAttributeQueryConfig {
    // protocol config
    private String idpSigningCert;
    private String idpEncryptionCert;
    private String subjectAttribute;


    // verification config
    private String clientId;
    private String expectedIssuer;

    // security config
    private boolean requireDocumentSignature;
    private boolean requireEncryption;
    private boolean signDocument;
    private boolean signAssertion;
    private boolean encryptAssertion;
    private boolean encryptSubject;

    /**
     * Get if the assertion in the SAML2 response should be encrypted
     * @return true if the assertion should be encrypted
     */
    public boolean isEncryptAssertion() {
        return encryptAssertion;
    }

    /**
     * Set if the assertion in the SAML2 response should be encrypted
     * @param encryptAssertion true if the assertion should be encrypted
     */
    public void setEncryptAssertion(boolean encryptAssertion) {
        this.encryptAssertion = encryptAssertion;
    }

    /**
     * Get if the subject in the SAML2 request/response should be encrypted
     * @return true if the subject should be encrypted
     */
    public boolean isEncryptSubject() {
        return encryptSubject;
    }

    /**
     * Set if the subject in the SAML2 request/response should be encrypted
     * @param encryptSubject true if the subject should be encrypted
     */
    public void setEncryptSubject(boolean encryptSubject) {
        this.encryptSubject = encryptSubject;
    }

    /**
     * Get if the assertion in the SAML2 response should be signed
     * @return true if the assertion should be signed
     */
    public boolean isSignAssertion() {
        return signAssertion;
    }

    /**
     * Set if the assertion in the SAML2 response should be signed
     * @param signAssertion true if the assertion should be signed
     */
    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    /**
     * Get the attribute that should be used to map the subject in the SAML2 request/response to a user in keycloak. If
     * it is null, then the user will be looked up by username.
     * @return the attribute to use to lookup the user in keycloak
     */
    public String getSubjectAttribute() {
        return subjectAttribute;
    }

    /**
     * Set the attribute that should be used to map the subject in the SAML2 request/response to a user in keycloak. If
     * it is null, then the user will be lookup up by username.
     * @param subjectAttribute the attribute to use to lookup the user in keycloak
     */
    public void setSubjectAttribute(String subjectAttribute) {
        this.subjectAttribute = subjectAttribute;
    }

    /**
     * Get the certificate to use for verifying incoming SAML2 requests/responses.
     * @return The certificate to use for verifying incoming SAML2 requests/responses
     */
    public String getIdpSigningCert() {
        return idpSigningCert;
    }

    /**
     * Set the certificate to use for verify incoming SAML2 requests/responses.
     * @param idpSigningCert The certificate to use for verifying incoming SAML2 requests/responses
     */
    public void setIdpSigningCert(String idpSigningCert) {
        this.idpSigningCert = idpSigningCert;
    }

    /**
     * Get the certificate used for encrypting outgoing SAML2 requests/responses.
     * @return The certificate to use for encrypting outgoing SAML2 requests/responses
     */
    public String getIdpEncryptionCert() {
        return idpEncryptionCert;
    }

    /**
     * Set the certificate to use for encrypting outgoing SAML2 requests/responses
     * @param idpEncryptionCert The certificate to use for encrypting outgoing SAML2 requests/responses
     */
    public void setIdpEncryptionCert(String idpEncryptionCert) {
        this.idpEncryptionCert = idpEncryptionCert;
    }

    /**
     * Get the client ID of the client configured to handle the SAML2 request/response
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Set the client to handle SAML2 requests/responses
     * @param clientId the client ID to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Get the issuer value that is expected on the SAML2 request/response. This is used to verify the request/response.
     * @return the expected issuer
     */
    public String getExpectedIssuer() {
        return expectedIssuer;
    }

    /**
     * Set the issuer that is expected on the SAML2 request/response. This is used to verify the request/response.
     * @param expectedIssuer the expected issuer to set
     */
    public void setExpectedIssuer(String expectedIssuer) {
        this.expectedIssuer = expectedIssuer;
    }

    /**
     * Get if the received SAML2 document is required to be signed
     * @return true if the received SAML2 document is required to be signed
     */
    public boolean isRequireDocumentSignature() {
        return requireDocumentSignature;
    }

    /**
     * Set if the received SAML2 document is required to be signed
     * @param requireDocumentSignature true if the received SAML2 document is required to be signed
     */
    public void setRequireDocumentSignature(boolean requireDocumentSignature) {
        this.requireDocumentSignature = requireDocumentSignature;
    }

    /**
     * Get if the received SAML2 document is required to be encrypted
     * @return true if the received SAML2 document is required to be encryption
     */
    public boolean isRequireEncryption() {
        return requireEncryption;
    }

    /**
     * Set if the received SAML2 document is required to be encrypted
     * @param requireEncryption true if the received SAML2 document is required to be encrypted
     */
    public void setRequireEncryption(boolean requireEncryption) {
        this.requireEncryption = requireEncryption;
    }

    /**
     * Get if the SAML2 request/response document should be signed
     * @return true if the outgoing SAML2 document should be signed
     */
    public boolean isSignDocument() {
        return signDocument;
    }

    /**
     * Set if the SAML2 document should be signed
     * @param signDocument true if the SAML2 document should be signed
     */
    public void setSignDocument(boolean signDocument) {
        this.signDocument = signDocument;
    }

    /**
     * Verify the configuration is valid
     * @param session The keycloak session
     * @throws ComponentValidationException Thrown if the configuration is invalid
     */
    public List<String> verify(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();

        List<String> errors = new ArrayList<>();

        // ensure document verification configs are set
        if (getExpectedIssuer() == null || getExpectedIssuer().isEmpty()){
            errors.add("expected issuer cannot be empty");
        }

        // ensure client exists (fetch by client ID or ID)
        ClientModel client = realm.getClientByClientId(getClientId());
        if (client == null){
            client = realm.getClientById(getClientId());
        }
        if (client == null){
            errors.add(String.format("Client %s does not exist", getClientId()));
        }

        // ensure idp cert is valid if set
        PublicKey idpSigningCert = null;
        if (getIdpSigningCert() != null && !getIdpSigningCert().isEmpty()) {
            try {
                idpSigningCert = SamlProtocolUtils.getPublicKey(getIdpSigningCert());
            } catch (VerificationException ex) {
                errors.add("IDP signing cert is invalid");
            }
        }

        // ensure signing cert is set when signatures are required
        if((isRequireDocumentSignature()) && idpSigningCert == null) {
            errors.add("IDP signing cert must be set if configured to require signatures");
        }
        if(isSignAssertion() && idpSigningCert == null){
            errors.add("IDP signing cert must be set when assertion signing is configured");
        }

        // ensure encrypted key is valid if set
        X509Certificate idpEncryptionCert = null;
        if (getIdpEncryptionCert() != null && !getIdpEncryptionCert().isEmpty()){
            try {
                idpEncryptionCert = PemUtils.decodeCertificate(getIdpEncryptionCert());
            } catch (PemException ex) {
                errors.add("IDP encryption cert is invalid");
            }
        }

        // ensure encryption cert is set when encryption is required
        if((isEncryptSubject() || isEncryptAssertion()) && idpEncryptionCert == null){
            errors.add("IDP encryption cert must be set if configure to encryption");
        }

        return errors;
    }
}
