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

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.model.CredentialConfigId;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.VerifiableCredentialType;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Optional;

/**
 * Provider Factory to create {@link  SdJwtSigningService}s
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SdJwtSigningServiceProviderFactory implements VCSigningServiceProviderFactory {

    public static final String SUPPORTED_FORMAT = Format.SD_JWT_VC;
    private static final String HELP_TEXT = "Issues SD-JWT-VCs following the specification of https://drafts.oauth.net/oauth-sd-jwt-vc/draft-ietf-oauth-sd-jwt-vc.html.";

    @Override
    public VerifiableCredentialsSigningService create(KeycloakSession session, ComponentModel model) {
        String keyId = model.get(SigningProperties.KEY_ID.getKey());
        String algorithmType = model.get(SigningProperties.ALGORITHM_TYPE.getKey());
        Optional<String> kid = Optional.ofNullable(model.get(SigningProperties.KID_HEADER.getKey()));

        // Store vct as a conditional attribute of the signing service.
        // But is vcConfigId is provided, vct must be provided as well.
        String vct = model.get(SigningProperties.VC_VCT.getKey());
        String vcConfigId = model.get(SigningProperties.VC_CONFIG_ID.getKey());

        return new SdJwtSigningService(session, keyId, algorithmType, kid,
                VerifiableCredentialType.from(vct), CredentialConfigId.from(vcConfigId));
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return VCSigningServiceProviderFactory.configurationBuilder()
                .property(SigningProperties.ALGORITHM_TYPE.asConfigProperty())
                .property(SigningProperties.KID_HEADER.asConfigProperty())
                .property(SigningProperties.VC_VCT.asConfigProperty())
                .property(SigningProperties.VC_CONFIG_ID.asConfigProperty())
                .build();
    }

    @Override
    public String getId() {
        return SUPPORTED_FORMAT;
    }

    @Override
    public void validateSpecificConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper helper = ConfigurationValidationHelper.check(model)
                .checkRequired(SigningProperties.ALGORITHM_TYPE.asConfigProperty());
        // Make sure VCT is set if vc config id is set.
        if (model.get(SigningProperties.VC_CONFIG_ID.getKey()) != null) {
            helper.checkRequired(SigningProperties.VC_VCT.asConfigProperty());
        }
    }

    @Override
    public String supportedFormat() {
        return SUPPORTED_FORMAT;
    }
}
