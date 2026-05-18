package org.keycloak.admin.api;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    public static class Builder extends ListOptions {

        public Builder withFields(Set<String> fields) {
            this.fields = fields;
            return this;
        }

        public ListOptions build() {
            return this;
        }
    }
    
    public static Builder newListOptionsBuilder() {
        return new Builder();
    }

    @Parameter(description = "Set of fields to include in the response. Must be top-level fields. If omitted or empty, all fields will be populated.")
    @QueryParam("fields")
    protected Set<String> fields;

    public Set<String> getFields() {
        return fields;
    }

}
