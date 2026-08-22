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

package org.keycloak.infinispan.health.site;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Abstraction over the Infinispan server management API used to query and control cross-site replication state.
 */
public interface InfinispanManagement extends AutoCloseable {

    /**
     * Returns the backup site statuses for all cross-site replicated caches.
     * <p>
     * The returned map keys are site names and the values are their replication status (e.g. {@code "online"},
     * {@code "offline"}).
     *
     * @return A map of site name to replication status.
     */
    Map<String, String> siteStatus() throws ExecutionException, InterruptedException;

    /**
     * Returns the local site identity and the set of sites currently visible in the cluster view.
     *
     * @return The {@link SiteConnection} for this node.
     */
    SiteConnection siteConnection() throws ExecutionException, InterruptedException;

    /**
     * Disconnects (takes offline) the given remote sites for all cross-site replicated caches.
     *
     * @param remoteSites The site names to disconnect.
     */
    void disconnect(Collection<String> remoteSites) throws ExecutionException, InterruptedException;

}
