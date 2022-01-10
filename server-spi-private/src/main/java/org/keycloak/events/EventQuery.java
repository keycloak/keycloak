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

    EventQuery type(EventType... types);

    EventQuery realm(String realmId);

    EventQuery client(String clientId);

    EventQuery user(String userId);

    EventQuery fromDate(Date fromDate);

    EventQuery toDate(Date toDate);

    EventQuery ipAddress(String ipAddress);

    EventQuery firstResult(int result);

    EventQuery maxResults(int results);

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
