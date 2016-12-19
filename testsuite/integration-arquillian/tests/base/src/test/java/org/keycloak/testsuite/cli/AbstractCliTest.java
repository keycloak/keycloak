package org.keycloak.testsuite.cli;

import org.junit.Assert;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.cli.exec.AbstractExec;

import java.util.List;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractCliTest extends AbstractKeycloakTest {


    public void assertExitCodeAndStdOutSize(AbstractExec exe, int exitCode, int stdOutLineCount) {
        assertExitCodeAndStreamSizes(exe, exitCode, stdOutLineCount, -1);
    }

    public void assertExitCodeAndStdErrSize(AbstractExec exe, int exitCode, int stdErrLineCount) {
        assertExitCodeAndStreamSizes(exe, exitCode, -1, stdErrLineCount);
    }

    public void assertExitCodeAndStreamSizes(AbstractExec exe, int exitCode, int stdOutLineCount, int stdErrLineCount) {
        Assert.assertEquals("exitCode == " + exitCode, exitCode, exe.exitCode());
        if (stdOutLineCount != -1) {
            try {
                assertLineCount("stdout output", exe.stdoutLines(), stdOutLineCount);
            } catch (Throwable e) {
                throw new AssertionError("STDOUT: " + exe.stdoutString(), e);
            }
        }
        if (stdErrLineCount != -1) {
            try {
                assertLineCount("stderr output", exe.stderrLines(), stdErrLineCount);
            } catch (Throwable e) {
                throw new AssertionError("STDERR: " + exe.stderrString(), e);
            }
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

}
