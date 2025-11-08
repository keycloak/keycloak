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

package org.keycloak.protocol.oid4vc.issuance.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Allows to add statically configured claims to the credential subject
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCStaticClaimMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-static-claim-mapper";

    public static final String STATIC_CLAIM_KEY = "staticValue";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
        subjectPropertyNameConfig.setName(CLAIM_NAME);
        subjectPropertyNameConfig.setLabel("Static Claim Property Name");
        subjectPropertyNameConfig.setHelpText("Name of the property to contain the static value.");
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

        ProviderConfigProperty claimValueConfig = new ProviderConfigProperty();
        claimValueConfig.setName(STATIC_CLAIM_KEY);
        claimValueConfig.setLabel("Static Claim Value");
        claimValueConfig.setHelpText("Value to be set for the property.");
        claimValueConfig.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(claimValueConfig);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // nothing to do for the mapper.
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        String staticValue = mapperModel.getConfig().get(STATIC_CLAIM_KEY);
        claims.put(propertyName, staticValue);
    }

    @Override
    public String getDisplayType() {
        return "Static Claim Mapper";
    }

    @Override
    public String getHelpText() {
        return "Allows to set static values for the credential subject.";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCStaticClaimMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}
