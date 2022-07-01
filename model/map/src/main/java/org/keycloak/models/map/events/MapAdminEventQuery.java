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

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEvent.SearchableFields;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
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
import static org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator.LIKE;
import static org.keycloak.models.map.storage.QueryParameters.Order.DESCENDING;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapAdminEventQuery implements AdminEventQuery {

    private Integer firstResult;
    private Integer maxResults;
    private DefaultModelCriteria<AdminEvent> mcb = criteria();
    private final Function<QueryParameters<AdminEvent>, Stream<AdminEvent>> resultProducer;

    public MapAdminEventQuery(Function<QueryParameters<AdminEvent>, Stream<AdminEvent>> resultProducer) {
        this.resultProducer = resultProducer;
    }

    @Override
    public AdminEventQuery realm(String realmId) {
        mcb = mcb.compare(SearchableFields.REALM_ID, EQ, realmId);
        return this;
    }

    @Override
    public AdminEventQuery authRealm(String realmId) {
        mcb = mcb.compare(SearchableFields.AUTH_REALM_ID, EQ, realmId);
        return this;
    }

    @Override
    public AdminEventQuery authClient(String clientId) {
        mcb = mcb.compare(SearchableFields.AUTH_CLIENT_ID, EQ, clientId);
        return this;
    }

    @Override
    public AdminEventQuery authUser(String userId) {
        mcb = mcb.compare(SearchableFields.AUTH_USER_ID, EQ, userId);
        return this;
    }

    @Override
    public AdminEventQuery authIpAddress(String ipAddress) {
        mcb = mcb.compare(SearchableFields.AUTH_IP_ADDRESS, EQ, ipAddress);
        return this;
    }

    @Override
    public AdminEventQuery operation(OperationType... operations) {
        mcb = mcb.compare(SearchableFields.OPERATION_TYPE, IN, Arrays.stream(operations));
        return this;
    }

    @Override
    public AdminEventQuery resourceType(ResourceType... resourceTypes) {
        mcb = mcb.compare(SearchableFields.RESOURCE_TYPE, IN, Arrays.stream(resourceTypes));
        return this;
    }

    @Override
    public AdminEventQuery resourcePath(String resourcePath) {
        mcb = mcb.compare(SearchableFields.RESOURCE_PATH, LIKE, resourcePath.replace('*', '%'));
        return this;
    }

    @Override
    public AdminEventQuery fromTime(Date fromTime) {
        mcb = mcb.compare(SearchableFields.TIMESTAMP, GE, fromTime.getTime());
        return this;
    }

    @Override
    public AdminEventQuery toTime(Date toTime) {
        mcb = mcb.compare(SearchableFields.TIMESTAMP, LE, toTime.getTime());
        return this;
    }

    @Override
    public AdminEventQuery firstResult(int first) {
        firstResult = first;
        return this;
    }

    @Override
    public AdminEventQuery maxResults(int max) {
        maxResults = max;
        return this;
    }

    @Override
    public Stream<AdminEvent> getResultStream() {
        return resultProducer.apply(QueryParameters.withCriteria(mcb)
                .offset(firstResult)
                .limit(maxResults)
                .orderBy(SearchableFields.TIMESTAMP, DESCENDING)
        );
    }
}
