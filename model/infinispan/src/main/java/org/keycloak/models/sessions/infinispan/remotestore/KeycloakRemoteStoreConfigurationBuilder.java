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

package org.keycloak.models.sessions.infinispan.remotestore;

import java.lang.reflect.Field;
import java.util.Map;

import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.keycloak.common.util.reflections.Reflections;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeycloakRemoteStoreConfigurationBuilder extends RemoteStoreConfigurationBuilder {

    public KeycloakRemoteStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
        super(builder);

        // No better way to add new attribute definition to superclass :/
        try {
            AttributeDefinition<String> def = KeycloakRemoteStoreConfiguration.USE_CONFIG_TEMPLATE_FROM_CACHE;
            Attribute<String> attribute = def.toAttribute();

            Field f = Reflections.findDeclaredField(AttributeSet.class, "attributes");
            f.setAccessible(true);
            Map<String, Attribute<? extends Object>> attributesInternal = (Map<String, Attribute<? extends Object>>) f.get(this.attributes);
            attributesInternal.put(def.name(), attribute);
        } catch (IllegalAccessException iae) {
            throw new CacheConfigurationException(iae);
        }
    }


    @Override
    public KeycloakRemoteStoreConfiguration create() {
        RemoteStoreConfiguration cfg = super.create();
        KeycloakRemoteStoreConfiguration cfg2 = new KeycloakRemoteStoreConfiguration(cfg);
        return cfg2;
    }
}
