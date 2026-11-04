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

import java.util.ArrayList;
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

    /**
     * Parses the metadata claim(s) contributed by a single protocol mapper. Most mappers produce at most one
     * claim; a mapper that spreads an external response's fields into individual top-level claims (see
     * {@link OID4VCMapper#getMetadataAttributePaths()}) can produce several from one mapper instance, all
     * sharing the same mandatory/display configuration.
     */
    public static List<Claim> parseAll(KeycloakSession keycloakSession,
                                        String credentialFormat,
                                        Oid4vcProtocolMapperModel protocolMapper) {
        try {
            ProtocolMapper protocolMapperImpl = keycloakSession.getProvider(ProtocolMapper.class,
                                                                            protocolMapper.getProtocolMapper());
            if (!(protocolMapperImpl instanceof OID4VCMapper)) {
                return List.of();
            }
            OID4VCMapper mapper = (OID4VCMapper) protocolMapperImpl;
            mapper.setMapperModel(protocolMapper, credentialFormat);

            if (!mapper.supportsCredentialFormat(credentialFormat)) {
                return List.of();
            }

            if (!mapper.includeInMetadata()) {
                return List.of();
            }

            if (!mapper.passesMappingGuards()) {
                return List.of();
            }

            List<List<String>> attributePaths = mapper.getMetadataAttributePaths();
            if (attributePaths == null || attributePaths.isEmpty()) {
                return List.of();
            }

            List<ClaimDisplay> claimDisplayList = null;
            String displayString = protocolMapper.getDisplay();
            if (StringUtil.isNotBlank(displayString)) {
                TypeReference<List<ClaimDisplay>> typeReference = new TypeReference<>() {};
                claimDisplayList = JsonSerialization.mapper.readValue(displayString, typeReference);
            }

            List<Claim> claims = new ArrayList<>();
            for (List<String> attributePath : attributePaths) {
                if (attributePath == null || attributePath.isEmpty()) {
                    continue;
                }
                Claim claim = new Claim();
                claim.setName(String.join(".", attributePath));
                claim.setPath(attributePath);
                claim.setMandatory(protocolMapper.isMandatory());
                claim.setDisplay(claimDisplayList);
                claims.add(claim);
            }
            return claims;
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
