package org.keycloak.admin.api;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    @Parameter(description = "Field(s) to sort by. Allowed values: clientId, displayName, description, protocol, enabled, appUrl. Defaults to clientId when omitted.",
               style = org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle.FORM,
               explode = org.eclipse.microprofile.openapi.annotations.enums.Explode.FALSE,
               schema = @Schema(type = SchemaType.ARRAY, implementation = ClientSortField.class))
    @QueryParam("sortBy")
    protected List<String> sortBy;
    
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

    public List<String> getSortBy() {
        return sortBy;
    }

    public void setSortBy(List<String> sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }
}
