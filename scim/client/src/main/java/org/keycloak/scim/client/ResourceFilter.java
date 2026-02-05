package org.keycloak.scim.client;

public class ResourceFilter {

    public static ResourceFilter filter() {
        return new ResourceFilter();
    }

    private StringBuilder filter = new StringBuilder();

    public ResourceFilter eq(String property, String value) {
        filter.append(property).append(" ").append("eq").append(" ").append("\"").append(value).append("\"");
        return this;
    }

    public String build() {
        return filter.toString();
    }
}
