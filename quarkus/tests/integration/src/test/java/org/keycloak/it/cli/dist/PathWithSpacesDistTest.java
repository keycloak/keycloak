package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.DistributionTest;

@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
class PathWithSpacesDistTest extends AbstractPathDistTest {

    @Override
    protected String getSubPath() {
        return " o o o o";
    }
}
