package org.keycloak.userprofile.profile;

import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileAttributes;

import java.util.List;
import java.util.Map;

public abstract class AbstractUserProfile implements UserProfile {

    private final UserProfileAttributes attributes;


    public AbstractUserProfile(Map<String, List<String>> attributes) {
        this.attributes = new UserProfileAttributes(attributes);
    }

    @Override
    public UserProfileAttributes getAttributes() {
        return this.attributes;
    }



}
