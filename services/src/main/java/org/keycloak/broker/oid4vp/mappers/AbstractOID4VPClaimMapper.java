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

package org.keycloak.broker.oid4vp.mappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.broker.oid4vp.DcqlQuery;
import org.keycloak.broker.oid4vp.OID4VPIdentityProvider;
import org.keycloak.broker.oid4vp.OID4VPIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * Base for OID4VP identity provider mappers that read a claim of one of the verified credential
 * presentations, addressed by the credential id and a {@link ClaimPath} over its claims JSON.
 * Subclasses decide where the resolved values go. Not tied to a credential format: any format whose
 * verification yields claims JSON under {@link OID4VPIdentityProvider#CREDENTIAL_CLAIMS} can build on it.
 */
public abstract class AbstractOID4VPClaimMapper extends AbstractIdentityProviderMapper
        implements EnvironmentDependentProviderFactory, DcqlQueryContributor {

    protected static final Logger logger = Logger.getLogger(AbstractOID4VPClaimMapper.class);

    public static final String CREDENTIAL = "credential";

    // Rendered by the admin console as a select of the credentials the identity provider requests

    public static final String CREDENTIAL_LIST_TYPE = "OID4VPCredentialList";
    public static final String CLAIM = "claim";

    private static final String[] COMPATIBLE_PROVIDERS = {OID4VPIdentityProviderFactory.PROVIDER_ID};

    protected static ProviderConfigProperty credentialProperty() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CREDENTIAL);
        property.setLabel("Credential");
        property.setHelpText("Id of the requested credential to read the claim from, as configured in the requested "
                + "credentials of the identity provider. If the credential is not presented, its claim counts as "
                + "absent.");
        property.setType(CREDENTIAL_LIST_TYPE);
        property.setRequired(true);
        return property;
    }

    protected static ProviderConfigProperty claimProperty() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM);
        property.setLabel("Claim");
        property.setHelpText("Path of the claim in the presented credential. Use dot notation for nested claims, "
                + "i.e. 'address.locality', [] to select all array elements, i.e. 'nationalities[]', and [0] to select the first element of the presented array. "
                + "To use dot (.) literally, escape it with backslash (\\.)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        return property;
    }

    @Override
    public String credentialId(IdentityProviderMapperModel mapperModel) {
        String credential = mapperModel.getConfig().get(CREDENTIAL);
        return StringUtil.isBlank(credential) ? null : credential.trim();
    }

    // Null when the claim path is missing or malformed.
    protected ClaimPath claimPath(IdentityProviderMapperModel mapperModel) {
        String claimPath = mapperModel.getConfig().get(CLAIM);
        if (StringUtil.isBlank(claimPath)) {
            logger.warnf("No claim configured for mapper %s", mapperModel.getName());
            return null;
        }
        ClaimPath path = ClaimPath.parse(claimPath.trim());
        if (path == null) {
            logger.warnf("Invalid claim path '%s' in mapper %s", claimPath, mapperModel.getName());
        }
        return path;
    }

    /**
     * The name of the attribute the claim is stored in, null if none is configured.
     */
    protected abstract String attribute(IdentityProviderMapperModel mapperModel);

    @Override
    public List<DcqlQuery.ClaimsQuery> requestedClaims(IdentityProviderMapperModel mapperModel) {
        // Without an attribute the claim is never stored, so the wallet should not have to disclose it.
        if (attribute(mapperModel) == null) {
            return List.of();
        }
        ClaimPath path = claimPath(mapperModel);
        if (path == null) {
            return List.of();
        }
        return List.of(new DcqlQuery.ClaimsQuery(null, path.toClaimsPathPointer(), null));
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VP);
    }

    /**
     * The claims of the credential this mapper reads, null if that credential was not presented.
     */
    protected JsonNode credentialClaims(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        Object claims = context.getContextData().get(OID4VPIdentityProvider.CREDENTIAL_CLAIMS);
        String credentialId = credentialId(mapperModel);
        if (claims == null || credentialId == null) {
            return null;
        }
        // The brokered context may have been serialized into the authentication session in
        // between, for example while the first broker login shows the review profile page.
        JsonNode claimsByCredential = claims instanceof JsonNode node ? node : JsonSerialization.mapper.valueToTree(claims);
        JsonNode credentialClaims = claimsByCredential.get(credentialId);
        return credentialClaims == null || credentialClaims.isNull() ? null : credentialClaims;
    }

    // Null when the claim is absent or the mapper is misconfigured.
    protected List<String> claimValues(IdentityProviderMapperModel mapperModel, JsonNode credentialClaims) {
        ClaimPath path = claimPath(mapperModel);
        if (path == null) {
            return null;
        }
        List<JsonNode> matches = path.select(credentialClaims);
        if (matches.isEmpty()) {
            return null;
        }
        Iterable<JsonNode> selected = matches.size() == 1 && matches.get(0).isArray()
                ? matches.get(0)
                : matches;
        // The values end up in the brokered context, whose serialization restores lists by their
        // concrete class, so they must stay plain ArrayLists.
        List<String> values = new ArrayList<>();
        for (JsonNode node : selected) {
            if (!node.isNull()) {
                values.add(value(node));
            }
        }
        return values;
    }

    protected String value(JsonNode node) {
        if (node.isValueNode()) {
            return node.asText();
        }
        try {
            return JsonSerialization.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize the claim value", e);
        }
    }
}
