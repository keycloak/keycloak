package org.keycloak.common.profile;

import org.keycloak.common.Feature;
import org.keycloak.common.Profile;

public interface ProfileConfigResolver {

    Profile.ProfileValue getProfile();

    Boolean getFeatureConfig(Feature feature);

}
