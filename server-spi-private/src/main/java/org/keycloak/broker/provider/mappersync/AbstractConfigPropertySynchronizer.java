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

package org.keycloak.broker.provider.mappersync;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.utils.StringUtil;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract base class for updating a single reference (specified via a single config property).
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractConfigPropertySynchronizer<T extends ProviderEvent> implements ConfigSynchronizer<T> {
    private static final Logger LOG = Logger.getLogger(AbstractConfigPropertySynchronizer.class);

    protected abstract String getConfigPropertyName();

    protected abstract void updateConfigPropertyIfNecessary(T event, String currentPropertyValue,
            Consumer<String> propertyUpdater);

    @Override
    public final void handleEvent(T event, IdentityProviderMapperModel idpMapper) {
        Map<String, String> config = idpMapper.getConfig();
        if (config == null) {
            return;
        }

        String configPropertyName = getConfigPropertyName();
        String configuredValue = config.get(configPropertyName);
        if (StringUtil.isBlank(configuredValue)) {
            return;
        }

        Consumer<String> propertyUpdater = value -> config.put(configPropertyName, value);

        updateConfigPropertyIfNecessary(event, configuredValue, propertyUpdater);

        final String newConfiguredValue = config.get(configPropertyName);
        if (!configuredValue.equals(newConfiguredValue)) {
            RealmModel realm = extractRealm(event);

            LOG.infof(
                    "Reference of type '%s' changed from '%s' to '%s' in realm '%s'. Adjusting the reference from mapper '%s' of IDP '%s'.",
                    configPropertyName, configuredValue, newConfiguredValue, realm.getName(), idpMapper.getName(),
                    idpMapper.getIdentityProviderAlias());
            realm.updateIdentityProviderMapper(idpMapper);
        }
    }

}
