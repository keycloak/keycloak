/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

import static org.keycloak.utils.StringUtil.isBlank;

/**
 * <p>Reads and validates the mappings between group attributes and attributes from SCIM Group extension schemas.
 *
 * <p>The mappings are stored as a JSON array in the realm attribute {@link #REALM_ATTRIBUTE}. Each element maps a group
 * attribute to a fully qualified SCIM attribute, for example:
 *
 * <pre>
 * [
 *   {
 *     "name": "entryUUID",
 *     "scimAttribute": "urn:example:params:scim:schemas:extension:custom:1.0:Group:entryUUID",
 *     "readOnly": true
 *   }
 * ]
 * </pre>
 */
public final class GroupAttributeMappings {

    /**
     * The name of the realm attribute holding the mappings.
     */
    public static final String REALM_ATTRIBUTE = "scimGroupAttributeMappings";

    static final String NAME = "name";
    static final String SCIM_ATTRIBUTE = "scimAttribute";
    static final String READ_ONLY = "readOnly";
    static final String MULTIVALUED = "multivalued";

    private static final Set<String> KNOWN_PROPERTIES = Set.of(NAME, SCIM_ATTRIBUTE, READ_ONLY, MULTIVALUED);

    /**
     * Group attribute names that cannot be mapped. {@code externalId} is already mapped by the core Group schema, and
     * the other names clash with columns of the group entity, which would make filters compare against the column
     * instead of the group attribute.
     */
    private static final Set<String> RESERVED_NAMES = Set.of("externalId", "id", "name", "description", "parentId",
            "realm", "type", "createdTimestamp", "lastModifiedTimestamp", "organization", "attributes");

    /**
     * Schema URN prefixes reserved for the schemas defined by RFC 7643 and RFC 7644.
     */
    private static final List<String> RESERVED_SCHEMA_PREFIXES = List.of("urn:ietf:params:scim:schemas:core:",
            "urn:ietf:params:scim:api:", Scim.ENTERPRISE_USER_SCHEMA);

    /**
     * The schema URN syntax accepted by the SCIM filter lexer ({@code URN_SCHEMA} in {@code ScimFilterLexer.g4}).
     */
    private static final Pattern SCHEMA_URN = Pattern.compile("urn:[a-zA-Z0-9]+(:[a-zA-Z0-9._-]+)+");

    private static final Logger logger = Logger.getLogger(GroupAttributeMappings.class);

    private GroupAttributeMappings() {
    }

    /**
     * A mapping between a group attribute and an attribute from a SCIM Group extension schema.
     *
     * @param name the name of the group attribute
     * @param scimAttribute the fully qualified name of the SCIM attribute, including the schema URN
     * @param readOnly whether SCIM clients are not allowed to change the value
     * @param multivalued whether the attribute holds multiple values
     */
    public record Mapping(String name, String scimAttribute, boolean readOnly, boolean multivalued) {
    }

    /**
     * Returns the mappings configured for the given {@code realm}, keyed by group attribute name. An invalid
     * configuration is logged and results in no mappings.
     *
     * @param realm the realm
     * @return the mappings, keyed by the name of the group attribute
     */
    public static Map<String, Mapping> getMappings(RealmModel realm) {
        if (realm == null) {
            return Map.of();
        }

        String value = realm.getAttribute(REALM_ATTRIBUTE);

        try {
            Map<String, Mapping> mappings = new HashMap<>();

            for (Mapping mapping : parse(value)) {
                mappings.put(mapping.name(), mapping);
            }

            return Collections.unmodifiableMap(mappings);
        } catch (ModelValidationException mve) {
            logger.warnf("Ignoring invalid SCIM group attribute mappings in realm '%s': %s", realm.getName(), mve.getMessage());
            return Map.of();
        }
    }

    /**
     * Parses and validates the given JSON {@code value}.
     *
     * @param value the JSON array with the mappings. If {@code null} or blank, no mappings are returned.
     * @return the mappings
     * @throws ModelValidationException if the value is not a valid configuration
     */
    public static List<Mapping> parse(String value) throws ModelValidationException {
        if (isBlank(value)) {
            return List.of();
        }

        JsonNode root;

        try {
            root = JsonSerialization.readValue(value, JsonNode.class);
        } catch (IOException e) {
            throw invalid("value is not valid JSON");
        }

        if (root == null || !root.isArray()) {
            throw invalid("value must be a JSON array");
        }

        List<Mapping> mappings = new ArrayList<>();
        Map<String, String> namesByScimAttribute = new HashMap<>();
        Set<String> names = new HashSet<>();

        for (JsonNode node : root) {
            Mapping mapping = toMapping(node);

            if (!names.add(mapping.name())) {
                throw invalid("duplicate mapping for group attribute '" + mapping.name() + "'");
            }

            // SCIM attribute names are case-insensitive (RFC 7643, Section 2.1)
            String existing = namesByScimAttribute.putIfAbsent(mapping.scimAttribute().toLowerCase(Locale.ROOT), mapping.name());

            if (existing != null) {
                throw invalid("duplicate SCIM attribute mapping '" + mapping.scimAttribute() + "' configured for group attributes '"
                        + existing + "' and '" + mapping.name() + "'");
            }

            mappings.add(mapping);
        }

        return List.copyOf(mappings);
    }

    private static Mapping toMapping(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw invalid("each mapping must be a JSON object");
        }

        node.fieldNames().forEachRemaining(property -> {
            if (!KNOWN_PROPERTIES.contains(property)) {
                throw invalid("unknown property '" + property + "'");
            }
        });

        String name = getText(node, NAME);
        String scimAttribute = getText(node, SCIM_ATTRIBUTE);

        if (isBlank(name)) {
            throw invalid("property '" + NAME + "' is required");
        }

        if (RESERVED_NAMES.contains(name)) {
            throw invalid("group attribute '" + name + "' cannot be mapped");
        }

        validateScimAttribute(name, scimAttribute);

        return new Mapping(name, scimAttribute, getBoolean(node, READ_ONLY), getBoolean(node, MULTIVALUED));
    }

    private static void validateScimAttribute(String name, String scimAttribute) {
        if (isBlank(scimAttribute)) {
            throw invalid("property '" + SCIM_ATTRIBUTE + "' is required for group attribute '" + name + "'");
        }

        String schema = Attribute.getSchema(scimAttribute);

        if (schema == null || !schema.contains(":")) {
            throw invalid("SCIM attribute '" + scimAttribute + "' must be prefixed with the URN of an extension schema");
        }

        // the same syntax accepted by the SCIM filter lexer, so that the attribute can be used in filters
        if (!SCHEMA_URN.matcher(schema).matches()) {
            throw invalid("SCIM attribute '" + scimAttribute + "' has an invalid schema URN '" + schema + "'");
        }

        String simpleName = Attribute.getSimpleName(scimAttribute);

        // sub-attributes are not supported, and SCIM attribute names start with a letter (RFC 7643, Section 2.1)
        if (!simpleName.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw invalid("SCIM attribute '" + scimAttribute + "' has an invalid attribute name '" + simpleName + "'");
        }

        for (String prefix : RESERVED_SCHEMA_PREFIXES) {
            if (schema.regionMatches(true, 0, prefix, 0, prefix.length())) {
                throw invalid("SCIM attribute '" + scimAttribute + "' cannot be mapped to the schema '" + schema + "'");
            }
        }
    }

    private static String getText(JsonNode node, String property) {
        JsonNode value = node.get(property);

        if (value == null || value.isNull()) {
            return null;
        }

        if (!value.isTextual()) {
            throw invalid("property '" + property + "' must be a string");
        }

        return value.textValue().trim();
    }

    private static boolean getBoolean(JsonNode node, String property) {
        JsonNode value = node.get(property);

        if (value == null || value.isNull()) {
            return false;
        }

        if (!value.isBoolean()) {
            throw invalid("property '" + property + "' must be a boolean");
        }

        return value.booleanValue();
    }

    private static ModelValidationException invalid(String message) {
        return new ModelValidationException("Invalid SCIM group attribute mappings: " + message);
    }
}
