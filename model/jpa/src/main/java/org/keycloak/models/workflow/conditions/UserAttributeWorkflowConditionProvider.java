package org.keycloak.models.workflow.conditions;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.workflow.WorkflowConditionProvider;
import org.keycloak.models.workflow.WorkflowEvent;
import org.keycloak.models.workflow.ResourceType;

public class UserAttributeWorkflowConditionProvider implements WorkflowConditionProvider {

    private final Map<String, List<String>> expectedAttributes;
    private final KeycloakSession session;

    public UserAttributeWorkflowConditionProvider(KeycloakSession session, Map<String, List<String>> expectedAttributes) {
        this.session = session;
        this.expectedAttributes = expectedAttributes;;
    }

    @Override
    public boolean evaluate(WorkflowEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);

        if (user == null) {
            return false;
        }

        for (Entry<String, List<String>> expected : expectedAttributes.entrySet()) {
            List<String> values = user.getAttributes().getOrDefault(expected.getKey(), List.of());
            List<String> expectedValues = expected.getValue();

            if (!collectionEquals(expectedValues, values)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void validate() {
        // no-op
    }

    @Override
    public void close() {

    }
}
