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
    
    private transient List<SortSegment> parsedSortSegments;

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

    public <F extends SortField> ListOptions sort(List<SortOption<F>> sort) {
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

    /**
     * Parses the raw {@code sort} query parameter into an ordered list of segments
     * (field name + direction), without resolving field names against any
     * resource-specific field set. Use {@link #getSort(SortFieldResolver)} to resolve
     * segments into typed {@link SortOption}s.
     */
    public List<SortSegment> getSortSegments() {
        if (sort == null) {
            return null;
        }
        if (sort.isEmpty()) {
            return List.of();
        }
        if (parsedSortSegments != null) {
            return parsedSortSegments;
        }
        parsedSortSegments = Arrays.stream(sort.split(","))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .map(ListOptions::parseSortSegment)
                .collect(Collectors.toUnmodifiableList());
        if (parsedSortSegments.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        return parsedSortSegments;
    }

    /**
     * Resolves the parsed {@code sort} segments against a resource-specific set of
     * sortable fields.
     *
     * @throws IllegalArgumentException if a segment's field name cannot be resolved by {@code resolver}
     */
    public <F extends SortField> List<SortOption<F>> getSort(SortFieldResolver<F> resolver) {
        List<SortSegment> segments = getSortSegments();
        if (segments == null) {
            return null;
        }
        if (segments.isEmpty()) {
            return List.of();
        }
        return segments.stream()
                .map(segment -> {
                    F field = resolver.resolve(segment.fieldName()).orElseThrow(() ->
                            new IllegalArgumentException(String.format("%s is not a sortable field", segment.fieldName())));
                    return SortOption.of(field, segment.order());
                })
                .collect(Collectors.toUnmodifiableList());
    }

    public <F extends SortField> void setSort(List<SortOption<F>> sort) {
        if (sort == null) {
            parsedSortSegments = null;
            this.sort = null;
        } else if (sort.isEmpty()) {
            parsedSortSegments = List.of();
            this.sort = "";
        } else {
            parsedSortSegments = sort.stream()
                    .map(option -> new SortSegment(option.field().toQueryValue(), option.order()))
                    .collect(Collectors.toUnmodifiableList());
            this.sort = sort.stream().map(SortOption::toQuerySegment).collect(Collectors.joining(","));
        }
    }

    private static SortSegment parseSortSegment(String segment) {
        String[] parts = segment.split("\\|", 2);
        String fieldName = parts[0].trim();
        if (fieldName.isEmpty()) {
            throw new IllegalArgumentException("sort must specify at least one field");
        }
        SortOrder order = parts.length == 1 ? SortOrder.ASC : parseSortOrder(parts[1].trim());
        return new SortSegment(fieldName, order);
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
