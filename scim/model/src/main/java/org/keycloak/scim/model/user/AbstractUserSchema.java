package org.keycloak.scim.model.user;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.scim.resource.schema.AbstractScimSchema;
import org.keycloak.scim.resource.user.User;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.Attributes;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

public abstract class AbstractUserSchema extends AbstractScimSchema<UserModel ,User> {

    private final KeycloakSession session;

    public AbstractUserSchema(KeycloakSession session, Map<String, AttributeMapper<UserModel, User>> attributeMappers) {
        super(attributeMappers);
        this.session = session;
    }

    @Override
    protected Set<String> getAttributeNames(UserModel model) {
        return getAttributes(model).nameSet();
    }

    @Override
    protected String getScimSchema(UserModel model, String name) {
        return String.valueOf(getAttributeAnnotations(model, name).get("scim.schema"));
    }

    @Override
    protected String getScimAttributeName(UserModel model, String name) {
        return String.valueOf(getAttributeAnnotations(model, name).get("scim.schema.attribute"));
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
