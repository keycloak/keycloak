package org.keycloak.testsuite.cli.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.keycloak.testsuite.cli.exec.AbstractExec.copyStream;

class StreamReaderThread extends Thread {

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