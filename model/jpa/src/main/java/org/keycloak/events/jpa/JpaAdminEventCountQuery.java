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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.keycloak.events.admin.AdminEventCountQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JpaAdminEventCountQuery implements AdminEventCountQuery {

    private final EntityManager em;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<Long> cq;
    private final Root<AdminEventEntity> root;
    private final ArrayList<Predicate> predicates;

    public JpaAdminEventCountQuery(EntityManager em) {
        this.em = em;

        cb = em.getCriteriaBuilder();
        cq = cb.createQuery(Long.class);
        root = cq.from(AdminEventEntity.class);
        predicates = new ArrayList<Predicate>();
    }

    @Override
    public AdminEventCountQuery realm(String realmId) {
        predicates.add(cb.equal(root.get("realmId"), realmId));
        return this;
    }

    @Override
    public AdminEventCountQuery operation(OperationType... operations) {
        List<String> operationStrings = new LinkedList<String>();
        for (OperationType e : operations) {
            operationStrings.add(e.toString());
        }
        predicates.add(root.get("operationType").in(operationStrings));
        return this;
    }

    @Override
    public AdminEventCountQuery resourceType(ResourceType... resourceTypes) {

        List<String> resourceTypeStrings = new LinkedList<String>();
        for (ResourceType e : resourceTypes) {
            resourceTypeStrings.add(e.toString());
        }
        predicates.add(root.get("resourceType").in(resourceTypeStrings));

        return this;
    }

    @Override
    public AdminEventCountQuery authRealm(String authRealmId) {
        predicates.add(cb.equal(root.get("authRealmId"), authRealmId));
        return this;
    }

    @Override
    public AdminEventCountQuery authClient(String authClientId) {
        predicates.add(cb.equal(root.get("authClientId"), authClientId));
        return this;
    }

    @Override
    public AdminEventCountQuery authUser(String authUserId) {
        predicates.add(cb.equal(root.get("authUserId"), authUserId));
        return this;
    }

    @Override
    public AdminEventCountQuery authIpAddress(String ipAddress) {
        predicates.add(cb.equal(root.get("authIpAddress"), ipAddress));
        return this;
    }

    @Override
    public AdminEventCountQuery resourcePath(String resourcePath) {
        Expression<String> rPath = root.get("resourcePath");
        predicates.add(cb.like(rPath, resourcePath.replace('*', '%')));
        return this;
    }

    @Override
    public AdminEventCountQuery fromTime(long fromTime) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("time"), fromTime));
        return this;
    }

    @Override
    public AdminEventCountQuery toTime(long toTime) {
        predicates.add(cb.lessThanOrEqualTo(root.get("time"), toTime));
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
