package org.keycloak.userprofile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * Enables legacy support when managing attributes without the declarative provider.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class LegacyAttributes extends DefaultAttributes {

    public LegacyAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user,
            UserProfileMetadata profileMetadata, KeycloakSession session) {
        super(context, attributes, user, profileMetadata, session);
    }

    @Override
    protected boolean isSupportedAttribute(String name) {
        if (super.isSupportedAttribute(name)) {
            return true;
        }

        if (name.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isReadOnly(String attributeName) {
        return isReadOnlyFromMetadata(attributeName) || isReadOnlyInternalAttribute(attributeName);
    }

    @Override
    public Map<String, List<String>> getReadable() {
        if(user == null)
            return null;

        Map<String, List<String>> attributes = new HashMap<>(user.getAttributes());

        if (attributes.isEmpty()) {
            return null;
        }

        return attributes;
    }

    @Override
    protected boolean isIncludeAttributeIfNotProvided(AttributeMetadata metadata) {
        // user api expects that attributes are not updated if not provided when in legacy mode
        return UserProfileContext.USER_API.equals(context);
    }
}
