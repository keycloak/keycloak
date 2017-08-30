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

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.commons.configuration.attributes.Attribute;
import org.infinispan.commons.configuration.attributes.AttributeDefinition;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfiguration;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@BuiltBy(KeycloakRemoteStoreConfigurationBuilder.class)
@ConfigurationFor(KeycloakRemoteStore.class)
public class KeycloakRemoteStoreConfiguration extends RemoteStoreConfiguration {

    static final AttributeDefinition<String> USE_CONFIG_TEMPLATE_FROM_CACHE = AttributeDefinition.builder("useConfigTemplateFromCache", null, String.class).immutable().build();

    private final Attribute<String> useConfigTemplateFromCache;


    public KeycloakRemoteStoreConfiguration(RemoteStoreConfiguration other) {
        super(other.attributes(), other.async(), other.singletonStore(), other.asyncExecutorFactory(), other.connectionPool());
        useConfigTemplateFromCache = attributes.attribute(USE_CONFIG_TEMPLATE_FROM_CACHE.name());
    }



    public String useConfigTemplateFromCache() {
        return useConfigTemplateFromCache==null ? null : useConfigTemplateFromCache.get();
    }
}
