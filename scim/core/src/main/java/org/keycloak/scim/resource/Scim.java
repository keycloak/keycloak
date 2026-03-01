package org.keycloak.scim.resource;

public final class Scim {

    // Core Schemas
    public static final String ENTERPRISE_USER_SCHEMA = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
    public static final String USER_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String GROUP_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Group";
    public static final String SERVICE_PROVIDER_CONFIG_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig";
    public static final String SCHEMA_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:Schema";
    public static final String RESOURCE_TYPE_CORE_SCHEMA = "urn:ietf:params:scim:schemas:core:2.0:ResourceType";

    // Core Resource Types
    public static final String USER_RESOURCE_TYPE = "User";
    public static final String GROUP_RESOURCE_TYPE = "Group";
    public static final String SERVICE_PROVIDER_CONFIG_RESOURCE_TYPE = "ServiceProviderConfig";
    public static final String RESOURCE_TYPE_RESOURCE_TYPE = "ResourceType";
    public static final String SCHEMA_RESOURCE_TYPE = "Schema";

    public static String getCoreSchema(Class<? extends ResourceTypeRepresentation> resourceType) {
        return switch (resourceType.getSimpleName()) {
            case USER_RESOURCE_TYPE -> USER_CORE_SCHEMA;
            case GROUP_RESOURCE_TYPE -> GROUP_CORE_SCHEMA;
            case SERVICE_PROVIDER_CONFIG_RESOURCE_TYPE -> SERVICE_PROVIDER_CONFIG_CORE_SCHEMA;
            case RESOURCE_TYPE_RESOURCE_TYPE -> RESOURCE_TYPE_CORE_SCHEMA;
            case SCHEMA_RESOURCE_TYPE -> SCHEMA_CORE_SCHEMA;
            default -> throw new IllegalArgumentException("Unknown resource type " + resourceType);
        };
    }
}
