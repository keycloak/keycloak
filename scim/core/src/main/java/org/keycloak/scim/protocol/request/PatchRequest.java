package org.keycloak.scim.protocol.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatchRequest {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:PatchOp";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("Operations")
    private List<PatchOperation> operations;

    public PatchRequest() {
        // reflection
    }

    public PatchRequest(List<PatchOperation> operations) {
        this.operations = operations;
    }

    public static Builder create() {
        return new Builder();
    }

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
        private JsonNode value;

        public PatchOperation() {
            // reflection
        }

        public PatchOperation(String op, String path, String value) {
            this.op = op;
            this.path = path;
            if (value == null) {
                this.value = null;
            } else {
                try {
                    if (value.startsWith("{") || value.startsWith("[")) {
                        this.value = JsonSerialization.readValue(value, JsonNode.class);
                    } else {
                        this.value = new TextNode(value);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

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

        public JsonNode getValue() {
            return value;
        }

        public void setValue(JsonNode value) {
            this.value = value;
        }
    }

    public static class Builder {

        private final List<PatchOperation> operations = new ArrayList<>();

        public Builder add(String path, String value) {
            operation("add", path, value);
            return this;
        }

        public Builder add(String value) {
            operation("add", null, value);
            return this;
        }

        public Builder replace(String path, String value) {
            operation("replace", path, value);
            return this;
        }

        public Builder replace(String value) {
            replace(null, value);
            return this;
        }

        public Builder remove(String path) {
            operation("remove", path, null);
            return this;
        }

        private void operation(String operation, String path, String value) {
            operations.add(new PatchOperation(operation, path, value));
        }

        public PatchRequest build() {
            return new PatchRequest(operations);
        }
    }
}
