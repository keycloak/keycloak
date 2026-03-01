/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class GeneratedRsaEncKeyProviderFactory extends AbstractGeneratedRsaKeyProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedRsaEncKeyProviderFactory.class);

    public static final String ID = "rsa-enc-generated";

    private static final String HELP_TEXT = "Generates RSA keys for key encryption and creates a self-signed certificate";

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
        return generatedRsaKeyConfigurationBuilder()
                .property(Attributes.RS_ENC_ALGORITHM_PROPERTY)
                .build();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean isValidKeyUse(KeyUse keyUse) {
        return keyUse.equals(KeyUse.ENC);
    }

    @Override
    protected boolean isSupportedRsaAlgorithm(String algorithm) {
        return algorithm.equals(Algorithm.RSA1_5)
                || algorithm.equals(Algorithm.RSA_OAEP)
                || algorithm.equals(Algorithm.RSA_OAEP_256);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
