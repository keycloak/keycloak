package org.keycloak.scim.model.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Permissions;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.AbstractModelSchema;
import org.keycloak.scim.resource.schema.attribute.Attribute;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

import static java.util.Optional.ofNullable;

import static org.keycloak.scim.resource.schema.attribute.Attribute.getSchema;

public abstract class AbstractUserModelSchema extends AbstractModelSchema<UserModel ,User> {

    public static final String ANNOTATION_SCIM_SCHEMA_ATTRIBUTE = "kc.scim.schema.attribute";
    private final KeycloakSession session;
    private UserProfile metadataProfile;
    private UserModel cachedUser;
    private Attributes cachedUserAttributes;

    public AbstractUserModelSchema(KeycloakSession session, String name) {
        super(name);
        this.session = session;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        UserProfile profile = getUserProfile();
        Set<String> names = new HashSet<>(profile.getAttributes().getReadable().keySet());

        names.add(UserModel.ENABLED);
        names.add("groups");

        return names;
    }

    @Override
    protected String getAttributeSchemaName(String name) {
        if ("groups".equals(name)) {
            return name;
        }

        Object schema = getAttributeAnnotations(name).get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);

        if (schema == null) {
            return null;
        }

        return String.valueOf(schema);
    }

    @Override
    protected Object getAttributeValue(UserModel model, String name) {
        if (UserModel.ENABLED.equals(name)) {
            return String.valueOf(model.isEnabled());
        }
        if ("groups".equals(name)) {
            Permissions permissions = session.getContext().getPermissions();

            if (permissions.hasPermission(model, AdminPermissionsSchema.USERS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW)) {
                return model.getGroupsStream()
                        .filter(group -> !isOrganizationGroup(group))
                        .filter(this::canViewGroup)
                        .toList();
            }

            return List.of();
        }
        if (UserModel.EMAIL.equals(name)) {
            return model.getEmail() == null ? List.of() : List.of(model.getEmail());
        }
        if (UserModel.CREATED_TIMESTAMP.equals(name)) {
            return model.getCreatedTimestamp();
        }
        return getUserAttributes(model).getFirst(name);
    }

    private Map<String, Object> getAttributeAnnotations(String name) {
        AttributeMetadata metadata = getUserProfile().getAttributes().getMetadata(name);

        if (metadata == null) {
            return Map.of();
        }

        return ofNullable(metadata.getAnnotations()).orElse(Map.of());
    }

    protected String createModelAttributeResolver(Attribute<UserModel, User> attribute) {
        for (String name : getModelAttributeNames()) {
            String scimName = getAttributeSchemaName(name);

            if (hasPath(attribute, scimName)) {
                return name;
            }
        }

        return null;
    }

    protected boolean hasSchema(String attributeName) {
        return getId().equals(getSchema(attributeName));
    }

    protected UserProfile getUserProfile() {
        if (metadataProfile == null) {
            metadataProfile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of());
        }
        return metadataProfile;
    }

    private Attributes getUserAttributes(UserModel model) {
        if (cachedUser != model) {
            UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, model);
            cachedUserAttributes = profile.getAttributes();
            cachedUser = model;
        }
        return cachedUserAttributes;
    }

    protected boolean canViewGroup(GroupModel group) {
        return session.getContext().getPermissions().hasPermission(group, AdminPermissionsSchema.GROUPS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW);
    }

    /**
     * Organization groups are only accessible through the Organization API and must not be exposed through SCIM.
     */
    protected static boolean isOrganizationGroup(GroupModel group) {
        return GroupModel.Type.ORGANIZATION.equals(group.getType()) && group.getOrganization() != null;
    }
}
