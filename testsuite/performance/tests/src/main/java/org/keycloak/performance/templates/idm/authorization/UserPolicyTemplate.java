package org.keycloak.performance.templates.idm.authorization;

import static java.util.stream.Collectors.toSet;
import org.keycloak.performance.dataset.idm.authorization.ResourceServer;
import org.keycloak.performance.dataset.idm.authorization.UserPolicy;
import org.keycloak.performance.iteration.RandomSublist;
import org.keycloak.performance.util.ValidateNumber;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

/**
 *
 * @author tkyjovsk
 */
public class UserPolicyTemplate extends PolicyTemplate<UserPolicy, UserPolicyRepresentation> {

    public static final String USER_POLICIES_PER_RESOURCE_SERVER = "userPoliciesPerResourceServer";
    public static final String USERS_PER_USER_POLICY = "usersPerUserPolicy";

    public final int userPoliciesPerResourceServer;
    public final int usersPerUserPolicy;

    public UserPolicyTemplate(ResourceServerTemplate resourceServerTemplate) {
        super(resourceServerTemplate);
        this.userPoliciesPerResourceServer = getConfiguration().getInt(USER_POLICIES_PER_RESOURCE_SERVER, 0);
        this.usersPerUserPolicy = getConfiguration().getInt(USERS_PER_USER_POLICY, 0);
    }

    @Override
    public int getEntityCountPerParent() {
        return userPoliciesPerResourceServer;
    }

    @Override
    public void validateConfiguration() {
        logger().info(String.format("%s: %s", USER_POLICIES_PER_RESOURCE_SERVER, userPoliciesPerResourceServer));
        ValidateNumber.minValue(userPoliciesPerResourceServer, 0);

        logger().info(String.format("%s: %s", USERS_PER_USER_POLICY, usersPerUserPolicy));
        ValidateNumber.isInRange(usersPerUserPolicy, 0,
                resourceServerTemplate().clientTemplate().realmTemplate().userTemplate.usersPerRealm);
    }

    @Override
    public UserPolicy newEntity(ResourceServer parentEntity, int index) {
        return new UserPolicy(parentEntity, index);
    }

    @Override
    public void processMappings(UserPolicy policy) {
        policy.setUsers(new RandomSublist<>(
                policy.getResourceServer().getClient().getRealm().getUsers(), // original list
                policy.hashCode(), // random seed
                usersPerUserPolicy, // sublist size
                false // unique randoms?
        ));
        policy.getRepresentation().setUsers(policy.getUsers()
                .stream().map(u -> u.getId())
                .filter(id -> id != null) // need non-null policy IDs
                .collect(toSet()));
    }

}
