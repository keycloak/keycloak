package org.keycloak.client.registration.cli.aesh;

import org.jboss.aesh.console.AeshConsoleImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This stream blocks and waits, until there is some stream in the queue.
 * It reads all streams from the queue, and then blocks until it receives more.
 */
public class ValveInputStream extends InputStream {

    private BlockingQueue<InputStream> queue = new LinkedBlockingQueue<>(10);

    private InputStream current;

    private AeshConsoleImpl console;

    @Override
    public int read() throws IOException {
        if (current == null) {
            try {
                current = queue.take();
            } catch (InterruptedException e) {
                throw new InterruptedIOException("Signalled to exit");
            }
        }
        int c = current.read();
        if (c == -1) {
            //current = null;
            if (console != null) {
                console.stop();
            }
        }

        return c;
    }

    /**
     * For some reason AeshInputStream wants to do blocking read of whole buffers, which for stdin
     * results in blocked input.
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int c = read();
        if (c == -1) {
            return c;
        }
        b[off] = (byte) c;
        return 1;
    }

    public void setInputStream(InputStream is) {
        if (queue.contains(is)) {
            return;
        }
        queue.add(is);
    }

    public void setConsole(AeshConsoleImpl console) {
        this.console = console;
    }

    public boolean isStdinAvailable() {
        return console.isRunning();
    }
}
