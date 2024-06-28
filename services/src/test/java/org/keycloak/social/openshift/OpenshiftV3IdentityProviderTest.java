package org.keycloak.social.openshift;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.IdentityProviderModel;

public class OpenshiftV3IdentityProviderTest {

    @Test
    public void shouldConstructProviderUrls() {
        final OpenshiftV3IdentityProviderConfig config = new OpenshiftV3IdentityProviderConfig(new IdentityProviderModel());
        config.setBaseUrl("http://openshift.io:8443");
        final OpenshiftV3IdentityProvider openshiftV3IdentityProvider = new OpenshiftV3IdentityProvider(null, config);

        assertConfiguredUrls(openshiftV3IdentityProvider);
    }

    @Test
    public void shouldConstructProviderUrlsForBaseUrlWithTrailingSlash() {
        final OpenshiftV3IdentityProviderConfig config = new OpenshiftV3IdentityProviderConfig(new IdentityProviderModel());
        config.setBaseUrl("http://openshift.io:8443/");
        final OpenshiftV3IdentityProvider openshiftV3IdentityProvider = new OpenshiftV3IdentityProvider(null, config);

        assertConfiguredUrls(openshiftV3IdentityProvider);
    }

    private void assertConfiguredUrls(OpenshiftV3IdentityProvider openshiftV3IdentityProvider) {
        Assert.assertEquals("http://openshift.io:8443/oauth/authorize", openshiftV3IdentityProvider.getConfig().getAuthorizationUrl());
        Assert.assertEquals("http://openshift.io:8443/oauth/token", openshiftV3IdentityProvider.getConfig().getTokenUrl());
        Assert.assertEquals("http://openshift.io:8443/oapi/v1/users/~", openshiftV3IdentityProvider.getConfig().getUserInfoUrl());
    }

}