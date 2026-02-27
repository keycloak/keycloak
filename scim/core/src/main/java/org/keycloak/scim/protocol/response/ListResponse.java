package org.keycloak.scim.protocol.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.scim.resource.ResourceTypeRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListResponse<T extends ResourceTypeRepresentation> {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:ListResponse";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("totalResults")
    private Integer totalResults;

    @JsonProperty("Resources")
    @JsonDeserialize(using = ListResponseDeserializer.class)
    private List<T> resources;

    @JsonProperty("startIndex")
    private Integer startIndex;

    @JsonProperty("itemsPerPage")
    private Integer itemsPerPage;

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public Integer getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(Integer totalResults) {
        this.totalResults = totalResults;
    }

    public List<T> getResources() {
        return resources;
    }

    public void setResources(List<T> resources) {
        this.resources = resources;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public Integer getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(Integer itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public void addResource(T resource) {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.add(resource);
    }
}
