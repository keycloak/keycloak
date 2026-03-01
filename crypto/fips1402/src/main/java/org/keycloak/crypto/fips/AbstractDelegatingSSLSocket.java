/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.crypto.fips;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.function.BiFunction;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * Forked from wildfly-elytron
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractDelegatingSSLSocket extends SSLSocket {

    private final SSLSocket delegate;

    AbstractDelegatingSSLSocket(final SSLSocket delegate) {
        this.delegate = delegate;
    }

    public String getApplicationProtocol() {
        return delegate.getApplicationProtocol();
    }

    public String getHandshakeApplicationProtocol() {
        return delegate.getHandshakeApplicationProtocol();
    }

    public void setHandshakeApplicationProtocolSelector(BiFunction<SSLSocket, List<String>, String> selector) {
        delegate.setHandshakeApplicationProtocolSelector(selector);
    }

    public BiFunction<SSLSocket, List<String>, String> getHandshakeApplicationProtocolSelector() {
        return delegate.getHandshakeApplicationProtocolSelector();
    }

    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    public String[] getEnabledCipherSuites() {
        return delegate.getEnabledCipherSuites();
    }

    public void setEnabledCipherSuites(final String[] suites) {
        delegate.setEnabledCipherSuites(suites);
    }

    public String[] getSupportedProtocols() {
        return delegate.getSupportedProtocols();
    }

    public String[] getEnabledProtocols() {
        return delegate.getEnabledProtocols();
    }

    public void setEnabledProtocols(final String[] protocols) {
        delegate.setEnabledProtocols(protocols);
    }

    public SSLSession getSession() {
        return delegate.getSession();
    }

    public SSLSession getHandshakeSession() {
        return delegate.getHandshakeSession();
    }

    public void addHandshakeCompletedListener(final HandshakeCompletedListener listener) {
        delegate.addHandshakeCompletedListener(listener);
    }

    public void removeHandshakeCompletedListener(final HandshakeCompletedListener listener) {
        delegate.removeHandshakeCompletedListener(listener);
    }

    public void startHandshake() throws IOException {
        delegate.startHandshake();
    }

    public void setUseClientMode(final boolean mode) {
        delegate.setUseClientMode(mode);
    }

    public boolean getUseClientMode() {
        return delegate.getUseClientMode();
    }

    public void setNeedClientAuth(final boolean need) {
        delegate.setNeedClientAuth(need);
    }

    public boolean getNeedClientAuth() {
        return delegate.getNeedClientAuth();
    }

    public void setWantClientAuth(final boolean want) {
        delegate.setWantClientAuth(want);
    }

    public boolean getWantClientAuth() {
        return delegate.getWantClientAuth();
    }

    public void setEnableSessionCreation(final boolean flag) {
        delegate.setEnableSessionCreation(flag);
    }

    public boolean getEnableSessionCreation() {
        return delegate.getEnableSessionCreation();
    }

    public SSLParameters getSSLParameters() {
        return delegate.getSSLParameters();
    }

    public void setSSLParameters(final SSLParameters params) {
        delegate.setSSLParameters(params);
    }

    public void connect(final SocketAddress endpoint) throws IOException {
        delegate.connect(endpoint);
    }

    public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
        delegate.connect(endpoint, timeout);
    }

    public void bind(final SocketAddress bindpoint) throws IOException {
        delegate.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    public int getPort() {
        return delegate.getPort();
    }

    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    public SocketChannel getChannel() {
        return delegate.getChannel();
    }

    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    public void setTcpNoDelay(final boolean on) throws SocketException {
        delegate.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return delegate.getTcpNoDelay();
    }

    public void setSoLinger(final boolean on, final int linger) throws SocketException {
        delegate.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return delegate.getSoLinger();
    }

    public void sendUrgentData(final int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    public void setOOBInline(final boolean on) throws SocketException {
        delegate.setOOBInline(on);
    }

    public boolean getOOBInline() throws SocketException {
        return delegate.getOOBInline();
    }

    public void setSoTimeout(final int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    public int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    public void setSendBufferSize(final int size) throws SocketException {
        delegate.setSendBufferSize(size);
    }

    public int getSendBufferSize() throws SocketException {
        return delegate.getSendBufferSize();
    }

    public void setReceiveBufferSize(final int size) throws SocketException {
        delegate.setReceiveBufferSize(size);
    }

    public int getReceiveBufferSize() throws SocketException {
        return delegate.getReceiveBufferSize();
    }

    public void setKeepAlive(final boolean on) throws SocketException {
        delegate.setKeepAlive(on);
    }

    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }

    public void setTrafficClass(final int tc) throws SocketException {
        delegate.setTrafficClass(tc);
    }

    public int getTrafficClass() throws SocketException {
        return delegate.getTrafficClass();
    }

    public void setReuseAddress(final boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    public void close() throws IOException {
        delegate.close();
    }

    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    public String toString() {
        return delegate.toString();
    }

    public boolean isConnected() {
        return delegate.isConnected();
    }

    public boolean isBound() {
        return delegate.isBound();
    }

    public boolean isClosed() {
        return delegate.isClosed();
    }

    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    protected SSLSocket getDelegate() {
        return delegate;
    }
}
