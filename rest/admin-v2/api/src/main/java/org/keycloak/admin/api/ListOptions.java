package org.keycloak.admin.api;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    @Parameter(description = "Set of fields to include in the response. Must be top-level fields. If omitted or empty, all fields will be populated.", 
            explode = Explode.FALSE, schema = @Schema(type = SchemaType.ARRAY, uniqueItems = true, implementation = String.class))
    @QueryParam("fields")
    protected String fields;

    @Parameter(description = "Filter expression using SCIM-like syntax, e.g. clientId eq \"my-app\" and enabled eq true")
    @QueryParam("q")
    protected String query;

    @Parameter(description = "Maximum number of results to return. Defaults to 100.")
    @QueryParam("limit")
    protected Integer limit;

    @Parameter(description = "Index of the first result to return, counted from 0. Defaults to 0.")
    @QueryParam("offset")
    protected Integer offset;

    public ListOptions fields(Set<String> fields) {
        this.setFields(fields);
        return this;
    }

    public ListOptions query(String query) {
        this.setQuery(query);
        return this;
    }

    public ListOptions limit(int limit) {
        this.setLimit(limit);
        return this;
    }

    public ListOptions offset(int offset) {
        this.setOffset(offset);
        return this;
    }

    public Set<String> getFields() {
        if (fields == null) {
            return null;
        }
        if (fields.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(List.of(fields.split(",")));
    }

    public void setFields(Set<String> fields) {
        this.fields = fields == null ? null : fields.stream().collect(Collectors.joining(","));
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
    
}
