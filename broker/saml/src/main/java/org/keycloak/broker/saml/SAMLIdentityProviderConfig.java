/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml;

import org.keycloak.models.IdentityProviderModel;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderConfig extends IdentityProviderModel {

    public SAMLIdentityProviderConfig() {
    }

    public SAMLIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get("singleSignOnServiceUrl");
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put("singleSignOnServiceUrl", singleSignOnServiceUrl);
    }

    public String getSingleLogoutServiceUrl() {
        return getConfig().get("singleLogoutServiceUrl");
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put("singleLogoutServiceUrl", singleLogoutServiceUrl);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get("validateSignature"));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put("validateSignature", String.valueOf(validateSignature));
    }

    public boolean isForceAuthn() {
        return Boolean.valueOf(getConfig().get("forceAuthn"));
    }

    public void setForceAuthn(boolean forceAuthn) {
        getConfig().put("forceAuthn", String.valueOf(forceAuthn));
    }

    public String getSigningCertificate() {
        return getConfig().get("signingCertificate");
    }

    public void setSigningCertificate(String signingCertificate) {
        getConfig().put("signingCertificate", signingCertificate);
    }

    public String getNameIDPolicyFormat() {
        return getConfig().get("nameIDPolicyFormat");
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        getConfig().put("nameIDPolicyFormat", nameIDPolicyFormat);
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get("wantAuthnRequestsSigned"));
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        getConfig().put("wantAuthnRequestsSigned", String.valueOf(wantAuthnRequestsSigned));
    }

    public String getEncryptionPublicKey() {
        return getConfig().get("encryptionPublicKey");
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put("encryptionPublicKey", encryptionPublicKey);
    }

    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get("postBindingAuthnRequest"));
    }

    public void setPostBindingAuthnRequest(boolean postBindingAuthnRequest) {
        getConfig().put("postBindingAuthnRequest", String.valueOf(postBindingAuthnRequest));
    }

    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get("postBindingResponse"));
    }

    public void setPostBindingResponse(boolean postBindingResponse) {
        getConfig().put("postBindingResponse", String.valueOf(postBindingResponse));
    }
}
