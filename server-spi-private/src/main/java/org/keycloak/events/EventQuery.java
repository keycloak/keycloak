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

import java.util.Date;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventQuery {

    /**
     * Search events with given types
     * @param types requested types
     * @return this object for method chaining
     */
    EventQuery type(EventType... types);

    /**
     * Search events within realm
     * @param realmId id of realm
     * @return this object for method chaining
     */
    EventQuery realm(String realmId);

    /**
     * Search events for only one client
     * @param clientId id of client
     * @return this object for method chaining
     */
    EventQuery client(String clientId);

    /**
     * Search events for only one user
     * @param userId id of user
     * @return this object for method chaining
     */
    EventQuery user(String userId);

    /**
     * Search events that are on or after {@code fromDate}
     * @param fromDate date
     * @return this object for method chaining
     */
    @Deprecated
    EventQuery fromDate(Date fromDate);

    /**
     * Search events that are on or after {@code fromDate}
     * @param fromDate from timestamp
     * @return this object for method chaining
     */
    EventQuery fromDate(long fromDate);

    /**
     * Search events that are on or before {@code toDate}
     * @param toDate date
     * @return this object for method chaining
     */
    @Deprecated
    EventQuery toDate(Date toDate);

    /**
     * Search events that are on or before {@code toDate}
     * @param toDate to timestamp
     * @return this object for method chaining
     */
    EventQuery toDate(long toDate);

    /**
     * Search events from ipAddress
     * @param ipAddress ip
     * @return this object for method chaining
     */
    EventQuery ipAddress(String ipAddress);

    /**
     * Index of the first result to return.
     * @param firstResult the index. Ignored if negative.
     * @return this object for method chaining
     */
    EventQuery firstResult(int firstResult);

    /**
     * Maximum number of results to return.
     * @param max a number. Ignored if negative.
     * @return this object for method chaining
     */
    EventQuery maxResults(int max);

    /**
     * Order the result by descending time
     *
     * @return <code>this</code> for method chaining
     */
    EventQuery orderByDescTime();

    /**
     * Order the result by ascending time
     *
     * @return <code>this</code> for method chaining
     */
    EventQuery orderByAscTime();

    /**
     * Returns requested results that match given criteria as a stream.
     * @return Stream of events. Never returns {@code null}.
     */
    Stream<Event> getResultStream();
}
