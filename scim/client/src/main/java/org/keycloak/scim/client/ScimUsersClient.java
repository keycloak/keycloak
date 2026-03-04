package org.keycloak.scim.client;

import org.keycloak.scim.protocol.response.ListResponse;
import org.keycloak.scim.resource.user.User;

import static java.util.Objects.requireNonNull;

import static org.keycloak.scim.client.ResourceFilter.filter;

public class ScimUsersClient extends AbstractScimResourceClient<User> {

    public ScimUsersClient(ScimClient client) {
        super(client, User.class);
    }

    /**
     * Get all users without filtering.
     *
     * @return list response containing all users
     */
    public ListResponse<User> getAll() {
        return doFilter(filter());
    }

    /**
     * Get all users matching the specified filter.
     *
     * @param filterExpression SCIM filter expression (e.g., "userName eq \"john\"")
     * @return list response containing matching users
     */
    public ListResponse<User> getAll(String filterExpression) {
        requireNonNull(filterExpression, "filterExpression must not be null");
        return doFilter(new ResourceFilter() {
            @Override
            public String build() {
                return filterExpression;
            }
        });
    }

    /**
     * Search for users using the POST /.search endpoint.
     * This is useful for complex filters that may exceed URL length limits.
     *
     * @param filterExpression SCIM filter expression (e.g., "userName eq \"john\"")
     * @return list response containing matching users
     */
    public ListResponse<User> search(String filterExpression) {
        return this.search(filterExpression, null, null);
    }

    /**
     * Search for users using the POST /.search endpoint.
     * This is useful for complex filters that may exceed URL length limits.
     *
     * @param filterExpression SCIM filter expression (e.g., "userName eq \"john\"")
     * @param startIndex      optional index of the first result to return (for pagination)
     *                        if null, the server will use its default value (usually 1)
     * @param count           optional maximum number of results to return (for pagination)
     *                        if null, the server will use its default value
     * @return list response containing matching users
     */
    @SuppressWarnings("unchecked")
    public ListResponse<User> search(String filterExpression, Integer startIndex, Integer count) {
        return doPost(filterExpression, startIndex, count);
    }

    @Override
    public void close() throws Exception {

    }
}
