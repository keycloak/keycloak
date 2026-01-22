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

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.keycloak.events.EventCountQuery;
import org.keycloak.events.EventType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JpaEventCountQuery implements EventCountQuery {

    private final EntityManager em;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<Long> cq;
    private final Root<EventEntity> root;
    private final ArrayList<Predicate> predicates;

    public JpaEventCountQuery(EntityManager em) {
        this.em = em;

        cb = em.getCriteriaBuilder();
        cq = cb.createQuery(Long.class);
        root = cq.from(EventEntity.class);
        predicates = new ArrayList<>(4);
    }

    @Override
    public EventCountQuery type(EventType... types) {
        List<String> eventStrings = new LinkedList<String>();
        for (EventType e : types) {
            eventStrings.add(e.toString());
        }
        predicates.add(root.get("type").in(eventStrings));
        return this;
    }

    @Override
    public EventCountQuery realm(String realmId) {
        predicates.add(cb.equal(root.get("realmId"), realmId));
        return this;
    }

    @Override
    public EventCountQuery client(String clientId) {
        predicates.add(cb.equal(root.get("clientId"), clientId));
        return this;
    }

    @Override
    public EventCountQuery user(String userId) {
        predicates.add(cb.equal(root.get("userId"), userId));
        return this;
    }

    @Override
    public EventCountQuery fromDate(long fromDate) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("time"), fromDate));
        return this;
    }

    @Override
    public EventCountQuery toDate(long toDate) {
        predicates.add(cb.lessThanOrEqualTo(root.get("time"), toDate));
        return this;
    }

    @Override
    public EventCountQuery ipAddress(String ipAddress) {
        predicates.add(cb.equal(root.get("ipAddress"), ipAddress));
        return this;
    }

    @Override
    public Long getCount() {
        cq.select(cb.count(root));

        if (!predicates.isEmpty()) {
            cq.where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
        }

        TypedQuery<Long> query = em.createQuery(cq);
        return query.getSingleResult();
    }
}
