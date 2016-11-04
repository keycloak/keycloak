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

import jnr.constants.platform.linux.SocketLevel;
import jnr.posix.CmsgHdr;
import jnr.posix.MsgHdr;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.unixsocket.UnixSocketChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class USOutputStream extends OutputStream {

    private UnixSocketChannel channel;

    private int sock;
    boolean closed = false;
    private byte[] onebuf = new byte[1];
    private UnixSocket us;

    public USOutputStream(UnixSocketChannel channel, int sock, UnixSocket us) {
        this.sock = sock;
        this.us = us;
        this.channel = channel;
    }

    public void close() throws IOException {
        closed = true;
        us.close();
    }

    public void flush() {
    } // no-op, we do not buffer

    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new NotConnectedException();
        send(sock, b, off, len);
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

    /*
     * Taken from JRuby with small modifications
     * @see <a href="https://github.com/jruby/jruby/blob/master/core/src/main/java/org/jruby/ext/socket/RubyUNIXSocket.java">RubyUNIXSocket.java</a>
     */
    private void send(int sock, ByteBuffer[] outIov) {

        final POSIX posix = POSIXFactory.getNativePOSIX();
        MsgHdr outMessage = posix.allocateMsgHdr();

        outMessage.setIov(outIov);

        CmsgHdr outControl = outMessage.allocateControl(4);
        outControl.setLevel(SocketLevel.SOL_SOCKET.intValue());
        outControl.setType(0x01);

        ByteBuffer fdBuf = ByteBuffer.allocateDirect(4);
        fdBuf.order(ByteOrder.nativeOrder());
        fdBuf.putInt(0, channel.getFD());
        outControl.setData(fdBuf);

        posix.sendmsg(sock, outMessage, 0);

    }

    private void send(int sock, byte[] dataBytes, int off, int len) {
        ByteBuffer[] outIov = new ByteBuffer[1];
        outIov[0] = ByteBuffer.allocateDirect(dataBytes.length);
        outIov[0].put(dataBytes, off, len);
        outIov[0].flip();

        send(sock, outIov);
    }

    protected void send(int sock, byte[] dataBytes) {
        ByteBuffer[] outIov = new ByteBuffer[1];
        outIov[0] = ByteBuffer.allocateDirect(dataBytes.length);
        outIov[0].put(dataBytes);
        outIov[0].flip();

        send(sock, outIov);
    }
}
