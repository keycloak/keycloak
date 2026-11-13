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
import javax.xml.crypto.dsig.CanonicalizationMethod;

import jakarta.validation.constraints.Size;

import org.keycloak.representations.admin.v2.validation.ValidCanonicalizationMethod;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Representation of a SAML client.
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
@Schema(description = "SAML Client configuration")
public class SAMLClientRepresentation extends BaseClientRepresentation {
    public static final String PROTOCOL = "saml";

    // Defaults for unset fields, shared by the @Schema documentation and the server side logic that applies them.
    // Booleans are strings because annotation values must be constants.
    public static final String DEFAULT_FORCE_NAME_ID_FORMAT = "false";
    public static final String DEFAULT_INCLUDE_AUTHN_STATEMENT = "true";
    public static final String DEFAULT_SIGN_DOCUMENTS = "true";
    public static final String DEFAULT_SIGN_ASSERTIONS = "false";
    public static final String DEFAULT_CLIENT_SIGNATURE_REQUIRED = "true";
    public static final String DEFAULT_FORCE_POST_BINDING = "true";
    public static final String DEFAULT_FRONT_CHANNEL_LOGOUT = "true";
    public static final String DEFAULT_ALLOW_ECP_FLOW = "false";
    public static final String DEFAULT_SIGNATURE_ALGORITHM = "RSA_SHA256";
    public static final String DEFAULT_SIGNATURE_CANONICALIZATION_METHOD = CanonicalizationMethod.EXCLUSIVE;

    public SAMLClientRepresentation() {
        this.protocol = PROTOCOL;
    }

    public enum NameIdFormat {
        USERNAME, EMAIL, PERSISTENT, TRANSIENT;

        @JsonValue
        public String toJson() {
            return name().toLowerCase();
        }

        @JsonCreator
        public static NameIdFormat fromJson(String value) {
            return value == null ? null : valueOf(value.toUpperCase());
        }
    }

    public enum SignatureAlgorithm {
        RSA_SHA1, RSA_SHA256, RSA_SHA256_MGF1, RSA_SHA512, RSA_SHA512_MGF1, DSA_SHA1;

        @JsonCreator
        public static SignatureAlgorithm fromJson(String value) {
            return value == null ? null : valueOf(value);
        }
    }

    @JsonPropertyDescription("Name ID format to use for the subject. Set to username when a client is created without a value. "
            + "A client without a stored format, such as one created before this setting was always populated, "
            + "uses the format requested by the SAML client and is left unchanged unless a value is provided.")
    private NameIdFormat nameIdFormat;

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

    @JsonPropertyDescription("Signature algorithm for signing SAML documents")
    private SignatureAlgorithm signatureAlgorithm;

    @ValidCanonicalizationMethod
    @JsonPropertyDescription("Canonicalization method for XML signatures")
    private String signatureCanonicalizationMethod;

    @Size(max = 65536)
    @JsonPropertyDescription("X.509 certificate for signing (PEM format, without headers)")
    private String signingCertificate;

    @JsonPropertyDescription("Allow ECP (Enhanced Client or Proxy) flow")
    private Boolean allowEcpFlow;

    public NameIdFormat getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(NameIdFormat nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    @Schema(defaultValue = DEFAULT_FORCE_NAME_ID_FORMAT)
    public Boolean getForceNameIdFormat() {
        return forceNameIdFormat;
    }

    public void setForceNameIdFormat(Boolean forceNameIdFormat) {
        this.forceNameIdFormat = forceNameIdFormat;
    }

    @Schema(defaultValue = DEFAULT_INCLUDE_AUTHN_STATEMENT)
    public Boolean getIncludeAuthnStatement() {
        return includeAuthnStatement;
    }

    public void setIncludeAuthnStatement(Boolean includeAuthnStatement) {
        this.includeAuthnStatement = includeAuthnStatement;
    }

    @Schema(defaultValue = DEFAULT_SIGN_DOCUMENTS)
    public Boolean getSignDocuments() {
        return signDocuments;
    }

    public void setSignDocuments(Boolean signDocuments) {
        this.signDocuments = signDocuments;
    }

    @Schema(defaultValue = DEFAULT_SIGN_ASSERTIONS)
    public Boolean getSignAssertions() {
        return signAssertions;
    }

    public void setSignAssertions(Boolean signAssertions) {
        this.signAssertions = signAssertions;
    }

    @Schema(defaultValue = DEFAULT_CLIENT_SIGNATURE_REQUIRED)
    public Boolean getClientSignatureRequired() {
        return clientSignatureRequired;
    }

    public void setClientSignatureRequired(Boolean clientSignatureRequired) {
        this.clientSignatureRequired = clientSignatureRequired;
    }

    @Schema(defaultValue = DEFAULT_FORCE_POST_BINDING)
    public Boolean getForcePostBinding() {
        return forcePostBinding;
    }

    public void setForcePostBinding(Boolean forcePostBinding) {
        this.forcePostBinding = forcePostBinding;
    }

    @Schema(defaultValue = DEFAULT_FRONT_CHANNEL_LOGOUT)
    public Boolean getFrontChannelLogout() {
        return frontChannelLogout;
    }

    public void setFrontChannelLogout(Boolean frontChannelLogout) {
        this.frontChannelLogout = frontChannelLogout;
    }

    @Schema(defaultValue = DEFAULT_SIGNATURE_ALGORITHM)
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Schema(defaultValue = DEFAULT_SIGNATURE_CANONICALIZATION_METHOD)
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

    @Schema(defaultValue = DEFAULT_ALLOW_ECP_FLOW)
    public Boolean getAllowEcpFlow() {
        return allowEcpFlow;
    }

    public void setAllowEcpFlow(Boolean allowEcpFlow) {
        this.allowEcpFlow = allowEcpFlow;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SAMLClientRepresentation)) return false;
        if (!super.equals(o)) return false;
        SAMLClientRepresentation that = (SAMLClientRepresentation)o;
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
