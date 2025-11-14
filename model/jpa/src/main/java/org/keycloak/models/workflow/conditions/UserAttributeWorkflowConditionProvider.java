package org.keycloak.models.workflow.conditions;

import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowExecutionContext;
import org.keycloak.models.workflow.WorkflowInvalidStateException;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;

public class UserAttributeWorkflowConditionProvider implements WorkflowConditionProvider {

    private final String expectedAttribute;
    private final KeycloakSession session;

    public UserAttributeWorkflowConditionProvider(KeycloakSession session, String expectedAttribute) {
        this.session = session;
        this.expectedAttribute = expectedAttribute;
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
