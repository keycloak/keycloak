/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.events;

import org.keycloak.events.Event;
import org.keycloak.events.Event.SearchableFields;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventType;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Arrays;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.EQ;
import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.GE;
import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.IN;
import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.LE;
import static org.keycloak.models.map.storage.QueryParameters.Order.DESCENDING;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapAuthEventQuery implements EventQuery {

    private Integer firstResult;
    private Integer maxResults;
    private DefaultModelCriteria<Event> mcb = criteria();
    private final Function<QueryParameters<Event>, Stream<Event>> resultProducer;

    public MapAuthEventQuery(Function<QueryParameters<Event>, Stream<Event>> resultProducer) {
        this.resultProducer = resultProducer;
    }

    @Override
    public EventQuery type(EventType... types) {
        mcb = mcb.compare(SearchableFields.EVENT_TYPE, IN, Arrays.asList(types));
        return this;
    }

    @Override
    public EventQuery realm(String realmId) {
        mcb = mcb.compare(SearchableFields.REALM_ID, EQ, realmId);
        return this;
    }

    @Override
    public EventQuery client(String clientId) {
        mcb = mcb.compare(SearchableFields.CLIENT_ID, EQ, clientId);
        return this;
    }

    @Override
    public EventQuery user(String userId) {
        mcb = mcb.compare(SearchableFields.USER_ID, EQ, userId);
        return this;
    }

    @Override
    public EventQuery fromDate(Date fromDate) {
        mcb = mcb.compare(SearchableFields.TIMESTAMP, GE, fromDate.getTime());
        return this;
    }

    @Override
    public EventQuery toDate(Date toDate) {
        mcb = mcb.compare(SearchableFields.TIMESTAMP, LE, toDate.getTime());
        return this;
    }

    @Override
    public EventQuery ipAddress(String ipAddress) {
        mcb = mcb.compare(SearchableFields.IP_ADDRESS, EQ, ipAddress);
        return this;
    }

    @Override
    public EventQuery firstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public EventQuery maxResults(int max) {
        this.maxResults = max;
        return this;
    }

    @Override
    public Stream<Event> getResultStream() {
        return resultProducer.apply(QueryParameters.withCriteria(mcb)
                .offset(firstResult)
                .limit(maxResults)
                .orderBy(SearchableFields.TIMESTAMP, DESCENDING));
    }
}
