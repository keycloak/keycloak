package org.keycloak.models.policy.conditions;

import static org.keycloak.common.util.CollectionUtil.collectionEquals;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.ResourcePolicyConditionProvider;
import org.keycloak.models.policy.ResourcePolicyEvent;
import org.keycloak.models.policy.ResourceType;

public class UserAttributePolicyConditionProvider implements ResourcePolicyConditionProvider {

    private final Map<String, List<String>> expectedAttributes;
    private final KeycloakSession session;

    public UserAttributePolicyConditionProvider(KeycloakSession session, Map<String, List<String>> expectedAttributes) {
        this.session = session;
        this.expectedAttributes = expectedAttributes;;
    }

    @Override
    public boolean evaluate(ResourcePolicyEvent event) {
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
    public void close() {

    }
}
