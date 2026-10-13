package org.keycloak.testsuite.user.profile;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;

public class CustomUserProfileProvider extends DeclarativeUserProfileProvider {

    public CustomUserProfileProvider(KeycloakSession session, CustomUserProfileProviderFactory factory) {
        super(session, factory);
        UPConfig upConfig = getConfiguration();

        upConfig.getAttribute(UserModel.FIRST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.LAST_NAME).setRequired(null);
        upConfig.getAttribute(UserModel.EMAIL).setRequired(null);

        setConfiguration(upConfig);
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

}
