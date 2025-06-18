package org.keycloak.testsuite.cli.exec;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class LoggingOutputStream extends FilterOutputStream {

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