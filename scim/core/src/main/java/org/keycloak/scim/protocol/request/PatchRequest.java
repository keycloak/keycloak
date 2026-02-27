package org.keycloak.scim.protocol.request;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatchRequest {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("Operations")
    private List<PatchOperation> operations;

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public List<PatchOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<PatchOperation> operations) {
        this.operations = operations;
    }

    /**
     * Represents a single PATCH operation
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PatchOperation {
        @JsonProperty("op")
        private String op; // "add", "remove", "replace"

        @JsonProperty("path")
        private String path;

        @JsonProperty("value")
        private Object value;

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
