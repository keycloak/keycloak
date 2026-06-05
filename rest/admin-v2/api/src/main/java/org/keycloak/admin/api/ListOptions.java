package org.keycloak.admin.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    @Parameter(description = "Field(s) to sort by, comma-separated for multi-field sort (e.g. displayName,clientId). Allowed values: clientId, displayName, description, protocol, enabled, appUrl. Defaults to clientId when omitted.", schema = @Schema(implementation = ClientSortField.class))
    @QueryParam("sortBy")
    protected String sortBy;
    
    @Parameter(description = "Sort direction. Allowed values: asc (default), desc.", schema = @Schema(implementation = SortOrder.class))
    @QueryParam("sortOrder")
    protected SortOrder sortOrder;

    @Parameter(description = "Set of fields to include in the response. Must be top-level fields. If omitted or empty, all fields will be populated.")
    @QueryParam("fields")
    protected Set<String> fields;

    @Parameter(description = "Filter expression using SCIM-like syntax, e.g. clientId eq \"my-app\" and enabled eq true")
    @QueryParam("q")
    protected String query;

    public ListOptions fields(Set<String> fields) {
        this.setFields(fields);
        return this;
    }

    public ListOptions query(String query) {
        this.setQuery(query);
        return this;
    }

    public Set<String> getFields() {
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public void setSortBy(ClientSortField sortBy) {
        this.sortBy = sortBy == null ? null : sortBy.toQueryValue();
    }

    public void setSortBy(List<ClientSortField> sortBy) {
        this.sortBy = sortBy == null || sortBy.isEmpty()
                ? null
                : sortBy.stream().map(ClientSortField::toQueryValue).collect(Collectors.joining(","));
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
