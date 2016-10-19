package org.keycloak.testsuite.cli;

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
public class KcRegExec {

    public static final String WORK_DIR = System.getProperty("user.dir") + "/target/containers/keycloak-client-tools";

    public static final OsArch OS_ARCH = OsUtils.determineOSAndArch();

    public static final String CMD = OS_ARCH.isWindows() ? "kcreg.bat" : "kcreg.sh";

    private long waitTimeout = 30000;

    private Process process;

    private int exitCode = -1;

    private boolean logStreams = Boolean.valueOf(System.getProperty("cli.log.output", "true"));

    private boolean dumpStreams;

    private String workDir = WORK_DIR;

    private String env;

    private String argsLine;

    private ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private InputStream stdin = new InteractiveInputStream();

    private Throwable err;

    private KcRegExec(String workDir, String argsLine, InputStream stdin) {
        this(workDir, argsLine, null, stdin);
    }

    private KcRegExec(String workDir, String argsLine, String env, InputStream stdin) {
        if (workDir != null) {
            this.workDir = workDir;
        }

        this.argsLine = argsLine;
        this.env = env;

        if (stdin != null) {
            this.stdin = stdin;
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static KcRegExec execute(String args) {
        return newBuilder()
                .argsLine(args)
                .execute();
    }

    public void execute() {
        executeAsync();
        if (err == null) {
            waitCompletion();
        }
    }


    public void executeAsync() {

        try {
            if (OS_ARCH.isWindows()) {
                String cmd = (env != null ? "set " + env + " & " : "") + "bin\\" + CMD + " " + fixQuotes(argsLine);
                System.out.println("Executing: cmd.exe /c " + cmd);
                process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", cmd}, null, new File(workDir));
            } else {
                String cmd = (env != null ? env + " " : "") + "bin/" + CMD + " " + argsLine;
                System.out.println("Executing: sh -c " + cmd);
                process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd}, null, new File(workDir));
            }

            new StreamReaderThread(process.getInputStream(), logStreams ? new LoggingOutputStream("STDOUT", stdout) : stdout)
                    .start();

            new StreamReaderThread(process.getErrorStream(), logStreams ? new LoggingOutputStream("STDERR", stderr) : stderr)
                    .start();

            new StreamReaderThread(stdin, process.getOutputStream())
                    .start();

        } catch (Throwable t) {
            err = t;
        }
    }

    private String fixQuotes(String argsLine) {
        argsLine = argsLine + " ";
        argsLine = argsLine.replaceAll("\"", "\\\\\"");
        argsLine = argsLine.replaceAll(" '", " \"");
        argsLine = argsLine.replaceAll("' ", "\" ");
        return argsLine;
    }

    public void waitCompletion() {

        //if (stdin instanceof InteractiveInputStream) {
        //    ((InteractiveInputStream) stdin).close();
        //}
        try {
            if (process.waitFor(waitTimeout, TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
                if (exitCode != 0) {
                    dumpStreams = true;
                }
            } else {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                throw new RuntimeException("Timeout after " + (waitTimeout / 1000) + " seconds.");
            }
        } catch (InterruptedException e) {
            dumpStreams = true;
            throw new RuntimeException("Interrupted ...", e);
        } catch (Throwable t) {
            dumpStreams = true;
            err = t;
        } finally {
            if (!logStreams && dumpStreams) try {
                System.out.println("STDOUT: ");
                copyStream(new ByteArrayInputStream(stdout.toByteArray()), System.out);
                System.out.println("STDERR: ");
                copyStream(new ByteArrayInputStream(stderr.toByteArray()), System.out);
            } catch (Exception ignored) {
            }
        }
    }

    public int exitCode() {
        return exitCode;
    }

    public Throwable error() {
        return err;
    }

    public InputStream stdout() {
        return new ByteArrayInputStream(stdout.toByteArray());
    }

    public List<String> stdoutLines() {
        return parseStreamAsLines(new ByteArrayInputStream(stdout.toByteArray()));
    }

    public String stdoutString() {
        return new String(stdout.toByteArray());
    }

    public InputStream stderr() {
        return new ByteArrayInputStream(stderr.toByteArray());
    }

    public List<String> stderrLines() {
        return parseStreamAsLines(new ByteArrayInputStream(stderr.toByteArray()));
    }

    public String stderrString() {
        return new String(stderr.toByteArray());
    }

    static List<String> parseStreamAsLines(InputStream stream) {
        List<String> lines = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException("Unexpected I/O error", e);
        }
    }

    public void waitForStdout(String content) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < waitTimeout) {
            if (stdoutString().indexOf(content) != -1) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted ...", e);
            }
        }

        throw new RuntimeException("Timed while waiting for content to appear in stdout");
    }

    public void sendToStdin(String s) {
        if (stdin instanceof InteractiveInputStream) {
            ((InteractiveInputStream) stdin).pushBytes(s.getBytes());
        } else {
            throw new RuntimeException("Can't push to stdin - not interactive");
        }
    }

    static class StreamReaderThread extends Thread {

        private InputStream is;
        private OutputStream os;

        StreamReaderThread(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        public void run() {
            try {
                copyStream(is, os);
            } catch (IOException e) {
                throw new RuntimeException("Unexpected I/O error", e);
            } finally {
                try {
                    os.close();
                } catch (IOException ignored) {
                    System.err.print("IGNORED: error while closing output stream: ");
                    ignored.printStackTrace();
                }
            }
        }
    }

    static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte [] buf = new byte[8192];

        try (InputStream iss = is) {
            int c;
            while ((c = iss.read(buf)) != -1) {
                os.write(buf, 0, c);
                os.flush();
            }
        }
    }

    public static class Builder {

        private String workDir;
        private String argsLine;
        private InputStream stdin;
        private String env;
        private boolean dumpStreams;

        public Builder workDir(String path) {
            this.workDir = path;
            return this;
        }

        public Builder argsLine(String cmd) {
            this.argsLine = cmd;
            return this;
        }

        public Builder stdin(InputStream is) {
            this.stdin = is;
            return this;
        }

        public Builder env(String env) {
            this.env = env;
            return this;
        }

        public Builder fullStreamDump() {
            this.dumpStreams = true;
            return this;
        }

        public KcRegExec execute() {
            KcRegExec exe = new KcRegExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.execute();
            return exe;
        }

        public KcRegExec executeAsync() {
            KcRegExec exe = new KcRegExec(workDir, argsLine, env, stdin);
            exe.dumpStreams = dumpStreams;
            exe.executeAsync();
            return exe;
        }
    }

    static class NullInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return -1;
        }
    }

    static class InteractiveInputStream extends InputStream {

        private LinkedList<Byte> queue = new LinkedList<>();

        private Thread consumer;

        private boolean closed;

        @Override
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {

            Byte current = null;
            int rc = 0;
            try {
                consumer = Thread.currentThread();

                do {
                    current = queue.poll();
                    if (current == null) {
                        if (rc > 0) {
                            return rc;
                        } else {
                            do {
                                if (closed) {
                                    return -1;
                                }
                                wait();
                            }
                            while ((current = queue.poll()) == null);
                        }
                    }

                    b[off + rc] = current;
                    rc++;
                } while (rc < len);

            } catch (InterruptedException e) {
                throw new InterruptedIOException("Signalled to exit");
            } finally {
                consumer = null;
            }
            return rc;
        }

        @Override
        public long skip(long n) throws IOException {
            return super.skip(n);
        }

        @Override
        public int available() throws IOException {
            return super.available();
        }

        @Override
        public synchronized void mark(int readlimit) {
            super.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            super.reset();
        }

        @Override
        public boolean markSupported() {
            return super.markSupported();
        }

        @Override
        public synchronized int read() throws IOException {
            if (closed) {
                return -1;
            }

            // when input is available pass it on
            Byte current;
            try {
                consumer = Thread.currentThread();

                while ((current = queue.poll()) == null) {
                    wait();
                    if (closed) {
                        return -1;
                    }
                }

            } catch (InterruptedException e) {
                throw new InterruptedIOException("Signalled to exit");
            } finally {
                consumer = null;
            }
            return current;
        }

        @Override
        public synchronized void close() {
            closed = true;
            new RuntimeException("IIS || close").printStackTrace();
            if (consumer != null) {
                consumer.interrupt();
            }
        }

        public synchronized void pushBytes(byte [] buff) {
            for (byte b : buff) {
                queue.add(b);
            }
            notify();
        }
    }


    static class LoggingOutputStream extends FilterOutputStream {

        private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private String name;

        public LoggingOutputStream(String name, OutputStream os) {
            super(os);
            this.name = name;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            if (b == 10) {
                log();
            } else {
                buffer.write(b);
            }
        }

        @Override
        public void write(byte[] buf) throws IOException {
            write(buf, 0, buf.length);
        }

        @Override
        public void write(byte[] buf, int offs, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                write(buf[offs+i]);
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (buffer.size() > 0) {
                log();
            }
        }

        private void log() {
            String log = new String(buffer.toByteArray());
            buffer.reset();
            System.out.println("[" + name + "] " + log);
        }
    }

}