/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.representations.admin.v2;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representation of a SAML client.
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Schema(description = "SAML Client configuration")
public class SAMLClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "saml";

    @JsonPropertyDescription("Name ID format to use for the subject (e.g., 'username', 'email', 'transient', 'persistent')")
    private String nameIdFormat;

    @JsonPropertyDescription("Force the specified Name ID format even if the client requests a different one")
    private Boolean forceNameIdFormat;

    @JsonPropertyDescription("Include AuthnStatement in the SAML response")
    private Boolean includeAuthnStatement;

    @JsonPropertyDescription("Sign SAML documents on the server side")
    private Boolean signDocuments;

    @JsonPropertyDescription("Sign SAML assertions")
    private Boolean signAssertions;

    @JsonPropertyDescription("Require client to sign SAML requests")
    private Boolean clientSignatureRequired;

    @JsonPropertyDescription("Force POST binding for SAML responses")
    private Boolean forcePostBinding;

    @JsonPropertyDescription("Use front-channel logout (browser redirect)")
    private Boolean frontChannelLogout;

    @JsonPropertyDescription("Signature algorithm for signing SAML documents (e.g., 'RSA_SHA256', 'RSA_SHA512')")
    private String signatureAlgorithm;

    @JsonPropertyDescription("Canonicalization method for XML signatures")
    private String signatureCanonicalizationMethod;

    @JsonPropertyDescription("X.509 certificate for signing (PEM format, without headers)")
    private String signingCertificate;

    @JsonPropertyDescription("Allow ECP (Enhanced Client or Proxy) flow")
    private Boolean allowEcpFlow;

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    public Boolean getForceNameIdFormat() {
        return forceNameIdFormat;
    }

    public void setForceNameIdFormat(Boolean forceNameIdFormat) {
        this.forceNameIdFormat = forceNameIdFormat;
    }

    public Boolean getIncludeAuthnStatement() {
        return includeAuthnStatement;
    }

    public void setIncludeAuthnStatement(Boolean includeAuthnStatement) {
        this.includeAuthnStatement = includeAuthnStatement;
    }

    public Boolean getSignDocuments() {
        return signDocuments;
    }

    public void setSignDocuments(Boolean signDocuments) {
        this.signDocuments = signDocuments;
    }

    public Boolean getSignAssertions() {
        return signAssertions;
    }

    public void setSignAssertions(Boolean signAssertions) {
        this.signAssertions = signAssertions;
    }

    public Boolean getClientSignatureRequired() {
        return clientSignatureRequired;
    }

    public void setClientSignatureRequired(Boolean clientSignatureRequired) {
        this.clientSignatureRequired = clientSignatureRequired;
    }

    public Boolean getForcePostBinding() {
        return forcePostBinding;
    }

    public void setForcePostBinding(Boolean forcePostBinding) {
        this.forcePostBinding = forcePostBinding;
    }

    public Boolean getFrontChannelLogout() {
        return frontChannelLogout;
    }

    public void setFrontChannelLogout(Boolean frontChannelLogout) {
        this.frontChannelLogout = frontChannelLogout;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureCanonicalizationMethod() {
        return signatureCanonicalizationMethod;
    }

    public void setSignatureCanonicalizationMethod(String signatureCanonicalizationMethod) {
        this.signatureCanonicalizationMethod = signatureCanonicalizationMethod;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public Boolean getAllowEcpFlow() {
        return allowEcpFlow;
    }

    public void setAllowEcpFlow(Boolean allowEcpFlow) {
        this.allowEcpFlow = allowEcpFlow;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SAMLClientRepresentation that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(nameIdFormat, that.nameIdFormat) 
                && Objects.equals(forceNameIdFormat, that.forceNameIdFormat) 
                && Objects.equals(includeAuthnStatement, that.includeAuthnStatement) 
                && Objects.equals(signDocuments, that.signDocuments) 
                && Objects.equals(signAssertions, that.signAssertions) 
                && Objects.equals(clientSignatureRequired, that.clientSignatureRequired) 
                && Objects.equals(forcePostBinding, that.forcePostBinding) 
                && Objects.equals(frontChannelLogout, that.frontChannelLogout) 
                && Objects.equals(signatureAlgorithm, that.signatureAlgorithm) 
                && Objects.equals(signatureCanonicalizationMethod, that.signatureCanonicalizationMethod) 
                && Objects.equals(signingCertificate, that.signingCertificate) 
                && Objects.equals(allowEcpFlow, that.allowEcpFlow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nameIdFormat, forceNameIdFormat, includeAuthnStatement, 
                signDocuments, signAssertions, clientSignatureRequired, forcePostBinding, 
                frontChannelLogout, signatureAlgorithm, signatureCanonicalizationMethod, 
                signingCertificate, allowEcpFlow);
    }
}
