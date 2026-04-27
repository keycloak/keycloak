package org.keycloak.testsuite.cli;

import java.io.InputStream;
import java.util.List;

import org.keycloak.common.crypto.FipsMode;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.cli.exec.AbstractExec;
import org.keycloak.testsuite.cli.exec.AbstractExecBuilder;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcAdmExec extends AbstractExec {

    public static final String WORK_DIR = System.getProperty("user.dir") + "/target/containers/keycloak-client-tools";

    public static final String CMD = OS_ARCH.isWindows() ? "kcadm.bat" : "kcadm.sh";

    private KcAdmExec(String workDir, String argsLine, InputStream stdin) {
        this(workDir, argsLine, null, stdin);
    }

    private KcAdmExec(String workDir, String argsLine, String env, InputStream stdin) {
        super(workDir, argsLine, env, stdin);
    }

    @Override
    public String getCmd() {
        return "bin/" + CMD;
    }

    public static KcAdmExec.Builder newBuilder() {
        return (KcAdmExec.Builder) new KcAdmExec.Builder().workDir(WORK_DIR);
    }

    public static KcAdmExec execute(String args) {
        return newBuilder()
                .argsLine(args)
                .execute();
    }

    @Override
    public List<String> stderrLines() {
        List<String> lines = super.stderrLines();
        // remove the two lines with the BC provider info if FIPS
        return AuthServerTestEnricher.AUTH_SERVER_FIPS_MODE == FipsMode.DISABLED || lines.size() < 2
            ? lines
            : lines.subList(2, lines.size());
    }

    public static class Builder extends AbstractExecBuilder<KcAdmExec> {

        @Override
        public KcAdmExec execute() {
            KcAdmExec exe = new KcAdmExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.execute();
            return exe;
        }

        @Override
        public KcAdmExec executeAsync() {
            KcAdmExec exe = new KcAdmExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.executeAsync();
            return exe;
        }
    }
}
