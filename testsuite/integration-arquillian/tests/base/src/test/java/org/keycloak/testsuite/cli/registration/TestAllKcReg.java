package org.keycloak.testsuite.cli.registration;

import org.junit.Test;
import java.io.IOException;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TestAllKcReg extends AbstractRegCliTest {


    @Test
    public void runAllTests() throws IOException {
        new KcRegConfigTest().testRegistrationToken();
        new KcRegCreateTest().adminClient(adminClient).runAllTests();
        new KcRegTest().adminClient(adminClient).runAllTests();
        new KcRegUpdateTest().adminClient(adminClient).testUpdateThoroughly();
        new KcRegUpdateTokenTest().testUpdateToken();
    }
}
