package org.keycloak.scim.model.schema;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.scim.model.group.GroupCoreModelSchema;
import org.keycloak.scim.model.user.UserCoreModelSchema;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.schema.Schema;
import org.keycloak.scim.resource.schema.Schema.Attribute;
import org.keycloak.scim.resource.spi.ScimResourceTypeProvider;
import org.keycloak.scim.resource.user.EnterpriseUser;
import org.keycloak.scim.resource.user.User;

/**
 * Provider for SCIM Schema resources. This provider exposes the supported SCIM schemas
 * for discovery by SCIM clients via the /Schemas endpoint.
 * <p>
 * Schemas are read-only resources that describe the structure of SCIM resources.
 * This implementation supports:
 * - Built-in core schemas (User, Group)
 * - Built-in extension schemas (EnterpriseUser)
 * - Custom extension schemas based on user profile configuration (future)
 */
public class SchemaResourceTypeProvider implements ScimResourceTypeProvider<Schema> {

    private final Map<String, Schema> schemas = new HashMap<>();
    private final KeycloakSession session;

    public SchemaResourceTypeProvider(KeycloakSession session) {
        this.session = session;
        initializeSchemas();
    }

    private void initializeSchemas() {
        // User Core Schema - built from UserCoreModelSchema
        Schema userSchema = buildSchemaFromResourceClass(
            Scim.USER_CORE_SCHEMA,
            "User",
            "User Account",
            User.class,
            new UserCoreModelSchema(session).getAttributes().keySet()
        );
        schemas.put(userSchema.getId(), userSchema);

        // Group Core Schema - built from GroupCoreModelSchema
        Schema groupSchema = buildSchemaFromResourceClass(
            Scim.GROUP_CORE_SCHEMA,
            "Group",
            "Group",
            Group.class,
            new GroupCoreModelSchema().getAttributes().keySet()
        );
        schemas.put(groupSchema.getId(), groupSchema);

        // Enterprise User Extension Schema
        // Note: UserEnterpriseModelSchema.getAttributes() returns empty map because
        // enterprise attributes are dynamically configured through user profiles.
        // We use the known attribute names from the EnterpriseUser class instead.
        Schema enterpriseUserSchema = buildSchemaFromResourceClass(
            Scim.ENTERPRISE_USER_SCHEMA,
            "EnterpriseUser",
            "Enterprise User",
            EnterpriseUser.class,
            // These are the standard SCIM Enterprise User extension attributes
            Set.of("employeeNumber", "costCenter", "organization", "division", "department", "manager")
        );
        schemas.put(enterpriseUserSchema.getId(), enterpriseUserSchema);

        // Add custom extension schemas from user profile configuration ??
    }

    /**
     * Builds a Schema resource by extracting attribute definitions from the resource class
     * and cross-referencing with the supported attribute paths from the ModelSchema.
     *
     * @param id the schema ID
     * @param name the schema name
     * @param description the schema description
     * @param resourceClass the resource class (User, Group, EnterpriseUser)
     * @param supportedPaths the supported SCIM attribute paths from the ModelSchema
     * @return the built Schema resource
     */
    private Schema buildSchemaFromResourceClass(String id, String name, String description,
                                                Class<?> resourceClass, Set<String> supportedPaths) {
        Schema schema = new Schema();
        schema.setId(id);
        schema.setName(name);
        schema.setDescription(description);

        // Extract top-level attribute names from supported SCIM paths
        Set<String> topLevelAttributes = supportedPaths.stream()
            .map(this::extractTopLevelAttributeName)
            .collect(Collectors.toSet());

        // Build Schema.Attribute objects using reflection on resource class
        List<Attribute> attributes = topLevelAttributes.stream()
            .map(attrName -> buildSchemaAttributeFromField(resourceClass, attrName))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        schema.setAttributes(attributes);
        return schema;
    }

    /**
     * Extracts the top-level attribute name from a SCIM path.
     * Examples:
     * - "emails[0].value" -> "emails"
     * - "name.givenName" -> "name"
     * - "userName" -> "userName"
     */
    private String extractTopLevelAttributeName(String scimPath) {
        int dotIndex = scimPath.indexOf('.');
        int bracketIndex = scimPath.indexOf('[');

        if (dotIndex > 0 && (bracketIndex < 0 || dotIndex < bracketIndex)) {
            return scimPath.substring(0, dotIndex);
        } else if (bracketIndex > 0) {
            return scimPath.substring(0, bracketIndex);
        } else {
            return scimPath;
        }
    }

    /**
     * Builds a Schema.Attribute by reflecting on the resource class field.
     * Determines the type and multiValued properties based on the field type.
     * Searches both the class and its parent classes for the field.
     */
    private Attribute buildSchemaAttributeFromField(Class<?> resourceClass, String fieldName) {
        Field field = findField(resourceClass, fieldName);
        if (field == null) {
            // Field doesn't exist in resource class or parent classes, skip it
            return null;
        }

        Attribute attr = new Attribute();
        attr.setName(fieldName);

        // Determine type and multiValued based on field type
        Class<?> fieldType = field.getType();
        Type genericType = field.getGenericType();

        if (List.class.isAssignableFrom(fieldType)) {
            attr.setMultiValued(true);
            // Determine element type for reference types
            if (genericType instanceof ParameterizedType pType) {
                Type[] typeArgs = pType.getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> elementType) {
                    // Handle reference types (members)
                    if (elementType.getSimpleName().equals("Member")) {
                        attr.setType("complex");
                        attr.setReferenceTypes(List.of("User", "Group"));
                    } else {
                        attr.setType("complex");
                    }
                } else {
                    attr.setType("complex");
                }
            } else {
                attr.setType("complex");
            }
        } else if (fieldType == String.class) {
            attr.setType("string");
            attr.setMultiValued(false);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            attr.setType("boolean");
            attr.setMultiValued(false);
        } else if (fieldType == Integer.class || fieldType == int.class ||
                   fieldType == Long.class || fieldType == long.class) {
            attr.setType("integer");
            attr.setMultiValued(false);
        } else if (fieldType == Double.class || fieldType == double.class ||
                   fieldType == Float.class || fieldType == float.class) {
            attr.setType("decimal");
            attr.setMultiValued(false);
        } else {
            // Complex type (Name, Address, Manager, etc.)
            attr.setType("complex");
            attr.setMultiValued(false);
            // Special case for manager field which references User
            if (fieldName.equals("manager")) {
                attr.setReferenceTypes(List.of("User"));
            }
        }

        return attr;
    }

    /**
     * Finds a field by name, searching the class and all its superclasses.
     * This is necessary because some attributes like 'externalId' are defined
     * in the parent ResourceTypeRepresentation class.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public Schema get(String id) {
        // TODO: Add `view-realm` role check for schema discovery ??
        // Currently accessible to any authenticated user with valid bearer token
        // Should be aligned with other discovery endpoints (ResourceTypes, ServiceProviderConfig)
        return schemas.get(id);
    }

    @Override
    public Stream<Schema> getAll(SearchRequest searchRequest) {
        // Per RFC 7644 Section 4, /Schemas is a discovery endpoint that SHALL return all schemas.
        // Filtering, sorting, and pagination are not supported for discovery endpoints.
        // The searchRequest parameter is ignored.
        return schemas.values().stream();
    }

    @Override
    public Schema create(Schema resource) {
        throw new ModelException("Schemas are read-only and cannot be created");
    }

    @Override
    public Schema update(Schema resource) {
        throw new ModelException("Schemas are read-only and cannot be updated");
    }

    @Override
    public boolean delete(String id) {
        throw new ModelException("Schemas are read-only and cannot be deleted");
    }

    @Override
    public String getSchema() {
        return Scim.SCHEMA_CORE_SCHEMA;
    }

    @Override
    public Class<Schema> getResourceType() {
        return Schema.class;
    }

    @Override
    public void close() {
        // No resources to close
    }
}
