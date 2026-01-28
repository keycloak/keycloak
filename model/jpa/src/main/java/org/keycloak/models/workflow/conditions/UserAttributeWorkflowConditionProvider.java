package org.keycloak.models.workflow.conditions;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.workflow.ResourceType;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;
import org.keycloak.storage.jpa.JpaHashUtils;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;

public class UserAttributeWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedAttribute;
    private final KeycloakSession session;

    public UserAttributeWorkflowConditionProvider(KeycloakSession session, String expectedAttribute) {
        this.session = session;
        this.expectedAttribute = expectedAttribute;
    }

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(ResourceType.USERS);
    }

    @Override
    public boolean evaluate(WorkflowExecutionContext context) {
        validate();

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, context.getResourceId());

        if (user == null) {
            return false;
        }

        String[] parsedKeyValuePair = parseKeyValuePair(expectedAttribute);
        List<String> values = user.getAttributes().getOrDefault(parsedKeyValuePair[0], List.of());
        List<String> expectedValues = List.of(parsedKeyValuePair[1].split(","));

        return collectionEquals(expectedValues, values);
    }

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, CriteriaQuery<String> query, Root<?> path) {
        validate();

        String[] parsedKeyValuePair = parseKeyValuePair(expectedAttribute);
        String attributeName = parsedKeyValuePair[0];
        List<String> expectedValues = Arrays.asList(parsedKeyValuePair[1].split(","));

        // Subquery to count how many of the expected values the user has
        // to check if there is no missing value
        Subquery<Long> matchingCountSubquery = query.subquery(Long.class);
        Root<UserAttributeEntity> attrRoot1 = matchingCountSubquery.from(UserAttributeEntity.class);
        matchingCountSubquery.select(cb.count(attrRoot1));

        // Build predicate for matching values
        // For values <= 255 chars: compare against 'value' field
        // For values > 255 chars: compare against 'longValueHash' field (to avoid Oracle NCLOB comparison issues)
        Predicate[] valuePredicates = expectedValues.stream()
                .map(expectedValue -> {
                    if (expectedValue.length() > 255) {
                        // Use hash comparison for long values to avoid NCLOB comparison issues in Oracle
                        return cb.equal(attrRoot1.get("longValueHash"), JpaHashUtils.hashForAttributeValue(expectedValue));
                    } else {
                        // For short values, compare directly
                        return cb.equal(attrRoot1.get("value"), expectedValue);
                    }
                })
                .toArray(Predicate[]::new);

        matchingCountSubquery.where(
                cb.and(
                        cb.equal(attrRoot1.get("user").get("id"), path.get("id")),
                        cb.equal(attrRoot1.get("name"), attributeName),
                        cb.or(valuePredicates)
                )
        );

        // Subquery to count total attributes with this name for the user
        // to check if there are no extra values
        Subquery<Long> totalCountSubquery = query.subquery(Long.class);
        Root<UserAttributeEntity> attrRoot2 = totalCountSubquery.from(UserAttributeEntity.class);
        totalCountSubquery.select(cb.count(attrRoot2));
        totalCountSubquery.where(
                cb.and(
                        cb.equal(attrRoot2.get("user").get("id"), path.get("id")),
                        cb.equal(attrRoot2.get("name"), attributeName)
                )
        );

        // Both counts must equal the expected count (exact match)
        int expectedCount = expectedValues.size();
        return cb.and(
                cb.equal(matchingCountSubquery, expectedCount),
                cb.equal(totalCountSubquery, expectedCount)
        );
    }

    @Override
    public void validate() {
        if (expectedAttribute == null) {
            throw new WorkflowInvalidStateException("Expected 'key:value' pair is not set.");
        }
    }

    @Override
    public void close() {

    }

    /**
     * Parses a key-value pair string in the format "key:value" and returns an array containing the key and value. It relies
     * on Properties.load to handle edge cases like escaped colons.
     *
     * @param keyValuePair the key-value pair string to parse
     * @return a {@link String} array where the first element is the key and the second element is the value.
     */
    public static String[] parseKeyValuePair(String keyValuePair) {
        Properties props = new Properties();
        try {
            props.load(new StringReader(keyValuePair));
        } catch (java.io.IOException e) {
            throw new WorkflowInvalidStateException("Error reading key-value pair " + keyValuePair + ". Expected format 'key:value'");
        }
        String key = props.stringPropertyNames().iterator().next();
        String value = props.getProperty(key);
        return new String[]{key, value};
    }
}
