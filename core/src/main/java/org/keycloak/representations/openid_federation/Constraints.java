package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Constraints {

    @JsonProperty("max_path_length")
    private Integer maxPathLength;
    @JsonProperty("naming_constraints")
    private NamingConstraints namingConstraints;
    @JsonProperty("allowed_entity_types")
    private List<String> allowedEntityTypes;

    public Integer getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(Integer maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public NamingConstraints getNamingConstraints() {
        return namingConstraints;
    }

    public void setNamingConstraints(NamingConstraints namingConstraints) {
        this.namingConstraints = namingConstraints;
    }

    public List<String> getAllowedEntityTypes() {
        return allowedEntityTypes;
    }

    public void setAllowedEntityTypes(List<String> allowedEntityTypes) {
        this.allowedEntityTypes = allowedEntityTypes;
    }
}
