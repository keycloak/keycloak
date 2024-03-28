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

package org.keycloak.protocol.oid4vc.issuance.signing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.issuance.OffsetTimeProvider;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Optional;

/**
 * Provider Factory to create {@link  LDSigningService}s
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class LDSigningServiceProviderFactory implements VCSigningServiceProviderFactory {
    public static final Format SUPPORTED_FORMAT = Format.LDP_VC;
    private static final String HELP_TEXT = "Issues Verifiable Credentials in the W3C Data Model, using Linked-Data Proofs. See https://www.w3.org/TR/vc-data-model/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public VerifiableCredentialsSigningService create(KeycloakSession session, ComponentModel model) {
        String keyId = model.get(SigningProperties.KEY_ID.getKey());
        String proofType = model.get(SigningProperties.PROOF_TYPE.getKey());
        String algorithmType = model.get(SigningProperties.ALGORITHM_TYPE.getKey());
        Optional<String> kid = Optional.ofNullable(model.get(SigningProperties.KID_HEADER.getKey()));
        return new LDSigningService(session, keyId, algorithmType, proofType, OBJECT_MAPPER, new OffsetTimeProvider(), kid);
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return VCSigningServiceProviderFactory.configurationBuilder()
                .property(SigningProperties.ALGORITHM_TYPE.asConfigProperty())
                .property(SigningProperties.PROOF_TYPE.asConfigProperty())
                .property(SigningProperties.KID_HEADER.asConfigProperty())
                .build();
    }

    @Override
    public String getId() {
        return SUPPORTED_FORMAT.toString();
    }

    @Override
    public void validateSpecificConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired(SigningProperties.ALGORITHM_TYPE.asConfigProperty())
                .checkRequired(SigningProperties.PROOF_TYPE.asConfigProperty());
    }

    @Override
    public Format supportedFormat() {
        return SUPPORTED_FORMAT;
    }

}