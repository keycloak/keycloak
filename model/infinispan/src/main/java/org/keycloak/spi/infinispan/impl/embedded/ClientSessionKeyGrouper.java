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

package org.keycloak.spi.infinispan.impl.embedded;

import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;

import org.infinispan.distribution.group.Grouper;

/**
 * A {@link Grouper} implementation that uses the User Session ID to assign the Client Session to the cache segment. It
 * groups all the Client Sessions belonging to the same User Session in the same node where the User Session lives.
 */
public enum ClientSessionKeyGrouper implements Grouper<EmbeddedClientSessionKey> {

    INSTANCE;

    // The Infinispan parser expects a constructor or a static "getInstance" method; fixes ClusterConfigKeepAliveDistTest.
    public static ClientSessionKeyGrouper getInstance() {
        return INSTANCE;
    }

    @Override
    public Object computeGroup(EmbeddedClientSessionKey key, Object group) {
        return key.userSessionId();
    }

    @Override
    public Class<EmbeddedClientSessionKey> getKeyType() {
        return EmbeddedClientSessionKey.class;
    }
}
