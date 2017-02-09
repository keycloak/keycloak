package org.keycloak.testsuite.cli.admin;

import org.junit.Test;

import java.io.IOException;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class TestAllKcAdm extends AbstractAdmCliTest {

    @Test
    public void runAllTests() throws IOException {
        new KcAdmTest().runAllTests();
        new KcAdmCreateTest().runAllTests();
        new KcAdmUpdateTest().testUpdateThoroughly();
        new KcAdmSessionTest().test();
    }
}
