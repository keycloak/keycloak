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

import cx.ath.matthew.debug.Debug;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents a UnixSocket.
 */
public class UnixSocket {

    private UnixSocketChannel channel;

    private UnixSocketAddress address = null;
    private USOutputStream os = null;
    private USInputStream is = null;
    private boolean closed = false;
    private boolean connected = false;
    private boolean passcred = false;
    private int sock = 0;
    private boolean blocking = true;
    private int uid = -1;
    private int pid = -1;
    private int gid = -1;

    UnixSocket(int sock, UnixSocketAddress address) {
        this.sock = sock;
        this.address = address;
        this.connected = true;
        this.os = new USOutputStream(channel, sock, this);
        this.is = new USInputStream(channel, this);
    }

    /**
     * Create an unconnected socket.
     */
    public UnixSocket() {
    }

    /**
     * Create a socket connected to the given address.
     *
     * @param address The Unix Socket address to connect to
     */
    public UnixSocket(UnixSocketAddress address) throws IOException {
        connect(address);
    }

    /**
     * Create a socket connected to the given address.
     *
     * @param address The Unix Socket address to connect to
     */
    public UnixSocket(String address) throws IOException {
        this(new UnixSocketAddress(new File(address)));
    }

    /**
     * Connect the socket to this address.
     *
     * @param address The Unix Socket address to connect to
     */
    public void connect(UnixSocketAddress address) throws IOException {
        if (connected) close();
        this.channel = UnixSocketChannel.open(address);
        this.channel = UnixSocketChannel.open(address);
        this.sock = channel.getFD();
        this.os = new USOutputStream(channel, sock, this);
        this.is = new USInputStream(channel, this);
        this.address = address;
        this.connected = true;
        this.closed = false;
        this.is.setBlocking(blocking);
    }

    /**
     * Connect the socket to this address.
     *
     * @param address The Unix Socket address to connect to
     */
    public void connect(String address) throws IOException {
        connect(new UnixSocketAddress(new File(address)));
    }

    public void finalize() {
        try {
            close();
        } catch (IOException IOe) {
        }
    }

    /**
     * Closes the connection.
     */
    public synchronized void close() throws IOException {
        if (Debug.debug) Debug.print(Debug.INFO, "Closing socket");
        channel.close();
        sock = 0;
        this.closed = true;
        this.connected = false;
        os = null;
        is = null;
    }

    /**
     * Returns an InputStream for reading from the socket.
     *
     * @return An InputStream connected to this socket.
     */
    public InputStream getInputStream() {
        return is;
    }

    /**
     * Returns an OutputStream for writing to the socket.
     *
     * @return An OutputStream connected to this socket.
     */
    public OutputStream getOutputStream() {
        return os;
    }

    /**
     * Returns the address this socket is connected to.
     * Returns null if the socket is unconnected.
     *
     * @return The UnixSocketAddress the socket is connected to
     */
    public UnixSocketAddress getAddress() {
        return address;
    }

    /**
     * Send a single byte of data with credentials.
     * (Works on BSDs)
     *
     * @param data The byte of data to send.
     */
    public void sendCredentialByte(byte data) throws IOException {
        if (!connected) throw new NotConnectedException();
            os.send(channel.getFD(), new byte[]{ data });
    }

    /**
     * Get the blocking mode.
     *
     * @return true if reads are blocking.
     * @see setBlocking
     */
    public boolean getBlocking() {
        return blocking;
    }

    /**
     * Set the blocking mode.
     *
     * @param enable Set to false for non-blocking reads.
     */
    public void setBlocking(boolean enable) {
        blocking = enable;
        if (null != is) is.setBlocking(enable);
    }

    /**
     * Check the socket status.
     *
     * @return true if closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Check the socket status.
     *
     * @return true if connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Check the socket status.
     *
     * @return true if the input stream has been shutdown
     */
    public boolean isInputShutdown() {
        return is.isClosed();
    }

    /**
     * Check the socket status.
     *
     * @return true if the output stream has been shutdown
     */
    public boolean isOutputShutdown() {
        return os.isClosed();
    }

    /**
     * Shuts down the input stream.
     * Subsequent reads on the associated InputStream will fail.
     */
    public void shutdownInput() {
        is.closed = true;
    }

    /**
     * Shuts down the output stream.
     * Subsequent writes to the associated OutputStream will fail.
     */
    public void shutdownOutput() {
        os.closed = true;
    }

    /**
     * Set timeout of read requests.
     */
    public void setSoTimeout(int timeout) {
        is.setSoTimeout(timeout);
    }
}
