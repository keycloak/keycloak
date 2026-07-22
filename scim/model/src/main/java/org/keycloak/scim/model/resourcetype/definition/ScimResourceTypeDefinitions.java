package org.keycloak.scim.model.resourcetype.definition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * Conversion and validation helpers for {@link ScimResourceTypeRepresentation} definitions stored as realm
 * {@link ComponentModel components}.
 */
public final class ScimResourceTypeDefinitions {

    /**
     * Default namespace used to derive a schema URN for a custom resource type when none is provided.
     * The resulting URN has the form {@code urn:keycloak:params:scim:schemas:realm:2.0:<Name>}.
     */
    public static final String DEFAULT_SCHEMA_URN_PREFIX = "urn:keycloak:params:scim:schemas:realm:2.0:";

    public static final String CONFIG_DESCRIPTION = "description";
    public static final String CONFIG_ENDPOINT = "endpoint";
    public static final String CONFIG_SCHEMA = "schema";
    public static final String CONFIG_ATTRIBUTES = "attributes";

    /**
     * Attribute names reserved by the SCIM core schema (RFC 7643) that cannot be redefined by a custom type.
     */
    private static final Set<String> RESERVED_ATTRIBUTE_NAMES = Set.of("id", "externalid", "meta", "schemas");

    private static final Set<String> VALID_TYPES = Set.of(
            "string", "boolean", "decimal", "integer", "datetime", "reference", "complex", "binary");
    private static final Set<String> VALID_MUTABILITY = Set.of("readonly", "readwrite", "immutable", "writeonly");
    private static final Set<String> VALID_UNIQUENESS = Set.of("none", "server", "global");
    private static final Set<String> VALID_RETURNED = Set.of("always", "never", "default", "request");

    private ScimResourceTypeDefinitions() {
    }

    /**
     * Derives the schema URN for a definition, falling back to {@link #DEFAULT_SCHEMA_URN_PREFIX} + name when
     * no schema is explicitly configured.
     */
    public static String resolveSchema(ScimResourceTypeRepresentation definition) {
        if (!isBlank(definition.getSchema())) {
            return definition.getSchema();
        }
        return DEFAULT_SCHEMA_URN_PREFIX + definition.getName();
    }

    /**
     * Derives the endpoint for a definition, falling back to {@code /<name>} when none is configured.
     */
    public static String resolveEndpoint(ScimResourceTypeRepresentation definition) {
        if (!isBlank(definition.getEndpoint())) {
            return definition.getEndpoint();
        }
        return "/" + definition.getName();
    }

    /**
     * Parses a stored component into a definition representation.
     */
    public static ScimResourceTypeRepresentation toRepresentation(ComponentModel model) {
        ScimResourceTypeRepresentation definition = new ScimResourceTypeRepresentation();

        definition.setId(model.getId());
        definition.setName(model.getName());
        definition.setDescription(model.get(CONFIG_DESCRIPTION));
        definition.setEndpoint(model.get(CONFIG_ENDPOINT));
        definition.setSchema(model.get(CONFIG_SCHEMA));
        definition.setAttributes(readAttributes(model.get(CONFIG_ATTRIBUTES)));

        return definition;
    }

    /**
     * Writes a definition representation into a component's name and config, normalizing endpoint and schema.
     */
    public static void writeConfig(ComponentModel model, ScimResourceTypeRepresentation definition) {
        model.setName(definition.getName());

        MultivaluedHashMap<String, String> config = model.getConfig();

        if (config == null) {
            config = new MultivaluedHashMap<>();
            model.setConfig(config);
        }

        putSingle(config, CONFIG_DESCRIPTION, definition.getDescription());
        putSingle(config, CONFIG_ENDPOINT, resolveEndpoint(definition));
        putSingle(config, CONFIG_SCHEMA, resolveSchema(definition));
        putSingle(config, CONFIG_ATTRIBUTES, writeAttributes(definition.getAttributes()));
    }

    /**
     * Validates the structure of a definition. Uniqueness and collision checks against other resource types are
     * performed by the caller, which has access to the realm.
     *
     * @throws ComponentValidationException if the definition is structurally invalid
     */
    public static void validate(ScimResourceTypeRepresentation definition) throws ComponentValidationException {
        if (isBlank(definition.getName())) {
            throw new ComponentValidationException("A resource type name is required");
        }

        String name = definition.getName();

        if (!name.matches("[a-zA-Z][a-zA-Z0-9_-]*")) {
            throw new ComponentValidationException(
                    "Resource type name '" + name + "' must start with a letter and contain only letters, digits, '-' or '_'");
        }

        String endpoint = definition.getEndpoint();

        if (!isBlank(endpoint) && !endpoint.startsWith("/")) {
            throw new ComponentValidationException("Resource type endpoint '" + endpoint + "' must start with '/'");
        }

        List<Schema.Attribute> attributes = definition.getAttributes();

        if (attributes == null) {
            return;
        }

        Set<String> seen = new HashSet<>();

        for (Schema.Attribute attribute : attributes) {
            validateAttribute(attribute, seen);
        }
    }

    private static void validateAttribute(Schema.Attribute attribute, Set<String> seen) {
        String attributeName = attribute.getName();

        if (isBlank(attributeName)) {
            throw new ComponentValidationException("An attribute name is required");
        }

        String lowerCaseName = attributeName.toLowerCase();

        if (RESERVED_ATTRIBUTE_NAMES.contains(lowerCaseName)) {
            throw new ComponentValidationException(
                    "Attribute name '" + attributeName + "' is reserved by the SCIM core schema");
        }

        if (!seen.add(lowerCaseName)) {
            throw new ComponentValidationException("Duplicate attribute name '" + attributeName + "'");
        }

        if (attribute.getType() != null && !VALID_TYPES.contains(attribute.getType().toLowerCase())) {
            throw new ComponentValidationException("Invalid attribute type '" + attribute.getType()
                    + "' for attribute '" + attributeName + "'");
        }

        if (attribute.getMutability() != null && !VALID_MUTABILITY.contains(attribute.getMutability().toLowerCase())) {
            throw new ComponentValidationException("Invalid attribute mutability '" + attribute.getMutability()
                    + "' for attribute '" + attributeName + "'");
        }

        if (attribute.getUniqueness() != null && !VALID_UNIQUENESS.contains(attribute.getUniqueness().toLowerCase())) {
            throw new ComponentValidationException("Invalid attribute uniqueness '" + attribute.getUniqueness()
                    + "' for attribute '" + attributeName + "'");
        }

        if (attribute.getReturned() != null && !VALID_RETURNED.contains(attribute.getReturned().toLowerCase())) {
            throw new ComponentValidationException("Invalid attribute returned value '" + attribute.getReturned()
                    + "' for attribute '" + attributeName + "'");
        }
    }

    private static List<Schema.Attribute> readAttributes(String json) {
        if (isBlank(json)) {
            return new ArrayList<>();
        }

        try {
            return JsonSerialization.readValue(json, new TypeReference<List<Schema.Attribute>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse SCIM resource type attribute definitions", e);
        }
    }

    private static String writeAttributes(List<Schema.Attribute> attributes) {
        if (attributes == null) {
            attributes = new ArrayList<>();
        }

        try {
            return JsonSerialization.writeValueAsString(attributes);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize SCIM resource type attribute definitions", e);
        }
    }

    private static void putSingle(MultivaluedHashMap<String, String> config, String key, String value) {
        if (value == null) {
            config.remove(key);
        } else {
            config.putSingle(key, value);
        }
    }
}
