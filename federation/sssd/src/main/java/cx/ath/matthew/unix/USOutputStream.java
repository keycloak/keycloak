/*
 * Java Unix Sockets Library
 *
 * Copyright (c) Matthew Johnson 2004
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 * To Contact the author, please email src@matthew.ath.cx
 *
 */
package cx.ath.matthew.unix;

import java.io.IOException;
import java.io.OutputStream;

public class USOutputStream extends OutputStream {
    private native int native_send(int sock, byte[] b, int off, int len) throws IOException;

    private native int native_send(int sock, byte[][] b) throws IOException;

    private int sock;
    boolean closed = false;
    private byte[] onebuf = new byte[1];
    private UnixSocket us;

    public USOutputStream(int sock, UnixSocket us) {
        this.sock = sock;
        this.us = us;
    }

    public void close() throws IOException {
        closed = true;
        us.close();
    }

    public void flush() {
    } // no-op, we do not buffer

    public void write(byte[][] b) throws IOException {
        if (closed) throw new NotConnectedException();
        native_send(sock, b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new NotConnectedException();
        native_send(sock, b, off, len);
    }

    public void write(int b) throws IOException {
        onebuf[0] = (byte) (b % 0x7F);
        if (1 == (b % 0x80)) onebuf[0] = (byte) -onebuf[0];
        write(onebuf);
    }

    public boolean isClosed() {
        return closed;
    }

    public UnixSocket getSocket() {
        return us;
    }
}
