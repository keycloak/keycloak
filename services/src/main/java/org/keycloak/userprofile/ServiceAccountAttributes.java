package org.keycloak.userprofile;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * <p>A specific {@link Attributes} implementation to handle service accounts.
 *
 * <p>Service accounts are not regular users, and it should be possible to manage unmanaged attributes but only when
 * operating through the {@link UserProfileContext#USER_API}. Otherwise, administrators will be forced to enable unmanaged
 * attributes by setting a {@link org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy} or
 * end up defining managed attributes that are specific for service accounts in the user profile configuration, which is
 * mainly targeted for regular users.
 *
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ServiceAccountAttributes extends DefaultAttributes {

    public ServiceAccountAttributes(UserProfileContext context, Map<String, ?> attributes, UserModel user,
                                    UserProfileMetadata profileMetadata, KeycloakSession session) {
        super(context, attributes, user, profileMetadata, session);
    }

    @Override
    public boolean isReadOnly(String name) {
        if (UserModel.USERNAME.equals(name)) {
            return true;
        }

        return !UserProfileContext.USER_API.equals(context);
    }

    @Override
    protected AttributeMetadata createUnmanagedAttributeMetadata(String name) {
        return new AttributeMetadata(name, Integer.MAX_VALUE) {
            @Override
            public boolean canView(AttributeContext context) {
                return UserProfileContext.USER_API.equals(context.getContext());
            }

            @Override
            public boolean canEdit(AttributeContext context) {
                return UserProfileContext.USER_API.equals(context.getContext());
            }
        };
    }

    @Override
    protected boolean isAllowUnmanagedAttribute() {
        return UserProfileContext.USER_API.equals(context);
    }

    @Override
    protected void setUserName(Map<String, List<String>> newAttributes, List<String> values) {
        // can not update username for service accounts
    }
}
