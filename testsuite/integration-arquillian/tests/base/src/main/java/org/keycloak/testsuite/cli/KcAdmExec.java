package org.keycloak.testsuite.cli;

import org.keycloak.testsuite.cli.exec.AbstractExec;
import org.keycloak.testsuite.cli.exec.AbstractExecBuilder;

import java.io.InputStream;

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
