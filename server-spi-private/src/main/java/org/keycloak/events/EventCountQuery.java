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

public interface EventCountQuery {

    /**
     * Search events with given types
     * @param types requested types
     * @return this object for method chaining
     */
    EventCountQuery type(EventType... types);

    /**
     * Search events within realm
     * @param realmId id of realm
     * @return this object for method chaining
     */
    EventCountQuery realm(String realmId);

    /**
     * Search events for only one client
     * @param clientId id of client
     * @return this object for method chaining
     */
    EventCountQuery client(String clientId);

    /**
     * Search events for only one user
     * @param userId id of user
     * @return this object for method chaining
     */
    EventCountQuery user(String userId);

    /**
     * Search events that are on or after {@code fromDate}
     * @param fromDate from timestamp
     * @return this object for method chaining
     */
    EventCountQuery fromDate(long fromDate);

    /**
     * Search events that are on or before {@code toDate}
     * @param toDate to timestamp
     * @return this object for method chaining
     */
    EventCountQuery toDate(long toDate);

    /**
     * Search events from ipAddress
     * @param ipAddress ip
     * @return this object for method chaining
     */
    EventCountQuery ipAddress(String ipAddress);

    /**
     * Returns requested results count that match given criteria as a long.
     * @return number of events. Never returns {@code null}.
     */
    Long getCount();
}
