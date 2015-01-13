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

import java.util.Map;

/**
 * @author Pedro Igor
 */
public class SAMLIdentityProviderConfig extends IdentityProviderModel {

    public SAMLIdentityProviderConfig() {
        super();
    }

    public SAMLIdentityProviderConfig(String providerId, String id, String name, Map<String, String> config) {
        super(providerId, id, name, config);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get("singleSignOnServiceUrl");
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put("singleSignOnServiceUrl", singleSignOnServiceUrl);
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

    public String getSigningPublicKey() {
        return getConfig().get("signingPublicKey");
    }

    public void setSigningPublicKey(String signingPublicKey) {
        getConfig().put("signingPublicKey", signingPublicKey);
    }

    public String getNameIDPolicyFormat() {
        return getConfig().get("nameIDPolicyFormat");
    }

    public void setNameIDPolicyFormat(String signingPublicKey) {
        getConfig().put("nameIDPolicyFormat", signingPublicKey);
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
}
