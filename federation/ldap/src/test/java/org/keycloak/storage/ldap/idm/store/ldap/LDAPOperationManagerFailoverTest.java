/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.naming.AuthenticationException;
import javax.naming.ldap.LdapName;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.idm.store.ldap.control.PasswordPolicyControl;
import org.keycloak.storage.ldap.idm.store.ldap.control.PasswordPolicyPasswordChangeException;
import org.keycloak.tracing.NoopTracingProvider;
import org.keycloak.tracing.TracingProvider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests that user authentication fails over to the next LDAP server of a multi-URL connection URL when the first
 * server accepts the TCP connection but never answers the bind request.
 */
public class LDAPOperationManagerFailoverTest {

    private static final String USER_DN = "uid=john,dc=example,dc=org";

    // SEQUENCE { error changeAfterReset(2) }
    private static final byte[] CHANGE_AFTER_RESET = { 0x30, 0x03, (byte) 0x81, 0x01, 0x02 };

    private BlackHoleServer unresponsiveServer;
    private FakeLdapServer workingServer;

    @Before
    public void before() throws IOException {
        unresponsiveServer = new BlackHoleServer();
        workingServer = new FakeLdapServer();
    }

    @After
    public void after() {
        unresponsiveServer.close();
        workingServer.close();
    }

    @Test
    public void authenticateFailsOverToNextUrlWhenBindTimesOut() throws Exception {
        newOperationManager(false).authenticate(new LdapName(USER_DN), "secret");

        Assert.assertTrue("Unresponsive server should have been contacted first", unresponsiveServer.accepted > 0);
        Assert.assertEquals(List.of(USER_DN), workingServer.bindDNs);
    }

    @Test
    public void authenticateWithInvalidCredentialsOnNextUrl() throws Exception {
        workingServer.resultCode = 49; // invalidCredentials

        Assert.assertThrows(AuthenticationException.class,
                () -> newOperationManager(false).authenticate(new LdapName(USER_DN), "wrong"));
        Assert.assertEquals(List.of(USER_DN), workingServer.bindDNs);
    }

    @Test
    public void authenticateReceivesPasswordPolicyControlAfterFailover() throws Exception {
        workingServer.passwordPolicyResponse = CHANGE_AFTER_RESET;

        Assert.assertThrows(PasswordPolicyPasswordChangeException.class,
                () -> newOperationManager(true).authenticate(new LdapName(USER_DN), "secret"));
        Assert.assertEquals(List.of(USER_DN), workingServer.bindDNs);
        Assert.assertTrue("Password policy request control should be sent with the bind", workingServer.passwordPolicyRequested);
    }

    private LDAPOperationManager newOperationManager(boolean passwordPolicy) {
        MultivaluedHashMap<String, String> cfg = new MultivaluedHashMap<>();
        cfg.add(LDAPConstants.CONNECTION_URL, "ldap://127.0.0.1:" + unresponsiveServer.getPort() + " ldap://127.0.0.1:" + workingServer.getPort());
        cfg.add(LDAPConstants.AUTH_TYPE, LDAPConstants.AUTH_TYPE_NONE);
        cfg.add(LDAPConstants.CONNECTION_TIMEOUT, "500");
        cfg.add(LDAPConstants.READ_TIMEOUT, "500");
        cfg.add(LDAPConstants.ENABLE_LDAP_PASSWORD_POLICY, String.valueOf(passwordPolicy));

        TracingProvider tracing = new NoopTracingProvider();
        KeycloakSession session = (KeycloakSession) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { KeycloakSession.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getProvider") && args.length == 1 && args[0] == TracingProvider.class) {
                        return tracing;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });

        return new LDAPOperationManager(session, new LDAPConfig(cfg), null);
    }

    private abstract static class AbstractServer implements Runnable {

        protected final ServerSocket serverSocket;
        protected final List<Socket> sockets = new CopyOnWriteArrayList<>();

        AbstractServer() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread thread = new Thread(this, getClass().getSimpleName());
            thread.setDaemon(true);
            thread.start();
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    sockets.add(socket);
                    handle(socket);
                } catch (IOException e) {
                    // closed
                }
            }
        }

        abstract void handle(Socket socket);

        void close() {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
            for (Socket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    /**
     * Accepts TCP connections but never answers, simulating a degraded or paused LDAP server.
     */
    private static class BlackHoleServer extends AbstractServer {

        volatile int accepted;

        BlackHoleServer() throws IOException {
            super();
        }

        @Override
        void handle(Socket socket) {
            accepted++;
        }
    }

    /**
     * Minimal LDAP server answering simple bind requests.
     */
    private static class FakeLdapServer extends AbstractServer {

        final List<String> bindDNs = new CopyOnWriteArrayList<>();
        volatile int resultCode = 0;
        volatile byte[] passwordPolicyResponse;
        volatile boolean passwordPolicyRequested;

        FakeLdapServer() throws IOException {
            super();
        }

        @Override
        void handle(Socket socket) {
            Thread thread = new Thread(() -> serve(socket), "FakeLdapServer-connection");
            thread.setDaemon(true);
            thread.start();
        }

        private void serve(Socket socket) {
            try (socket) {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                OutputStream out = socket.getOutputStream();
                while (true) {
                    // LDAPMessage ::= SEQUENCE { messageID, protocolOp, controls [0] OPTIONAL }
                    if (in.read() != 0x30) {
                        return;
                    }
                    byte[] message = readBytes(in, readLength(in));
                    Tlv messageId = Tlv.parse(message, 0);
                    Tlv op = Tlv.parse(message, messageId.end);

                    if (op.tag == 0x60) { // BindRequest ::= [APPLICATION 0] SEQUENCE { version, name, authentication }
                        Tlv version = Tlv.parse(message, op.valueStart);
                        Tlv name = Tlv.parse(message, version.end);
                        bindDNs.add(new String(message, name.valueStart, name.length, StandardCharsets.UTF_8));
                        if (op.end < message.length) {
                            passwordPolicyRequested = new String(message, op.end, message.length - op.end, StandardCharsets.US_ASCII)
                                    .contains(PasswordPolicyControl.OID);
                        }
                        out.write(bindResponse(message, messageId));
                        out.flush();
                    } else if (op.tag == 0x42) { // UnbindRequest
                        return;
                    }
                }
            } catch (IOException e) {
                // connection closed
            }
        }

        private byte[] bindResponse(byte[] message, Tlv messageId) {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(message, messageId.start, messageId.end - messageId.start);
            // BindResponse ::= [APPLICATION 1] SEQUENCE { resultCode, matchedDN, diagnosticMessage }
            body.writeBytes(tlv(0x61, new byte[] { 0x0A, 0x01, (byte) resultCode, 0x04, 0x00, 0x04, 0x00 }));
            byte[] ppolicy = passwordPolicyResponse;
            if (ppolicy != null) {
                ByteArrayOutputStream control = new ByteArrayOutputStream();
                control.writeBytes(tlv(0x04, PasswordPolicyControl.OID.getBytes(StandardCharsets.US_ASCII)));
                control.writeBytes(tlv(0x04, ppolicy));
                body.writeBytes(tlv(0xA0, tlv(0x30, control.toByteArray())));
            }
            return tlv(0x30, body.toByteArray());
        }

        private static byte[] tlv(int tag, byte[] value) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(tag);
            if (value.length < 0x80) {
                out.write(value.length);
            } else {
                out.write(0x82);
                out.write(value.length >> 8);
                out.write(value.length & 0xFF);
            }
            out.writeBytes(value);
            return out.toByteArray();
        }

        private static int readLength(InputStream in) throws IOException {
            int first = in.read();
            if (first < 0x80) {
                return first;
            }
            int length = 0;
            for (int i = 0; i < (first & 0x7F); i++) {
                length = (length << 8) | in.read();
            }
            return length;
        }

        private static byte[] readBytes(DataInputStream in, int length) throws IOException {
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            return bytes;
        }
    }

    private record Tlv(int tag, int start, int valueStart, int length, int end) {

        static Tlv parse(byte[] data, int offset) {
            int tag = data[offset] & 0xFF;
            int pos = offset + 1;
            int first = data[pos++] & 0xFF;
            int length = first;
            if (first >= 0x80) {
                length = 0;
                for (int i = 0; i < (first & 0x7F); i++) {
                    length = (length << 8) | (data[pos++] & 0xFF);
                }
            }
            return new Tlv(tag, offset, pos, length, pos + length);
        }
    }
}
