package org.keycloak.scim.resource.config;

import java.util.List;
import java.util.Set;

import org.keycloak.scim.resource.ResourceTypeRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceProviderConfig extends ResourceTypeRepresentation {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";

    @JsonProperty("documentationUri")
    private String documentationUri;

    @JsonProperty("patch")
    private Supported patch;

    @JsonProperty("bulk")
    private BulkSupport bulk;

    @JsonProperty("filter")
    private FilterSupport filter;

    @JsonProperty("changePassword")
    private Supported changePassword;

    @JsonProperty("sort")
    private Supported sort;

    @JsonProperty("etag")
    private Supported etag;

    @JsonProperty("authenticationSchemes")
    private List<AuthenticationScheme> authenticationSchemes;

    @Override
    public Set<String> getSchemas() {
        return Set.of(SCHEMA);
    }

    public String getDocumentationUri() {
        return documentationUri;
    }

    public void setDocumentationUri(String documentationUri) {
        this.documentationUri = documentationUri;
    }

    public Supported getPatch() {
        return patch;
    }

    public void setPatch(Supported patch) {
        this.patch = patch;
    }

    public BulkSupport getBulk() {
        return bulk;
    }

    public void setBulk(BulkSupport bulk) {
        this.bulk = bulk;
    }

    public FilterSupport getFilter() {
        return filter;
    }

    public void setFilter(FilterSupport filter) {
        this.filter = filter;
    }

    public Supported getChangePassword() {
        return changePassword;
    }

    public void setChangePassword(Supported changePassword) {
        this.changePassword = changePassword;
    }

    public Supported getSort() {
        return sort;
    }

    public void setSort(Supported sort) {
        this.sort = sort;
    }

    public Supported getEtag() {
        return etag;
    }

    public void setEtag(Supported etag) {
        this.etag = etag;
    }

    public List<AuthenticationScheme> getAuthenticationSchemes() {
        return authenticationSchemes;
    }

    public void setAuthenticationSchemes(List<AuthenticationScheme> authenticationSchemes) {
        this.authenticationSchemes = authenticationSchemes;
    }

    /**
     * Generic supported feature indicator
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Supported {

        public static final Supported TRUE = new Supported(true);
        public static final Supported FALSE = new Supported(false);

        @JsonProperty("supported")
        private Boolean supported;

        public Supported(boolean supported) {
            this.supported = supported;
        }

        public Supported() {
            this.supported = false;
        }

        public Boolean getSupported() {
            return supported;
        }

        public void setSupported(Boolean supported) {
            this.supported = supported;
        }
    }

    /**
     * Bulk operation support configuration
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkSupport extends Supported {
        @JsonProperty("maxOperations")
        private Integer maxOperations = 0;

        @JsonProperty("maxPayloadSize")
        private Integer maxPayloadSize = 0;

        public Integer getMaxOperations() {
            return maxOperations;
        }

        public void setMaxOperations(Integer maxOperations) {
            this.maxOperations = maxOperations;
        }

        public Integer getMaxPayloadSize() {
            return maxPayloadSize;
        }

        public void setMaxPayloadSize(Integer maxPayloadSize) {
            this.maxPayloadSize = maxPayloadSize;
        }
    }

    /**
     * Filter support configuration
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FilterSupport extends Supported {
        @JsonProperty("maxResults")
        private Integer maxResults = 0;

        public Integer getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(Integer maxResults) {
            this.maxResults = maxResults;
        }
    }

    /**
     * Authentication scheme details
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AuthenticationScheme {
        @JsonProperty("type")
        private String type;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("specUri")
        private String specUri;

        @JsonProperty("documentationUri")
        private String documentationUri;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getSpecUri() {
            return specUri;
        }

        public void setSpecUri(String specUri) {
            this.specUri = specUri;
        }

        public String getDocumentationUri() {
            return documentationUri;
        }

        public void setDocumentationUri(String documentationUri) {
            this.documentationUri = documentationUri;
        }
    }
}
