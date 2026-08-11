package org.keycloak.models.workflow.conditions;

import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.ClientAttributeEntity;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;

import org.hibernate.Session;

import static org.keycloak.models.workflow.conditions.UserAttributeWorkflowConditionProvider.parseKeyValuePair;

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

        String[] parsedKeyValuePair = parseKeyValuePair(expectedAttribute);
        String key = parsedKeyValuePair[0];
        String valuePart = parsedKeyValuePair[1];

        // Presence-only: "key" -> true if the client has the attribute, regardless of its value
        if (valuePart.isEmpty()) {
            return client.getAttributes().containsKey(key);
        }

        // Client attributes are single-valued, so the expected value is compared literally
        return Objects.equals(valuePart, client.getAttribute(key));
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path) {
        validate();

        String[] parsedKeyValuePair = parseKeyValuePair(expectedAttribute);
        String attributeName = parsedKeyValuePair[0];
        String valuePart = parsedKeyValuePair[1];

        // Subquery to find if an attribute with this name (and value, if one is expected) exists for the client
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<ClientAttributeEntity> attrRoot = subquery.from(ClientAttributeEntity.class);
        subquery.select(cb.literal(1));

        Predicate clientPredicate = cb.equal(attrRoot.get("client").get("id"), path.get("id"));
        Predicate namePredicate = cb.equal(attrRoot.get("name"), attributeName);

        // Presence-only: require the attribute to exist for the client, regardless of its value
        if (valuePart.isEmpty()) {
            subquery.where(cb.and(clientPredicate, namePredicate));
            return cb.exists(subquery);
        }

        subquery.where(cb.and(clientPredicate, namePredicate, createValuePredicate(cb, attrRoot, valuePart)));
        return cb.exists(subquery);
    }

    private Predicate createValuePredicate(CriteriaBuilder cb, Root<ClientAttributeEntity> attrRoot, String expectedValue) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        //noinspection resource
        String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());

        if (dbProductName.equals("Oracle")) {
            // Oracle is not able to compare a CLOB with a VARCHAR unless it being converted with TO_CHAR
            // But for this all values in the table need to be smaller than 4K, otherwise the cast will fail with
            // "ORA-22835: Buffer too small for CLOB to CHAR" (even if it is in another row).
            // This leaves DBMS_LOB.COMPARE as the option to compare the CLOB with the value.
            return cb.equal(cb.function("DBMS_LOB.COMPARE", Integer.class, attrRoot.get("value"), cb.literal(expectedValue)), 0);
        } else if (dbProductName.equals("PostgreSQL")) {
            // use the substr comparison and the full comparison in postgresql
            return cb.and(
                    cb.equal(
                            cb.function("substr", Integer.class, attrRoot.get("value"), cb.literal(1), cb.literal(255)),
                            cb.function("substr", Integer.class, cb.literal(expectedValue), cb.literal(1), cb.literal(255))),
                    cb.equal(attrRoot.get("value"), expectedValue));
        }

        return cb.equal(attrRoot.get("value"), expectedValue);
    }

    @Override
    public void validate() {
        if (expectedAttribute == null) {
            throw new WorkflowInvalidStateException("workflowConditionAttributeNotSet");
        }
    }

    @Override
    public void close() {

    }
}
