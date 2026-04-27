package org.keycloak.scim.resource.schema;

import java.util.List;
import java.util.Set;

import org.keycloak.scim.resource.ResourceTypeRepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Schema extends ResourceTypeRepresentation {

    public static final String SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Schema";

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("attributes")
    private List<Attribute> attributes;

    @Override
    public Set<String> getSchemas() {
        return Set.of(SCHEMA);
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

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * Represents a schema attribute definition
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Attribute {
        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        @JsonProperty("multiValued")
        private Boolean multiValued;

        @JsonProperty("description")
        private String description;

        @JsonProperty("required")
        private Boolean required;

        @JsonProperty("canonicalValues")
        private List<String> canonicalValues;

        @JsonProperty("caseExact")
        private Boolean caseExact;

        @JsonProperty("mutability")
        private String mutability;

        @JsonProperty("returned")
        private String returned;

        @JsonProperty("uniqueness")
        private String uniqueness;

        @JsonProperty("subAttributes")
        private List<Attribute> subAttributes;

        @JsonProperty("referenceTypes")
        private List<String> referenceTypes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getMultiValued() {
            return multiValued;
        }

        public void setMultiValued(Boolean multiValued) {
            this.multiValued = multiValued;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public List<String> getCanonicalValues() {
            return canonicalValues;
        }

        public void setCanonicalValues(List<String> canonicalValues) {
            this.canonicalValues = canonicalValues;
        }

        public Boolean getCaseExact() {
            return caseExact;
        }

        public void setCaseExact(Boolean caseExact) {
            this.caseExact = caseExact;
        }

        public String getMutability() {
            return mutability;
        }

        public void setMutability(String mutability) {
            this.mutability = mutability;
        }

        public String getReturned() {
            return returned;
        }

        public void setReturned(String returned) {
            this.returned = returned;
        }

        public String getUniqueness() {
            return uniqueness;
        }

        public void setUniqueness(String uniqueness) {
            this.uniqueness = uniqueness;
        }

        public List<Attribute> getSubAttributes() {
            return subAttributes;
        }

        public void setSubAttributes(List<Attribute> subAttributes) {
            this.subAttributes = subAttributes;
        }

        public List<String> getReferenceTypes() {
            return referenceTypes;
        }

        public void setReferenceTypes(List<String> referenceTypes) {
            this.referenceTypes = referenceTypes;
        }
    }
}
