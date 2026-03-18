package org.keycloak.scim.model.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
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

public abstract class AbstractUserModelSchema extends AbstractModelSchema<UserModel ,User> {

    public static final String ANNOTATION_SCIM_SCHEMA_ATTRIBUTE = "kc.scim.schema.attribute";
    private final KeycloakSession session;

    public AbstractUserModelSchema(KeycloakSession session, String name) {
        super(name);
        this.session = session;
    }

    @Override
    protected Set<String> getModelAttributeNames() {
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of());
        Attributes attributes = profile.getAttributes();
        Set<String> names = new HashSet<>(attributes.nameSet());
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
            return model.getGroupsStream().toList();
        }
        if (UserModel.EMAIL.equals(name)) {
            return model.getEmail() == null ? List.of() : List.of(model.getEmail());
        }
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, model);
        Attributes attributes = profile.getAttributes();
        return attributes.getFirst(name);
    }

    private Map<String, Object> getAttributeAnnotations(String name) {
        AttributeMetadata metadata = getProfileAttributes().getMetadata(name);

        if (metadata == null) {
            return Map.of();
        }

        return ofNullable(metadata.getAnnotations()).orElse(Map.of());
    }

    private Attributes getProfileAttributes() {
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, Map.of());
        return profile.getAttributes();
    }

    protected String createModelAttributeResolver(Attribute<UserModel, User> attribute) {
        for (String name : getModelAttributeNames()) {
            Object scimName = getAttributeSchemaName(name);
            List<String> paths = getPaths(attribute);

            if (paths.contains(scimName)) {
                return name;
            }
        }

        return null;
    }
}
