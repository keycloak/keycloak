package org.keycloak.testsuite.cli;

import org.keycloak.testsuite.cli.exec.AbstractExec;
import org.keycloak.testsuite.cli.exec.AbstractExecBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class KcRegExec extends AbstractExec {

    public static final String WORK_DIR = System.getProperty("user.dir") + "/target/containers/keycloak-client-tools";

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