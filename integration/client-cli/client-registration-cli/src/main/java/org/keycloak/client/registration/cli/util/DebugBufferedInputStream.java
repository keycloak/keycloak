package org.keycloak.client.registration.cli.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class DebugBufferedInputStream extends BufferedInputStream {

    public DebugBufferedInputStream(InputStream in) {
        super(in);
    }

    @Override
    public synchronized int read() throws IOException {
        log("read() >>>");
        int b = super.read();
        log("read() <<< " + (char) b + " (" + b + ")");
        return b;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        log("read(buf, off, len) >>>");
        int c = super.read(b, off, len);
        log("read(buf, off, len) <<< " + (c != -1 ? "[" + new String(b, off, c) + "]" : "-1"));
        return c;
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        log("skip()");
        return super.skip(n);
    }

    @Override
    public synchronized int available() throws IOException {
        log("available() >>>");
        int c = super.available();
        log("available() >>> " + c);
        return c;
    }

    @Override
    public synchronized void mark(int readlimit) {
        log("mark()");
        super.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        log("reset()");
        super.reset();
    }

    @Override
    public boolean markSupported() {
        log("markSupported()");
        return super.markSupported();
    }

    @Override
    public void close() throws IOException {
        log("close()");
        super.close();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    private void log(String msg) {
        System.err.println(msg);
    }
}
