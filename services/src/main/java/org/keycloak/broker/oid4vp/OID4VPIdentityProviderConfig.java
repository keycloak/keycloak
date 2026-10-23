/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.broker.oid4vp;

import java.util.Arrays;
import java.util.List;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

public class OID4VPIdentityProviderConfig extends IdentityProviderModel {

    public static final String DEFAULT_CREDENTIAL_TYPES = "defaultCredentialTypes";
    public static final String DCQL_QUERY = "dcqlQuery";
    public static final String TRUSTED_ISSUER_JWKS = "trustedIssuerJwks";
    public static final String TRUST_MATERIAL_IDPS = "trustMaterialIdps";
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String WALLET_SCHEME = "walletScheme";
    public static final String SIGNING_KEY_ID = "signingKeyId";
    public static final String RESPONSE_MODE = "responseMode";

    public static final String DEFAULT_WALLET_SCHEME = "openid4vp://";

    public OID4VPIdentityProviderConfig() {
    }

    public OID4VPIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public List<String> getDefaultCredentialTypes() {
        String credentialTypes = getConfig().get(DEFAULT_CREDENTIAL_TYPES);
        if (StringUtil.isBlank(credentialTypes)) {
            return List.of();
        }
        return Arrays.stream(credentialTypes.split(","))
                .map(String::trim)
                .filter(StringUtil::isNotBlank)
                .distinct()
                .toList();
    }

    public void setDefaultCredentialTypes(List<String> credentialTypes) {
        getConfig().put(DEFAULT_CREDENTIAL_TYPES, String.join(",", credentialTypes));
    }

    public String getDcqlQuery() {
        return getConfig().get(DCQL_QUERY);
    }

    public void setDcqlQuery(String dcqlQuery) {
        getConfig().put(DCQL_QUERY, dcqlQuery);
    }

    public String getTrustedIssuerJwks() {
        return getConfig().get(TRUSTED_ISSUER_JWKS);
    }

    public void setTrustedIssuerJwks(String trustedIssuerJwks) {
        getConfig().put(TRUSTED_ISSUER_JWKS, trustedIssuerJwks);
    }

    public String getTrustMaterialIdps() {
        return getConfig().get(TRUST_MATERIAL_IDPS);
    }

    public void setTrustMaterialIdps(String trustMaterialIdps) {
        getConfig().put(TRUST_MATERIAL_IDPS, trustMaterialIdps);
    }

    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    public void setPrincipalAttribute(String principalAttribute) {
        getConfig().put(PRINCIPAL_ATTRIBUTE, principalAttribute);
    }

    public String getSigningKeyId() {
        return getConfig().get(SIGNING_KEY_ID);
    }

    public void setSigningKeyId(String signingKeyId) {
        getConfig().put(SIGNING_KEY_ID, signingKeyId);
    }

    public ResponseMode getResponseMode() {
        String value = getConfig().get(RESPONSE_MODE);
        return StringUtil.isBlank(value) ? ResponseMode.DIRECT_POST : ResponseMode.from(value);
    }

    public void setResponseMode(ResponseMode responseMode) {
        getConfig().put(RESPONSE_MODE, responseMode.value());
    }

    public boolean isEncryptedResponse() {
        return getResponseMode() == ResponseMode.DIRECT_POST_JWT;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        String responseMode = getConfig().get(RESPONSE_MODE);
        if (StringUtil.isNotBlank(responseMode)) {
            ResponseMode.from(responseMode);
        }
        if (StringUtil.isNotBlank(getDcqlQuery())) {
            DcqlQuery.parse(getDcqlQuery());
        } else if (getDefaultCredentialTypes().isEmpty()) {
            throw new IllegalArgumentException(
                    "Default credential types are required unless a DCQL query is configured");
        }
        if (StringUtil.isBlank(getTrustedIssuerJwks()) && StringUtil.isBlank(getTrustMaterialIdps())) {
            throw new IllegalArgumentException(
                    "Either a trusted issuer JWKS or trust material identity provider aliases are required");
        }
    }

    public String getWalletScheme() {
        String value = getConfig().get(WALLET_SCHEME);
        return StringUtil.isNotBlank(value) ? value : DEFAULT_WALLET_SCHEME;
    }

    public void setWalletScheme(String walletScheme) {
        getConfig().put(WALLET_SCHEME, walletScheme);
    }
}
