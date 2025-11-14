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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.model.CredentialSubject;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.provider.ProviderConfigProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map issuance date to the credential, under the default claim name "iat"
 * <p>
 * subjectProperty can be used to change the claim name.
 * <p>
 * Source of the information can either be computed, or read from the VerifiableCredential object
 * bearing other claims. Default is the value in the verifiable credential.
 * <p>
 * We will use the java.time.temporal.ChronoUnit enum values to help flatten down the time.
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class OID4VCIssuedAtTimeClaimMapper extends OID4VCMapper {

    public static final String MAPPER_ID = "oid4vc-issued-at-time-claim-mapper";

    // We will use the java.time.temporal.ChronoUnit enum values to help flatten down the time.
    // Omit property if no truncation.
    public static final String TRUNCATE_TO_TIME_UNIT_KEY = "truncateToTimeUnit";

    // Time computed (COMPUTE) or taken from the verifiable credential (VC).
    // Defaults to VC. Falls back to COMPUTE.
    public static final String VALUE_SOURCE = "valueSource";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(OID4VCIssuedAtTimeClaimMapper.class);

    static {
        ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
        subjectPropertyNameConfig.setName(CLAIM_NAME);
        subjectPropertyNameConfig.setLabel("Time Claim Name");
        subjectPropertyNameConfig.setHelpText("Name of this time claim. Default is iat");
        subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
        subjectPropertyNameConfig.setDefaultValue("iat");
        CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

        ProviderConfigProperty truncateToTimeUnit = new ProviderConfigProperty();
        truncateToTimeUnit.setName(TRUNCATE_TO_TIME_UNIT_KEY);
        truncateToTimeUnit.setLabel("Truncate To Time Unit");
        truncateToTimeUnit.setHelpText("Truncate time to the first second of the MINUTES, HOURS, HALF_DAYS, DAYS, WEEKS, MONTHS or YEARS. Such as to prevent correlation of credentials based on this time value.");
        truncateToTimeUnit.setType(ProviderConfigProperty.LIST_TYPE);
        truncateToTimeUnit.setOptions(List.of("MINUTES", "HOURS", "HALF_DAYS", "DAYS", "WEEKS", "MONTHS", "YEARS"));
        CONFIG_PROPERTIES.add(truncateToTimeUnit);

        ProviderConfigProperty valueSource = new ProviderConfigProperty();
        valueSource.setName(VALUE_SOURCE);
        valueSource.setLabel("Source of Value");
        valueSource.setHelpText("Tells the protocol mapper where to get the information. For now: COMPUTE or VC. Default is COMPUTE, in which this protocol mapper computes the current time in seconds. With value `VC`, the time is read from the verifiable credential issuance date field.");
        valueSource.setType(ProviderConfigProperty.LIST_TYPE);
        valueSource.setOptions(List.of("COMPUTE", "VC"));
        valueSource.setDefaultValue("COMPUTE");
        CONFIG_PROPERTIES.add(valueSource);
    }

    @Override
    protected List<ProviderConfigProperty> getIndividualConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    /**
     * this claim is not added by default to the metadata
     */
    @Override
    public boolean includeInMetadata() {
        return Optional.ofNullable(mapperModel.getConfig().get(CredentialScopeModel.INCLUDE_IN_METADATA))
                       .map(Boolean::parseBoolean)
                       .orElse(false);
    }

    public void setClaimsForCredential(VerifiableCredential verifiableCredential,
                                       UserSessionModel userSessionModel) {
        // Set the value
        List<String> attributePath = getMetadataAttributePath();
        String propertyName = attributePath.get(attributePath.size() - 1);
        if (propertyName == null) {
            log.error("Invalid configuration: missing config-property '{}' for mapper '{}' of type '{}'. Mapper is ignored.",
                      CLAIM_NAME,
                      mapperModel.getName(),
                      MAPPER_ID);
            return;
        }

        Instant iat = Optional.ofNullable(mapperModel.getConfig())
                .flatMap(config -> Optional.ofNullable(config.get(VALUE_SOURCE)))
                .filter(valueSource -> Objects.equals(valueSource, "COMPUTE"))
                .map(valueSource -> Instant.now())
                .orElseGet(() -> Optional.ofNullable(verifiableCredential.getIssuanceDate())
                        .orElse(Instant.now()));

        // truncate is possible. Return iat if not.
        Instant iatTrunc = Optional.ofNullable(mapperModel.getConfig())
                .flatMap(config -> Optional.ofNullable(config.get(TRUNCATE_TO_TIME_UNIT_KEY)))
                .filter(String::isEmpty)
                .map(ChronoUnit::valueOf)
                .map(iat::truncatedTo)
                .orElse(iat);

        CredentialSubject credentialSubject = verifiableCredential.getCredentialSubject();
        credentialSubject.setClaims(propertyName, iatTrunc.getEpochSecond());
    }

    @Override
    public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
        // NoOp
    }

    @Override
    public String getDisplayType() {
        return "Issuance Date Claim Mapper";
    }

    @Override
    public String getHelpText() {
        return "Allows to set the issuance date credential subject.";
    }

    @Override
    public ProtocolMapper create(KeycloakSession session) {
        return new OID4VCIssuedAtTimeClaimMapper();
    }

    @Override
    public String getId() {
        return MAPPER_ID;
    }
}
