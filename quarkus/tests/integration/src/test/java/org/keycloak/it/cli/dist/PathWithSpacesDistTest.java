package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.DistributionTest;

@DistributionTest
class PathWithSpacesDistTest extends AbstractPathDistTest {

    @Override
    protected String getSubPath() {
        return " o o o o";
    }
}
