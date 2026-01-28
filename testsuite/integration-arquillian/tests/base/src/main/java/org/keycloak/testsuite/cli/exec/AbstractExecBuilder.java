package org.keycloak.testsuite.cli.exec;

import java.io.InputStream;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public abstract class AbstractExecBuilder<T> {

    protected String workDir;
    protected String argsLine;
    protected InputStream stdin;
    protected String env;
    protected boolean dumpStreams;

    public AbstractExecBuilder<T> workDir(String path) {
        this.workDir = path;
        return this;
    }

    public AbstractExecBuilder<T> argsLine(String cmd) {
        this.argsLine = cmd;
        return this;
    }

    public AbstractExecBuilder<T> stdin(InputStream is) {
        this.stdin = is;
        return this;
    }

    public AbstractExecBuilder<T> env(String env) {
        this.env = env;
        return this;
    }

    public AbstractExecBuilder<T> fullStreamDump() {
        this.dumpStreams = true;
        return this;
    }

    public abstract T execute();

    public abstract T executeAsync();
}
