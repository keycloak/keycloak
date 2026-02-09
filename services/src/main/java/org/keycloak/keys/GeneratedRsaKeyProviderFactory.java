/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GeneratedRsaKeyProviderFactory extends AbstractGeneratedRsaKeyProviderFactory {

    private static final Logger logger = Logger.getLogger(GeneratedRsaKeyProviderFactory.class);

    public static final String ID = "rsa-generated";

    private static final String HELP_TEXT = "Generates RSA signature keys and creates a self-signed certificate";

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        if (model.getConfig().get(Attributes.KEY_USE) == null) {
            // for backward compatibility : it allows "enc" key use for "rsa-generated" provider
            model.put(Attributes.KEY_USE, KeyUse.SIG.name());
        }
        return new AbstractRsaKeyProvider(session.getContext().getRealm(), model){};
    }

    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return generatedRsaKeyConfigurationBuilder()
                .property(Attributes.RS_ALGORITHM_PROPERTY)
                .build();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected boolean isValidKeyUse(KeyUse keyUse) {
        return keyUse.equals(KeyUse.SIG);
    }

    @Override
    protected boolean isSupportedRsaAlgorithm(String algorithm) {
        return algorithm.equals(Algorithm.RS256) 
                || algorithm.equals(Algorithm.PS256) 
                || algorithm.equals(Algorithm.RS384)
                || algorithm.equals(Algorithm.PS384) 
                || algorithm.equals(Algorithm.RS512) 
                || algorithm.equals(Algorithm.PS512);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
