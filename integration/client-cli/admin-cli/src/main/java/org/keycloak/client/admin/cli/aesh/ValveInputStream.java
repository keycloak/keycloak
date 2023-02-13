/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.client.admin.cli.aesh;

import org.jboss.aesh.console.AeshConsoleImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This stream blocks and waits, until there is a stream in the queue.
 * It reads the stream to the end, then stops Aesh console.
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
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
