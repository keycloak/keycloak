package org.keycloak.scim.model.user;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public abstract class AbstractUserModelSchema extends AbstractModelSchema<UserModel ,User> {

    public static final String ANNOTATION_SCIM_SCHEMA = "scim.schema";
    public static final String ANNOTATION_SCIM_SCHEMA_ATTRIBUTE = "scim.schema.attribute";
    private final KeycloakSession session;

    public AbstractUserModelSchema(KeycloakSession session, String name, List<Attribute<UserModel, User>> attributeMappers) {
        super(name, attributeMappers);
        this.session = session;
    }

    @Override
    protected Set<String> getAttributeNames(UserModel model) {
        Set<String> names = new HashSet<>(getAttributes(model).nameSet());
        names.add(UserModel.ENABLED);
        return names;
    }

    @Override
    protected String getAttributeSchema(UserModel model, String name) {
        Object schema = getAttributeAnnotations(model, name).get(ANNOTATION_SCIM_SCHEMA);

        if (schema == null) {
            return null;
        }

        return String.valueOf(schema);
    }

    @Override
    protected String getAttributeSchemaName(UserModel model, String name) {
        Object schema = getAttributeAnnotations(model, name).get(ANNOTATION_SCIM_SCHEMA_ATTRIBUTE);

        if (schema == null) {
            return null;
        }

        return String.valueOf(schema);
    }

    @Override
    protected String getAttributeValue(UserModel model, String name) {
        if (UserModel.ENABLED.equals(name)) {
            return String.valueOf(model.isEnabled());
        }
        return getAttributes(model).getFirst(name);
    }

    private Map<String, Object> getAttributeAnnotations(UserModel model, String name) {
        AttributeMetadata metadata = getAttributes(model).getMetadata(name);

        if (metadata == null) {
            return Map.of();
        }

        return Optional.ofNullable(metadata.getAnnotations()).orElse(Map.of());
    }

    private Attributes getAttributes(UserModel model) {
        UserProfile profile = session.getProvider(UserProfileProvider.class).create(UserProfileContext.SCIM, model);
        return profile.getAttributes();
    }
}
