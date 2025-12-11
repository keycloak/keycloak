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
package org.keycloak.protocol.oid4vc.model;

import java.util.List;
import java.util.Optional;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.oid4vci.Oid4vcProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCMapper;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Holding metadata on a claim of verifiable credential.
 * <p>
 * See: <a
 * href="https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2.2">openid-4-verifiable-credential-issuance-1_0.html#appendix-A.2.2</a>
 *
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Claim {

    /**
     * the claims name, which is not part of the underlying JSON structure
     */
    @JsonIgnore
    private String name;

    @JsonProperty("path")
    private List<String> path;

    @JsonProperty("mandatory")
    private Boolean mandatory;

    @JsonProperty("display")
    private List<ClaimDisplay> display;

    public static Optional<Claim> parse(KeycloakSession keycloakSession,
                                        String credentialFormat,
                                        Oid4vcProtocolMapperModel protocolMapper) {
        try {
            Claim claim = new Claim();
            ProtocolMapper protocolMapperImpl = keycloakSession.getProvider(ProtocolMapper.class,
                                                                            protocolMapper.getProtocolMapper());
            if (!(protocolMapperImpl instanceof OID4VCMapper)) {
                return Optional.empty();
            }
            OID4VCMapper mapper = (OID4VCMapper) protocolMapperImpl;
            mapper.setMapperModel(protocolMapper, credentialFormat);

            if (!mapper.includeInMetadata()) {
                return Optional.empty();
            }

            claim.setName(String.join(".", mapper.getMetadataAttributePath()));

            claim.setPath(mapper.getMetadataAttributePath());
            claim.setMandatory(protocolMapper.isMandatory());

            String displayString = protocolMapper.getDisplay();
            if (StringUtil.isNotBlank(displayString)) {
                TypeReference<List<ClaimDisplay>> typeReference = new TypeReference<>() {};
                List<ClaimDisplay> claimDisplayList = JsonSerialization.mapper.readValue(displayString, typeReference);
                claim.setDisplay(claimDisplayList);
            }

            return Optional.of(claim);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public Claim setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getPath() {
        return path;
    }

    public Claim setPath(List<String> path) {
        this.path = path;
        return this;
    }

    public boolean isMandatory() {
        return Optional.ofNullable(mandatory).orElse(false);
    }

    public Claim setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
        return this;
    }

    public List<ClaimDisplay> getDisplay() {
        return display;
    }

    public Claim setDisplay(List<ClaimDisplay> display) {
        this.display = display;
        return this;
    }
}
