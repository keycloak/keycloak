package org.keycloak.client.admin.cli.v2;

import java.util.List;

/**
 * Compact descriptor for v2 CLI commands.
 * Produced at build time from OpenAPI spec, cached per-server at runtime.
 * Deserialized with Jackson — no SmallRye needed on the read path.
 */
public class KcAdmV2CommandDescriptor {

    private String version;
    private List<ResourceDescriptor> resources;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ResourceDescriptor> getResources() {
        return resources;
    }

    public void setResources(List<ResourceDescriptor> resources) {
        this.resources = resources;
    }

    public static class ResourceDescriptor {
        private String name;
        private List<CommandDescriptor> commands;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<CommandDescriptor> getCommands() {
            return commands;
        }

        public void setCommands(List<CommandDescriptor> commands) {
            this.commands = commands;
        }
    }

    public static class CommandDescriptor {
        private String name;
        private String resourceName;
        private String httpMethod;
        private String path;
        private String description;
        private boolean requiresId;
        private boolean hasResponseBody = true;
        private List<OptionDescriptor> options;
        private List<VariantDescriptor> variants;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequiresId() {
            return requiresId;
        }

        public void setRequiresId(boolean requiresId) {
            this.requiresId = requiresId;
        }

        public boolean isHasResponseBody() {
            return hasResponseBody;
        }

        public void setHasResponseBody(boolean hasResponseBody) {
            this.hasResponseBody = hasResponseBody;
        }

        public List<OptionDescriptor> getOptions() {
            return options;
        }

        public void setOptions(List<OptionDescriptor> options) {
            this.options = options;
        }

        public List<VariantDescriptor> getVariants() {
            return variants;
        }

        public void setVariants(List<VariantDescriptor> variants) {
            this.variants = variants;
        }
    }

    /**
     * Represents a protocol-specific variant of a command (e.g., "oidc" vs "saml" for client create/patch).
     * Derived from the OpenAPI schema discriminator — each variant becomes a CLI subcommand
     * with its own set of options (base + protocol-specific fields merged).
     */
    public static class VariantDescriptor {
        private String name;
        private String discriminatorField;
        private String discriminatorValue;
        private List<OptionDescriptor> options;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDiscriminatorField() {
            return discriminatorField;
        }

        public void setDiscriminatorField(String discriminatorField) {
            this.discriminatorField = discriminatorField;
        }

        public String getDiscriminatorValue() {
            return discriminatorValue;
        }

        public void setDiscriminatorValue(String discriminatorValue) {
            this.discriminatorValue = discriminatorValue;
        }

        public List<OptionDescriptor> getOptions() {
            return options;
        }

        public void setOptions(List<OptionDescriptor> options) {
            this.options = options;
        }
    }

    /**
     * Maps an OpenAPI schema property to a CLI option.
     * {@code fieldName} is the JSON property name (e.g., "clientId") used when building the request body,
     * {@code name} is the kebab-case CLI flag (e.g., "client-id").
     */
    public static class OptionDescriptor {
        public static final String TYPE_BOOLEAN = "boolean";
        public static final String TYPE_STRING = "string";

        private String name;
        private String fieldName;
        private String type;
        private String description;
        private boolean array;
        private List<String> enumValues;
        private String parentFieldName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isArray() {
            return array;
        }

        public void setArray(boolean array) {
            this.array = array;
        }

        public List<String> getEnumValues() {
            return enumValues;
        }

        public void setEnumValues(List<String> enumValues) {
            this.enumValues = enumValues;
        }

        public String getParentFieldName() {
            return parentFieldName;
        }

        public void setParentFieldName(String parentFieldName) {
            this.parentFieldName = parentFieldName;
        }
    }
}
