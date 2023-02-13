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

package org.keycloak.events;

import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventStoreProvider extends EventListenerProvider {

    /**
     * Returns an object representing auth event query of type {@link EventQuery}.
     *
     * The object is used for collecting requested properties of auth events (e.g. realm, operation, resourceType
     * time boundaries, etc.) and contains the {@link EventQuery#getResultStream()} method that returns all
     * objects from this store provider that have given properties.
     *
     * @return a query object
     */
    EventQuery createQuery();

    /**
     * Returns an object representing admin event query of type {@link AdminEventQuery}.
     *
     * The object is used for collecting requested properties of admin events (e.g. realm, operation, resourceType
     * time boundaries, etc.) and contains the {@link AdminEventQuery#getResultStream()} method that returns all
     * objects from this store provider that have given properties.
     *
     * @return a query object
     */
    AdminEventQuery createAdminQuery();

    /**
     * Removes all auth events from this store provider.
     *
     * @deprecated Unused method. Currently, used only in the testsuite
     */
    void clear();

    /**
     * Removes all auth events for the realm from this store provider.
     * @param realm the realm
     *
     */
    void clear(RealmModel realm);

    /**
     * Removes all auth events for the realm that are older than {@code olderThan} from this store provider.
     *
     * @param realm the realm
     * @param olderThan point in time in milliseconds
     */
    void clear(RealmModel realm, long olderThan);

    /**
     * Clears all expired events in all realms
     *
     * @deprecated This method is problem from the performance perspective. Some storages can provide better way
     * for doing this (e.g. entry lifespan in the Infinispan server, etc.). We need to leave solving event expiration
     * to each storage provider separately using expiration field on entity level.
     *
     */
    void clearExpiredEvents();

    /**
     * Removes all admin events from this store provider.
     *
     * @deprecated Unused method. Currently, used only in the testsuite
     */
    void clearAdmin();

    /**
     * Removes all auth events for the realm from this store provider.
     * @param realm the realm
     */
    void clearAdmin(RealmModel realm);

    /**
     * Removes all auth events for the realm that are older than {@code olderThan} from this store provider.
     *
     * @param realm the realm
     * @param olderThan point in time in milliseconds
     */
    void clearAdmin(RealmModel realm, long olderThan);

}
