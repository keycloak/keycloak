package org.keycloak.testsuite.cli;

import org.keycloak.testsuite.cli.exec.AbstractExec;
import org.keycloak.testsuite.cli.exec.AbstractExecBuilder;

import java.io.InputStream;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcinitExec extends AbstractExec {

    public static final String WORK_DIR = System.getProperty("user.dir") + "/target";

    public static final String CMD = OS_ARCH.isWindows() ? "kcinit" : "kcinit";

    private KcinitExec(String workDir, String argsLine, InputStream stdin) {
        this(workDir, argsLine, null, stdin);
    }

    private KcinitExec(String workDir, String argsLine, String env, InputStream stdin) {
        super(workDir, argsLine, env, stdin);
    }

    @Override
    public String getCmd() {
        return "./" + CMD;
    }

    public static KcinitExec.Builder newBuilder() {
        return (KcinitExec.Builder) new KcinitExec.Builder().workDir(WORK_DIR);
    }

    public static KcinitExec execute(String args) {
        return newBuilder()
                .argsLine(args)
                .execute();
    }

    public static class Builder extends AbstractExecBuilder<KcinitExec> {

        @Override
        public KcinitExec execute() {
            KcinitExec exe = new KcinitExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.execute();
            return exe;
        }

        @Override
        public KcinitExec executeAsync() {
            KcinitExec exe = new KcinitExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.executeAsync();
            return exe;
        }
    }
}
