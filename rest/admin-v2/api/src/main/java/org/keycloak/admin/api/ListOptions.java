package org.keycloak.admin.api;

import java.util.Arrays;
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

    @Parameter(description = "Sort expression. Comma-separated fields with optional direction per field using | (e.g. displayName|desc,clientId). Default direction is asc.",
            schema = @Schema(type = SchemaType.STRING, defaultValue = "clientId"))
    @QueryParam("sort")
    protected String sort;

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

    public ListOptions sort(List<SortOption> sort) {
        this.setSort(sort);
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

    public List<SortOption> getSort() {
        if (sort == null) {
            return null;
        }
        if (sort.isEmpty()) {
            return List.of();
        }
        List<SortOption> options = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .map(ListOptions::parseSortSegment)
                .collect(Collectors.toList());
        if (options.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        return options;
    }

    public void setSort(List<SortOption> sort) {
        if (sort == null) {
            this.sort = null;
        } else if (sort.isEmpty()) {
            this.sort = "";
        } else {
            this.sort = sort.stream().map(SortOption::toQuerySegment).collect(Collectors.joining(","));
        }
    }

    private static SortOption parseSortSegment(String segment) {
        String[] parts = segment.split("\\|", 2);
        String fieldName = parts[0].trim();
        if (fieldName.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        ClientField field = ClientField.fromApiName(fieldName).orElseThrow(() ->
                new IllegalArgumentException(String.format("%s is not a sortable field", fieldName)));
        SortOrder order = parts.length == 1 ? SortOrder.ASC : parseSortOrder(parts[1].trim());
        return SortOption.of(field, order);
    }

    private static SortOrder parseSortOrder(String value) {
        if (value.isEmpty()) {
            return SortOrder.ASC;
        }
        for (SortOrder order : SortOrder.values()) {
            if (order.name().equalsIgnoreCase(value)) {
                return order;
            }
        }
        throw new IllegalArgumentException("sort direction must be asc or desc");
    }
}
