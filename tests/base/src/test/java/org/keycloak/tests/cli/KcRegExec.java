package org.keycloak.tests.cli;

import java.io.InputStream;
import java.util.List;

import org.keycloak.tests.cli.exec.AbstractExec;
import org.keycloak.tests.cli.exec.AbstractExecBuilder;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegExec extends AbstractExec {

    public static final String WORK_DIR = System.getProperty(CLI_TOOLS_DIR_PROPERTY,
            System.getProperty("user.dir") + "/target/containers/keycloak-client-tools");

    public static final String CMD = OS_ARCH.isWindows() ? "kcreg.bat" : "kcreg.sh";

    private KcRegExec(String workDir, String argsLine, InputStream stdin) {
        this(workDir, argsLine, null, stdin);
    }

    private KcRegExec(String workDir, String argsLine, String env, InputStream stdin) {
        super(workDir, argsLine, env, stdin);
    }

    @Override
    public String getCmd() {
        return "bin/" + CMD;
    }

    public static KcRegExec.Builder newBuilder() {
        return (KcRegExec.Builder) new KcRegExec.Builder().workDir(WORK_DIR);
    }

    public static KcRegExec execute(String args) {
        return newBuilder()
                .argsLine(args)
                .execute();
    }

    @Override
    public List<String> stderrLines() {
        List<String> lines = super.stderrLines();
        // remove the two lines with the BC provider info if FIPS
        return !isFips() || lines.size() < 2
            ? lines
            : lines.subList(2, lines.size());
    }

    public static class Builder extends AbstractExecBuilder<KcRegExec> {

        @Override
        public KcRegExec execute() {
            KcRegExec exe = new KcRegExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.execute();
            return exe;
        }

        @Override
        public KcRegExec executeAsync() {
            KcRegExec exe = new KcRegExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.executeAsync();
            return exe;
        }
    }

}
