package org.keycloak.scim.client;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.group.Group;

import static java.util.Objects.requireNonNull;

public class ScimGroupsClient extends AbstractScimResourceClient<Group> {

    public ScimGroupsClient(ScimClient client) {
        super(client, Group.class);
    }

    /**
     * Get all groups matching the specified filter.
     *
     * @param filterExpression SCIM filter expression (e.g., "displayName eq \"mygroup\"")
     * @return list response containing matching users
     */
    public ListResponse<Group> getAll(String filterExpression) {
        requireNonNull(filterExpression, "filterExpression must not be null");
        return doFilter(new ResourceFilter() {
            @Override
            public String build() {
                return filterExpression;
            }
        });
    }

    @Override
    public void close() {
    }
}
