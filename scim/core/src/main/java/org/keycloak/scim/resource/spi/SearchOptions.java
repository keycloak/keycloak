package org.keycloak.scim.resource.spi;

import java.util.List;
import java.util.Optional;

import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.utils.StringUtil;

public class SearchOptions {
    
    private List<String> attributes;

    private List<String> excludedAttributes;
    
    private String filter;

    private List<SortField> sort;

    private Integer startIndex;

    private Integer count;

    private ScimFilterParser.FilterContext filterContext;

    // Getters and setters

    public List<String> getAttributes() {
        return Optional.ofNullable(attributes).orElse(List.of());
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public List<String> getExcludedAttributes() {
        return Optional.ofNullable(excludedAttributes).orElse(List.of());
    }

    public void setExcludedAttributes(List<String> excludedAttributes) {
        this.excludedAttributes = excludedAttributes;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
        this.filterContext = null;
    }

    public ScimFilterParser.FilterContext getFilterContext() {
        if (filterContext == null && StringUtil.isNotBlank(filter)) {
            filterContext = FilterUtils.parseFilter(filter);
        }
        return filterContext;
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
    
    public List<SortField> getSort() {
        return Optional.ofNullable(sort).orElse(List.of());
    }
    
    public void setSort(List<SortField> sort) {
        this.sort = sort;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final SearchOptions searchRequest;

        private Builder() {
            this.searchRequest = new SearchOptions();
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

        public Builder withSort(List<SortField> sort) {
            searchRequest.setSort(sort);
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

        public SearchOptions build() {
            return searchRequest;
        }
    }
}
