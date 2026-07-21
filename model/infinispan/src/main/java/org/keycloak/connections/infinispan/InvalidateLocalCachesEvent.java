/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import org.keycloak.provider.ProviderEvent;

/**
 * A {@link ProviderEvent} that clears all local caches to prevent stale cached data.
 * <p>
 * This event should be triggered when there is a risk of missing invalidation events, for example after a split-brain
 * scenario where some cache invalidations may have been lost. Unlike most provider events, this event does not carry a
 * {@link org.keycloak.models.KeycloakSessionFactory} reference and the listeners must obtain it through other means.
 */
public enum InvalidateLocalCachesEvent implements ProviderEvent {
    INSTANCE;
}
