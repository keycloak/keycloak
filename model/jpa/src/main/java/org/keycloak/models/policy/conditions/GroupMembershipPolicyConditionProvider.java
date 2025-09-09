package org.keycloak.models.policy.conditions;

import java.util.List;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.policy.ResourcePolicyConditionProvider;
import org.keycloak.models.policy.ResourcePolicyEvent;
import org.keycloak.models.policy.ResourcePolicyInvalidStateException;
import org.keycloak.models.policy.ResourceType;

public class GroupMembershipPolicyConditionProvider implements ResourcePolicyConditionProvider {

    private final List<String> expectedGroups;
    private final KeycloakSession session;

    public GroupMembershipPolicyConditionProvider(KeycloakSession session, List<String> expectedGroups) {
        this.session = session;
        this.expectedGroups = expectedGroups;;
    }

    @Override
    public boolean evaluate(ResourcePolicyEvent event) {
        if (!ResourceType.USERS.equals(event.getResourceType())) {
            return false;
        }

        validate();

        String userId = event.getResourceId();
        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);

        for (String expectedGroup : expectedGroups) {
            GroupModel group = session.groups().getGroupById(realm, expectedGroup);

            if (user.isMemberOf(group)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void validate() {
        expectedGroups.forEach(id -> {
            if (session.groups().getGroupById(session.getContext().getRealm(), id) == null) {
                throw new ResourcePolicyInvalidStateException(String.format("Group with id %s does not exist.", id));
            }
        });
    }

    @Override
    public void close() {

    }
}
