package org.keycloak.testsuite.oauth.tokenexchange;

import org.keycloak.common.Profile;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;

import org.junit.Ignore;
import org.junit.Test;

@EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
public class StandardTokenExchangeV2WithDynamicScopesTest extends StandardTokenExchangeV2Test {

    @Override
    @Test
    @UncaughtServerErrorExpected
    @Ignore
    public void testExchangeWithDynamicScopesEnabled(){
    }
}
