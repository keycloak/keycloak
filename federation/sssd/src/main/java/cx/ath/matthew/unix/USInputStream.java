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
import java.io.InputStream;

public class USInputStream extends InputStream {
    public static final int MSG_DONTWAIT = 0x40;

    private native int native_recv(int sock, byte[] b, int off, int len, int flags, int timeout) throws IOException;

    private int sock;
    boolean closed = false;
    private byte[] onebuf = new byte[1];
    private UnixSocket us;
    private boolean blocking = true;
    private int flags = 0;
    private int timeout = 0;

    public USInputStream(int sock, UnixSocket us) {
        this.sock = sock;
        this.us = us;
    }

    public void close() throws IOException {
        closed = true;
        us.close();
    }

    public boolean markSupported() {
        return false;
    }

    public int read() throws IOException {
        int rv = 0;
        while (0 >= rv) rv = read(onebuf);
        if (-1 == rv) return -1;
        return 0 > onebuf[0] ? -onebuf[0] : onebuf[0];
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new NotConnectedException();
        int count = native_recv(sock, b, off, len, flags, timeout);
      /* Yes, I really want to do this. Recv returns 0 for 'connection shut down'.
       * read() returns -1 for 'end of stream.
       * Recv returns -1 for 'EAGAIN' (all other errors cause an exception to be raised)
       * whereas read() returns 0 for '0 bytes read', so yes, I really want to swap them here.
       */
        if (0 == count) return -1;
        else if (-1 == count) return 0;
        else return count;
    }

    public boolean isClosed() {
        return closed;
    }

    public UnixSocket getSocket() {
        return us;
    }

    public void setBlocking(boolean enable) {
        flags = enable ? 0 : MSG_DONTWAIT;
    }

    public void setSoTimeout(int timeout) {
        this.timeout = timeout;
    }
}
