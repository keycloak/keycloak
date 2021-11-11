package org.keycloak.testsuite.user.profile;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileMetadata;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;

import java.util.Map;

public class CustomUserProfileProvider extends DeclarativeUserProfileProvider {

    public static final String ID = "custom-user-profile";

    public CustomUserProfileProvider() {
        super();
    }

    public CustomUserProfileProvider(KeycloakSession session,
            Map<UserProfileContext, UserProfileMetadata> metadataRegistry, String defaultRawConfig) {
        super(session, metadataRegistry, defaultRawConfig);
    }

    @Override
    protected UserProfileProvider create(KeycloakSession session,
            Map<UserProfileContext, UserProfileMetadata> metadataRegistry) {
        return new CustomUserProfileProvider(session, metadataRegistry, UPConfigUtils.readDefaultConfig());
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public UserProfile create(UserProfileContext context, UserModel user) {
        return this.create(context, user.getAttributes(), user);
    }

    @Override
    public UserProfile create(UserProfileContext context, Map<String, ?> attributes, UserModel user) {
        return super.create(context, attributes, user);
    }

    @Override
    public UserProfile create(UserProfileContext context, Map<String, ?> attributes) {
        return this.create(context, attributes, (UserModel) null);
    }

    @Override
    public int order() {
        return super.order() - 1;
    }
}
