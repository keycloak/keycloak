/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keys;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author <a href="mailto:f.b.rissi@gmail.com">Filipe Bojikian Rissi</a>
 */
public class ImportedRsaEncKeyProviderFactory extends AbstractImportedRsaKeyProviderFactory {

    public static final String ID = "rsa-enc";

    private static final String HELP_TEXT = "RSA for key encryption provider that can optionally generated a self-signed certificate";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractImportedRsaKeyProviderFactory.rsaKeyConfigurationBuilder()
            .property(Attributes.RS_ENC_ALGORITHM_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        model.put(Attributes.KEY_USE, KeyUse.ENC.name());
        return new ImportedRsaKeyProvider(session.getContext().getRealm(), model);
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected boolean isValidKeyUse(KeyUse keyUse) {
        return keyUse.equals(KeyUse.ENC);
    }

    @Override
    protected boolean isSupportedRsaAlgorithm(String algorithm) {
        return algorithm.equals(JWEConstants.RSA1_5)
                || algorithm.equals(JWEConstants.RSA_OAEP)
                || algorithm.equals(JWEConstants.RSA_OAEP_256);
    }

    @Override
    public String getId() {
        return ID;
    }

}
