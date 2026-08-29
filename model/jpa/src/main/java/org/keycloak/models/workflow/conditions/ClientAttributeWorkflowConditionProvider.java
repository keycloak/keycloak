/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.workflow.conditions;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.hibernate.Session;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientAttributeEntity;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;

public class ClientAttributeWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedAttribute;
    private final KeycloakSession session;

    public ClientAttributeWorkflowConditionProvider(KeycloakSession session, String expectedAttribute) {
        this.session = session;
        this.expectedAttribute = expectedAttribute;
    }

    @Override
    public ResourceType getSupportedResourceType() {
        return ResourceType.CLIENTS;
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        ClientModel client = session.clients().getClientById(realm, context.getResourceId());

        if (client == null) {
            return false;
        }

        String[] parsedKeyValuePair = UserAttributeWorkflowConditionProvider.parseKeyValuePair(expectedAttribute);
        String key = parsedKeyValuePair[0];
        String valuePart = parsedKeyValuePair[1];

        Map<String, String> attributes = client.getAttributes();
        if (attributes == null) {
            return false;
        }

        String actualValue = attributes.get(key);
        if (actualValue == null) {
            return false;
        }

        if (valuePart == null || valuePart.isEmpty()) {
            return true;
        }

        return valuePart.equals(actualValue);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path) {
        validate();

        String[] parsedKeyValuePair = UserAttributeWorkflowConditionProvider.parseKeyValuePair(expectedAttribute);
        String attributeName = parsedKeyValuePair[0];
        String valuePart = parsedKeyValuePair[1];

        if (valuePart == null || valuePart.isEmpty()) {
            return cb.greaterThan(createTotalCountSubquery(cb, query, path, attributeName), 0L);
        }

        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());

        Subquery<Long> matchingCountSubquery = query.subquery(Long.class);
        Root<ClientAttributeEntity> attrRoot = matchingCountSubquery.from(ClientAttributeEntity.class);
        matchingCountSubquery.select(cb.count(attrRoot));

        Predicate valuePredicate;
        if ("Oracle".equals(dbProductName)) {
            valuePredicate = cb.equal(cb.function("DBMS_LOB.COMPARE", Integer.class, attrRoot.get("value"), cb.literal(valuePart)), 0);
        } else if ("PostgreSQL".equals(dbProductName)) {
            Predicate attrValuePredicate1 = cb.equal(
                    cb.function("substr", String.class, attrRoot.get("value"), cb.literal(1), cb.literal(255)),
                    cb.function("substr", String.class, cb.literal(valuePart), cb.literal(1), cb.literal(255)));
            Predicate attrValuePredicate2 = cb.equal(attrRoot.get("value"), valuePart);
            valuePredicate = cb.and(attrValuePredicate1, attrValuePredicate2);
        } else {
            valuePredicate = cb.equal(attrRoot.get("value"), valuePart);
        }

        matchingCountSubquery.where(
                cb.and(
                        cb.equal(attrRoot.get("client").get("id"), path.get("id")),
                        cb.equal(attrRoot.get("name"), attributeName),
                        valuePredicate
                )
        );

        return cb.greaterThan(matchingCountSubquery, 0L);
    }

    private Subquery<Long> createTotalCountSubquery(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path, String attributeName) {
        Subquery<Long> totalCountSubquery = query.subquery(Long.class);
        Root<ClientAttributeEntity> attrRoot = totalCountSubquery.from(ClientAttributeEntity.class);
        totalCountSubquery.select(cb.count(attrRoot));
        totalCountSubquery.where(
                cb.and(
                        cb.equal(attrRoot.get("client").get("id"), path.get("id")),
                        cb.equal(attrRoot.get("name"), attributeName)
                )
        );
        return totalCountSubquery;
    }

    @Override
    public void validate() {
        if (expectedAttribute == null || expectedAttribute.trim().isEmpty()) {
            throw new WorkflowInvalidStateException("workflowConditionAttributeNotSet");
        }
    }

    @Override
    public void close() {
    }
}
