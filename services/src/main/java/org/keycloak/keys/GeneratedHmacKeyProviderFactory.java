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

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class GeneratedHmacKeyProviderFactory extends AbstractGeneratedSecretKeyProviderFactory<GeneratedHmacKeyProvider> {

    private static final Logger logger = Logger.getLogger(GeneratedHmacKeyProviderFactory.class);

    public static final String ID = "hmac-generated";

    private static final String HELP_TEXT = "Generates HMAC secret key";

    public static final int DEFAULT_HMAC_KEY_SIZE = 64;

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = SecretKeyProviderUtils.configurationBuilder()
            .property(Attributes.SECRET_SIZE_PROPERTY)
            .property(Attributes.HS_ALGORITHM_PROPERTY)
            .build();

    @Override
    public GeneratedHmacKeyProvider create(KeycloakSession session, ComponentModel model) {
        return new GeneratedHmacKeyProvider(model);
    }

    @Override
    public boolean createFallbackKeys(KeycloakSession session, KeyUse keyUse, String algorithm) {
        if (keyUse.equals(KeyUse.SIG) && (algorithm.equals(Algorithm.HS256) || algorithm.equals(Algorithm.HS384) || algorithm.equals(Algorithm.HS512))) {
            RealmModel realm = session.getContext().getRealm();

            ComponentModel generated = new ComponentModel();
            generated.setName("fallback-" + algorithm);
            generated.setParentId(realm.getId());
            generated.setProviderId(ID);
            generated.setProviderType(KeyProvider.class.getName());

            MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
            config.putSingle(Attributes.PRIORITY_KEY, "-100");
            config.putSingle(Attributes.ALGORITHM_KEY, algorithm);
            generated.setConfig(config);

            realm.addComponentModel(generated);

            return true;
        } else {
            return false;
        }
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
    public String getId() {
        return ID;
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    protected int getDefaultKeySize() {
        return DEFAULT_HMAC_KEY_SIZE;
    }
}
