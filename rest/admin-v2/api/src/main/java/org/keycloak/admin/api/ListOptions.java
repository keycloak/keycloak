package org.keycloak.admin.api;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    @Parameter(name = "sortBy",
               description = "Field(s) to sort by, comma-separated for multi-field sort (e.g. displayName,clientId). Allowed values: clientId, displayName, description, protocol, enabled, appUrl. Defaults to clientId when omitted.",
               style = ParameterStyle.FORM,
               explode = Explode.FALSE,
               schema = @Schema(type = SchemaType.ARRAY, implementation = ClientField.class))
    @QueryParam("sortBy")
    protected String sortBy;
    
    @Parameter(description = "Sort direction. Allowed values: asc (default), desc.", schema = @Schema(implementation = SortOrder.class))
    @QueryParam("sortOrder")
    protected SortOrder sortOrder;

    @Parameter(name = "fields",
               description = "Set of fields to include in the response. Must be top-level fields. If omitted or empty, all fields will be populated.",
               style = ParameterStyle.FORM,
               explode = Explode.FALSE,
               schema = @Schema(type = SchemaType.ARRAY, implementation = ClientField.class))
    @QueryParam("fields")
    protected String fields;

    @Parameter(description = "Filter expression using SCIM-like syntax, e.g. clientId eq \"my-app\" and enabled eq true")
    @QueryParam("q")
    protected String query;

    public ListOptions query(String query) {
        this.setQuery(query);
        return this;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public void setFields(List<ClientField> fields) {
        this.fields = parseField(fields);
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

    public void setSortBy(ClientField sortBy) {
        this.sortBy = sortBy == null ? null : sortBy.toQueryValue();
    }

    public void setSortBy(List<ClientField> sortBy) {
        this.sortBy = parseField(sortBy);
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    private String parseField(List<ClientField> list) {
        return list == null || list.isEmpty() ? null :
                 list.stream().map(ClientField::toQueryValue).collect(Collectors.joining(","));
    }
}
