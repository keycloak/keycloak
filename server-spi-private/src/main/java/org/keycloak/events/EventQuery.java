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
import java.util.List;
import java.util.stream.Collectors;
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
     * Search events that are newer than {@code fromDate}
     * @param fromDate date
     * @return this object for method chaining
     */
    EventQuery fromDate(Date fromDate);

    /**
     * Search events that are older than {@code toDate}
     * @param toDate date
     * @return this object for method chaining
     */
    EventQuery toDate(Date toDate);

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
     * @deprecated Use {@link #getResultStream() getResultStream} instead.
     */
    @Deprecated
    default List<Event> getResultList() {
        return getResultStream().collect(Collectors.toList());
    }

    /**
     * Returns requested results that match given criteria as a stream.
     * @return Stream of events. Never returns {@code null}.
     */
    Stream<Event> getResultStream();
}
