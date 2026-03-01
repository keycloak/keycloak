package org.keycloak.scim.client;

import java.util.List;

import org.keycloak.scim.protocol.request.SearchRequest;
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
        return doFilter(filter().pr("userName"));
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
        requireNonNull(filterExpression, "filterExpression must not be null");
        SearchRequest searchRequest = SearchRequest.builder()
                .withFilter(filterExpression)
                .withStartIndex(startIndex)
                .withCount(count).build();
        return client.execute(client.doPost(User.class, "/.search").json(searchRequest), ListResponse.class);
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
