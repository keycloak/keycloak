package org.keycloak.utils;

import org.keycloak.common.Profile;

public class FeatureHelper {

    public static boolean isMlDsaOAuthEnbled() {
        return Profile.isFeatureEnabled(Profile.Feature.OAUTH_ML_DSA) && !Profile.isFeatureEnabled(Profile.Feature.FIPS);
    }

}
