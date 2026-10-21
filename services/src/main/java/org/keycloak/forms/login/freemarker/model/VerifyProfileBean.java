package org.keycloak.forms.login.freemarker.model;

import java.util.stream.Stream;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.userprofile.UserProfileProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class VerifyProfileBean extends AbstractUserProfileBean {

    private final UserModel user;

    public VerifyProfileBean(UserModel user, MultivaluedMap<String, String> formData, KeycloakSession session) {
        super(formData);
        this.user = user;
        init(session, false);
    }

    @Override
    protected UserProfile createUserProfile(UserProfileProvider provider) {
        return provider.create(UserProfileContext.UPDATE_PROFILE, user);
    }

    @Override
    protected Stream<String> getAttributeDefaultValues(String name){
        return user.getAttributeStream(name);
    }
    
    @Override 
    public String getContext() {
        return UserProfileContext.UPDATE_PROFILE.name();
    }

}
