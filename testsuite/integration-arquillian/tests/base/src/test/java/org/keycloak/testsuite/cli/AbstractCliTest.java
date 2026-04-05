package org.keycloak.testsuite.cli;

import java.util.List;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.cli.exec.AbstractExec;

import org.junit.Assert;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractCliTest extends AbstractKeycloakTest {

    protected String serverUrl = "http://localhost:" + getAuthServerHttpPort() + "/auth";

    static int getAuthServerHttpPort() {
        try {
            return Integer.valueOf(System.getProperty("auth.server.http.port"));
        } catch (Exception e) {
            throw new RuntimeException("System property 'auth.server.http.port' not set or invalid: '"
                  + System.getProperty("auth.server.http.port") + "'");
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        for (RealmRepresentation tr : testRealms) {
            tr.setSslRequired("external");
        }
    }

    public void assertExitCodeAndStdOutSize(AbstractExec exe, int exitCode, int stdOutLineCount) {
        assertExitCodeAndStreamSizes(exe, exitCode, stdOutLineCount, -1);
    }

    public void assertExitCodeAndStdErrSize(AbstractExec exe, int exitCode, int stdErrLineCount) {
        assertExitCodeAndStreamSizes(exe, exitCode, -1, stdErrLineCount);
    }

    public void assertExitCodeAndStreamSizes(AbstractExec exe, int exitCode, int stdOutLineCount, int stdErrLineCount) {
        Assert.assertEquals("exitCode == " + exitCode, exitCode, exe.exitCode());
        if (stdOutLineCount != -1) {
            assertLineCount("STDOUT: " + exe.stdoutString(), exe.stdoutLines(), stdOutLineCount);
        }
        // There is additional logging in case that BC FIPS libraries are used, so the count of logged lines don't match with the case with plain BC used
        // Hence we test count of lines just with FIPS disabled
        if (stdErrLineCount != -1 && isFipsDisabled()) {
            assertLineCount("STDERR: " + exe.stderrString(), exe.stderrLines(), stdErrLineCount);
        }
    }

    private void assertLineCount(String label, List<String> lines, int count) {
        if (lines.size() == count) {
            return;
        }
        // there is some kind of race condition in 'kcreg' that results in intermittent extra empty line
        if (lines.size() == count + 1) {
            if ("".equals(lines.get(lines.size()-1))) {
                return;
            }
        }
        Assert.assertTrue(label + " has " + lines.size() + " lines (expected: " + count + ")", lines.size() == count);
    }

    private boolean isFipsDisabled() {
        return AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE == FipsMode.DISABLED;
    }

}
