package org.keycloak.common;

public interface ProfileConfigResolver {

    Profile.ProfileValue getProfile();

    Boolean getFeatureConfig(Feature feature);

}
