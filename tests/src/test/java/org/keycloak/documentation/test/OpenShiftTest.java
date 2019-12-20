package org.keycloak.documentation.test;

import org.junit.Assume;
import org.junit.BeforeClass;

public class OpenShiftTest extends AbstractDocsTest {

    @BeforeClass
    public static void skipOnCommunity() {
        Assume.assumeTrue("Skipping product OpenShift guide testing in community", System.getProperties().containsKey("product"));
    }

    @Override
    public String getGuideDirName() {
        return "openshift";
    }

}
