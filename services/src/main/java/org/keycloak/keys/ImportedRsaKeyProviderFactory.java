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

import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ImportedRsaKeyProviderFactory extends AbstractImportedRsaKeyProviderFactory {

    public static final String ID = "rsa";

    private static final String HELP_TEXT = "RSA signature key provider that can optionally generated a self-signed certificate";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = AbstractImportedRsaKeyProviderFactory.rsaKeyConfigurationBuilder()
            .property(Attributes.RS_ALGORITHM_PROPERTY)
            .build();

    @Override
    public KeyProvider create(KeycloakSession session, ComponentModel model) {
        if (model.getConfig().get(Attributes.KEY_USE) == null) {
            // for backward compatibility : it allows "enc" key use for "rsa" provider
            model.put(Attributes.KEY_USE, KeyUse.SIG.name());
        }
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
    public String getId() {
        return ID;
    }

}
