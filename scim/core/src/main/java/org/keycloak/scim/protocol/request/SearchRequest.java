package org.keycloak.scim.protocol.request;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SCIM Search Request for POST /.search endpoint (RFC 7644 section 3.4.3).
 * This allows clients to submit complex search requests that may exceed URL length limits.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SearchRequest {

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of("urn:ietf:params:scim:api:messages:2.0:SearchRequest");

    @JsonProperty("attributes")
    private List<String> attributes;

    @JsonProperty("excludedAttributes")
    private List<String> excludedAttributes;

    @JsonProperty("filter")
    private String filter;

    @JsonProperty("sortBy")
    private String sortBy;

    @JsonProperty("sortOrder")
    private String sortOrder;

    @JsonProperty("startIndex")
    private Integer startIndex;

    @JsonProperty("count")
    private Integer count;

    // Getters and setters

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    public void setExcludedAttributes(List<String> excludedAttributes) {
        this.excludedAttributes = excludedAttributes;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SearchRequest searchRequest;

        private Builder() {
            this.searchRequest = new SearchRequest();
        }

        public Builder withAttributes(List<String> attributes) {
            searchRequest.setAttributes(attributes);
            return this;
        }

        public Builder withExcludedAttributes(List<String> excludedAttributes) {
            searchRequest.setExcludedAttributes(excludedAttributes);
            return this;
        }

        public Builder withFilter(String filter) {
            searchRequest.setFilter(filter);
            return this;
        }

        public Builder withSortBy(String sortBy) {
            searchRequest.setSortBy(sortBy);
            return this;
        }

        public Builder withSortOrder(String sortOrder) {
            searchRequest.setSortOrder(sortOrder);
            return this;
        }

        public Builder withStartIndex(Integer startIndex) {
            searchRequest.setStartIndex(startIndex);
            return this;
        }

        public Builder withCount(Integer count) {
            searchRequest.setCount(count);
            return this;
        }

        public SearchRequest build() {
            return searchRequest;
        }
    }
}
