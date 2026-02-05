package org.keycloak.scim.client;

import java.util.List;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;

import static java.util.Objects.requireNonNull;

import static org.keycloak.scim.client.ResourceFilter.filter;

public class ScimUsersClient extends AbstractScimResourceClient<User> {

    public ScimUsersClient(ScimClient client) {
        super(client, User.class);
    }

    public User getByUsername(String userName) {
        requireNonNull(userName, "userName must not be null");

        ListResponse<User> r = doFilter(filter().eq("userName", userName));
        List<User> resources = r.getResources();

        if (resources.isEmpty()) {
            return null;
        }

        if (resources.size() > 1) {
            throw new IllegalStateException("More than one user with username " + userName + " found");
        }

        return resources.get(0);
    }

    @Override
    public void close() throws Exception {

    }
}
