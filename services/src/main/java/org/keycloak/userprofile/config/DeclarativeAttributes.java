package org.keycloak.userprofile.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.DefaultAttributes;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;

/**
 * Temporary implementation of {@link org.keycloak.userprofile.Attributes}. It should be removed once
 * the {@link DeclarativeUserProfileProvider} becomes the default.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeclarativeAttributes extends DefaultAttributes {

    public DeclarativeAttributes(UserProfileContext context, Map<String, ?> attributes,
            UserModel user, UserProfileMetadata profileMetadata,
            KeycloakSession session) {
        super(context, attributes, user, profileMetadata, session);
    }

    @Override
    public Map<String, List<String>> getReadable() {
        Map<String, List<String>> attributes = new HashMap<>(this);

        for (String name : nameSet()) {
            AttributeMetadata metadata = getMetadata(name);

            if (metadata == null || !metadata.canView(createAttributeContext(metadata))) {
                attributes.remove(name);
            }
        }

        return attributes;
    }
}
