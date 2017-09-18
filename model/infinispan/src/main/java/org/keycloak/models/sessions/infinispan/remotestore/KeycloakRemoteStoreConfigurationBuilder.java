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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
            Field f = Reflections.findDeclaredField(AttributeSet.class, "attributes");
            f.setAccessible(true);
            Map<String, Attribute<? extends Object>> attributesInternal = (Map<String, Attribute<? extends Object>>) f.get(this.attributes);

            AttributeDefinition<String> def = KeycloakRemoteStoreConfiguration.USE_CONFIG_TEMPLATE_FROM_CACHE;
            Attribute<String> attribute = def.toAttribute();
            attributesInternal.put(def.name(), attribute);

            def = KeycloakRemoteStoreConfiguration.REMOTE_SERVERS;
            attribute = def.toAttribute();
            attributesInternal.put(def.name(), attribute);

            AttributeDefinition<Boolean> defBool = KeycloakRemoteStoreConfiguration.SESSION_CACHE;
            Attribute<Boolean> attributeBool = defBool.toAttribute();
            attributesInternal.put(defBool.name(), attributeBool);

        } catch (IllegalAccessException iae) {
            throw new CacheConfigurationException(iae);
        }
    }


    @Override
    public KeycloakRemoteStoreConfiguration create() {
        String remoteServersAttr = attributes.attribute(KeycloakRemoteStoreConfiguration.REMOTE_SERVERS).get();
        boolean isServersAlreadySet = isServersAlreadySet();
        if (remoteServersAttr != null && !isServersAlreadySet) {
            parseRemoteServersAttr(remoteServersAttr);
        }

        RemoteStoreConfiguration cfg = super.create();
        KeycloakRemoteStoreConfiguration cfg2 = new KeycloakRemoteStoreConfiguration(cfg);
        return cfg2;
    }


    public KeycloakRemoteStoreConfigurationBuilder useConfigTemplateFromCache(String useConfigTemplateFromCache) {
        attributes.attribute(KeycloakRemoteStoreConfiguration.USE_CONFIG_TEMPLATE_FROM_CACHE).set(useConfigTemplateFromCache);
        return this;
    }


    public KeycloakRemoteStoreConfigurationBuilder remoteServers(String remoteServers) {
        attributes.attribute(KeycloakRemoteStoreConfiguration.REMOTE_SERVERS).set(remoteServers);
        return this;
    }


    public KeycloakRemoteStoreConfigurationBuilder sessionCache(Boolean sessionCache) {
        attributes.attribute(KeycloakRemoteStoreConfiguration.SESSION_CACHE).set(sessionCache);
        return this;
    }


    private void parseRemoteServersAttr(String remoteServers) {
        StringTokenizer st = new StringTokenizer(remoteServers, ",");

        while (st.hasMoreElements()) {
            String nodeStr = st.nextToken();
            String[] node = nodeStr.trim().split(":", 2);

            addServer()
                    .host(node[0].trim())
                    .port(Integer.parseInt(node[1].trim()));
        }
    }


    private boolean isServersAlreadySet() {
        try {
            Field f = Reflections.findDeclaredField(RemoteStoreConfigurationBuilder.class, "servers");
            f.setAccessible(true);
            List originalRemoteServers = (List) f.get(this);
            return !originalRemoteServers.isEmpty();
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
}
