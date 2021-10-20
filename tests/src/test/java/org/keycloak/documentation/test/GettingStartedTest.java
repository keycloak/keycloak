package org.keycloak.documentation.test;

import org.junit.Assume;
import org.junit.BeforeClass;

public class GettingStartedTest extends AbstractDocsTest {

    @BeforeClass
    public static void skipOnCommunity() {
        Assume.assumeTrue("Skipping product OpenShift guide testing in community", System.getProperties().containsKey("product"));
    }

    @Override
    public String getGuideDirName() {
        return "getting_started";
    }

}
