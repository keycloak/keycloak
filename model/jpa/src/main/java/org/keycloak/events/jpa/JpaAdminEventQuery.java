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

package org.keycloak.events.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import org.hibernate.jpa.AvailableHints;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class JpaAdminEventQuery implements AdminEventQuery {
    
    private final EntityManager em;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<AdminEventEntity> cq;
    private final Root<AdminEventEntity> root;
    private final ArrayList<Predicate> predicates;
    private Integer firstResult;
    private Integer maxResults;
    private boolean orderByDescTime = true;

    public JpaAdminEventQuery(EntityManager em) {
        this.em = em;

        cb = em.getCriteriaBuilder();
        cq = cb.createQuery(AdminEventEntity.class);
        root = cq.from(AdminEventEntity.class);
        predicates = new ArrayList<Predicate>();
    }
    
    @Override
    public AdminEventQuery realm(String realmId) {
        predicates.add(cb.equal(root.get("realmId"), realmId));
        return this;
    }

    @Override
    public AdminEventQuery operation(OperationType... operations) {
        List<String> operationStrings = new LinkedList<String>();
        for (OperationType e : operations) {
            operationStrings.add(e.toString());
        }
        predicates.add(root.get("operationType").in(operationStrings));
        return this;
    }

    @Override
    public AdminEventQuery resourceType(ResourceType... resourceTypes) {

        List<String> resourceTypeStrings = new LinkedList<String>();
        for (ResourceType e : resourceTypes) {
            resourceTypeStrings.add(e.toString());
        }
        predicates.add(root.get("resourceType").in(resourceTypeStrings));

        return this;
    }

    @Override
    public AdminEventQuery authRealm(String authRealmId) {
        predicates.add(cb.equal(root.get("authRealmId"), authRealmId));
        return this;
    }

    @Override
    public AdminEventQuery authClient(String authClientId) {
        predicates.add(cb.equal(root.get("authClientId"), authClientId));
        return this;
    }

    @Override
    public AdminEventQuery authUser(String authUserId) {
        predicates.add(cb.equal(root.get("authUserId"), authUserId));
        return this;
    }

    @Override
    public AdminEventQuery authIpAddress(String ipAddress) {
        predicates.add(cb.equal(root.get("authIpAddress"), ipAddress));
        return this;
    }

    @Override
    public AdminEventQuery resourcePath(String resourcePath) {
        Expression<String> rPath = root.get("resourcePath");
        predicates.add(cb.like(rPath, resourcePath.replace('*', '%')));
        return this;
    }

    @Override
    @Deprecated
    public AdminEventQuery fromTime(Date fromTime) {
        return fromTime(fromTime.getTime());
    }

    @Override
    public AdminEventQuery fromTime(long fromTime) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("time"), fromTime));
        return this;
    }

    @Override
    @Deprecated
    public AdminEventQuery toTime(Date toTime) {
        return toTime(toTime.getTime());
    }

    @Override
    public AdminEventQuery toTime(long toTime) {
        predicates.add(cb.lessThanOrEqualTo(root.get("time"), toTime));
        return this;
    }

    @Override
    public AdminEventQuery firstResult(int firstResult) {
        this.firstResult = firstResult;
        return this;
    }

    @Override
    public AdminEventQuery maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public AdminEventQuery orderByDescTime() {
        orderByDescTime = true;
        return this;
    }

    @Override
    public AdminEventQuery orderByAscTime() {
        orderByDescTime = false;
        return this;
    }

    @Override
    public Stream<AdminEvent> getResultStream() {
        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }

        if (orderByDescTime) {
            cq.orderBy(cb.desc(root.get("time")));
        } else {
            cq.orderBy(cb.asc(root.get("time")));
        }

        TypedQuery<AdminEventEntity> query = em.createQuery(cq);
        query.setHint(AvailableHints.HINT_READ_ONLY, true);

        return closing(paginateQuery(query, firstResult, maxResults).getResultStream().map(JpaEventStoreProvider::convertAdminEvent));
    }
    
}
