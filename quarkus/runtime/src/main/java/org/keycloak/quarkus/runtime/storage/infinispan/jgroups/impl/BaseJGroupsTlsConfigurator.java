/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups.impl;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.util.SocketFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quarkus.runtime.storage.infinispan.CacheManagerFactory;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsStackConfigurator;
import org.keycloak.quarkus.runtime.storage.infinispan.jgroups.JGroupsUtil;

abstract class BaseJGroupsTlsConfigurator implements JGroupsStackConfigurator {

    @Override
    public void configure(ConfigurationBuilderHolder holder, KeycloakSession session) {
        var factory = createSocketFactory(session);
        JGroupsUtil.transportOf(holder).addProperty(JGroupsTransport.SOCKET_FACTORY, factory);
        JGroupsUtil.validateTlsAvailable(holder);
        CacheManagerFactory.logger.info("JGroups Encryption enabled (mTLS).");
    }

    abstract SocketFactory createSocketFactory(KeycloakSession session);
}
