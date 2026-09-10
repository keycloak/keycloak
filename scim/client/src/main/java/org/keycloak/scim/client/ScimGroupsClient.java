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

    /**
     * Search for groups using the POST /.search endpoint.
     * This is useful for complex filters that may exceed URL length limits.
     *
     * @param filterExpression SCIM filter expression (e.g., "displayName eq \"Engineering\"")
     * @param startIndex      optional index of the first result to return (for pagination)
     *                        if null, the server will use its default value (usually 1)
     * @param count           optional maximum number of results to return (for pagination)
     *                        if null, the server will use its default value
     * @return list response containing matching users
     */
    @SuppressWarnings("unchecked")
    public ListResponse<Group> search(String filterExpression, Integer startIndex, Integer count) {
        return doPost(filterExpression, startIndex, count);
    }


    @Override
    public void close() {
    }
}
