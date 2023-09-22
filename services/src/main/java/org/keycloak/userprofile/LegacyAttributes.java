package org.keycloak.userprofile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
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
        if (UserProfileContext.USER_API.equals(context) || UserProfileContext.ACCOUNT.equals(context)) {
            return true;
        }

        if (super.isSupportedAttribute(name)) {
            return true;
        }

        if (name.startsWith(Constants.USER_ATTRIBUTES_PREFIX)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isReadOnly(String name) {
        RealmModel realm = session.getContext().getRealm();

        if (isReadOnlyInternalAttribute(name)) {
            return true;
        }

        if (user == null) {
            return false;
        }

        if (UserModel.USERNAME.equals(name)) {
            if (isServiceAccountUser()) {
                return true;
            }
            if (UserProfileContext.IDP_REVIEW.equals(context)) {
                return false;
            }
            if (UserProfileContext.USER_API.equals(context)) {
                if (realm.isRegistrationEmailAsUsername()) {
                    return false;
                }
            }
            return !realm.isEditUsernameAllowed();
        }

        if (UserModel.EMAIL.equals(name)) {
            if (isServiceAccountUser()) {
                return false;
            }
            if (UserProfileContext.IDP_REVIEW.equals(context)
                    || UserProfileContext.USER_API.equals(context)) {
                return false;
            }
            if (realm.isRegistrationEmailAsUsername() && !realm.isEditUsernameAllowed()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, List<String>> getReadable() {
        if(user == null || user.getAttributes() == null) {
            return Collections.emptyMap();
        }

        return new HashMap<>(user.getAttributes());
    }

    @Override
    public Map<String, List<String>> getWritable() {
        Map<String, List<String>> attributes = new HashMap<>(this);

        for (String name : nameSet()) {
            if (isReadOnly(name)) {
                attributes.remove(name);
            }
        }

        return attributes;
    }

    @Override
    protected boolean isIncludeAttributeIfNotProvided(AttributeMetadata metadata) {
        if (UserModel.LOCALE.equals(metadata.getName())) {
            // locale is an internal attribute and should be updated as a regular attribute
            return false;
        }

        // user api expects that attributes are not updated if not provided when in legacy mode
        return UserProfileContext.USER_API.equals(context);
    }
}
