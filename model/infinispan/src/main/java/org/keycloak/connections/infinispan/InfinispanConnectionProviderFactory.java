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

package org.keycloak.connections.infinispan;

import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface InfinispanConnectionProviderFactory extends ProviderFactory<InfinispanConnectionProvider> {

    /**
     * Detects network split in the cluster.
     * <p>
     * If a possible network split is detected and this node does not belong to the winning partition, this method will
     * return {@code false}, and should not continue handling requests, to keep data safety.
     *
     * @return {@code true} if the cluster is healthy and this node can continue processing requests. When
     * {@code false}, this node must reject any work.
     */
    default boolean isClusterHealthy() {
        return true;
    }

    /**
     * Checks if the cluster health check is supported.
     * <p>
     * Not all JGroups configurations support discovering network splits and this method signals if the current in use
     * configuration can detect those.
     *
     * @return {@code true} if the cluster health check is supported.
     */
    default boolean isClusterHealthSupported() {
        return false;
    }

}
