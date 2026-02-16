package org.keycloak.it.cli.dist;

import org.keycloak.it.junit5.extension.DistributionTest;

@DistributionTest(reInstall = DistributionTest.ReInstall.BEFORE_TEST)
class PathWithSpecialCharsDistTest extends AbstractPathDistTest {

    @Override
    protected String getSubPath() {
        return "äöüÄÖÜ-èêñçà#žščř";
    }
}
