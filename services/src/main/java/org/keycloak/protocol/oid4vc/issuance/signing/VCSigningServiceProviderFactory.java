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

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oid4vc.OID4VCEnvironmentProviderFactory;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Provider Factory to create {@link  VerifiableCredentialsSigningService}s
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public interface VCSigningServiceProviderFactory extends ComponentFactory<VerifiableCredentialsSigningService, VerifiableCredentialsSigningService>, OID4VCEnvironmentProviderFactory {

    /**
     * Key for the realm attribute providing the issuerDidy.
     */
    String ISSUER_DID_REALM_ATTRIBUTE_KEY = "issuerDid";

    public static ProviderConfigurationBuilder configurationBuilder() {
        return ProviderConfigurationBuilder.create()
                .property(SigningProperties.KEY_ID.asConfigProperty());
    }

    @Override
    default void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException {
        ConfigurationValidationHelper.check(model)
                .checkRequired(SigningProperties.KEY_ID.asConfigProperty());
        validateSpecificConfiguration(session, realm, model);
    }


    @Override
    default void close() {
        // no-op
    }

    @Override
    default void init(Config.Scope config) {
        // no-op
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    default boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.OID4VC_VCI);
    }

    /**
     * Should validate potential implementation specific configuration of the factory.
     */
    void validateSpecificConfiguration(KeycloakSession session, RealmModel realm, ComponentModel model) throws ComponentValidationException;

    /**
     * Should return the credentials format supported by the signing service.
     *
     * @return the format
     */
    Format supportedFormat();
}