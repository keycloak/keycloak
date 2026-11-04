package org.keycloak.tests.x509;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal LDAP server for the tests. It accepts any bind and answers any search with a single entry whose
 * {@code certificateRevocationList;binary} attribute holds the configured bytes. An unresponsive variant accepts
 * connections and reads requests, but never answers them.
 */
final class LdapCrlServer implements AutoCloseable {

    private static final String CRL_ATTRIBUTE = "certificateRevocationList;binary";

    // LDAP protocol operation tags, see RFC 4511
    private static final int BIND_REQUEST = 0x60;
    private static final int BIND_RESPONSE = 0x61;
    private static final int UNBIND_REQUEST = 0x42;
    private static final int SEARCH_REQUEST = 0x63;
    private static final int SEARCH_RESULT_ENTRY = 0x64;
    private static final int SEARCH_RESULT_DONE = 0x65;

    // universal ASN.1 tags
    private static final int INTEGER = 0x02;
    private static final int OCTET_STRING = 0x04;
    private static final int ENUMERATED = 0x0a;
    private static final int SEQUENCE = 0x30;
    private static final int SET = 0x31;

    private final ServerSocket serverSocket;
    private final byte[] crl;
    // unique DN, so the URL never matches a CRL cached by a previous test that used the same port
    private final String dn = "cn=crl-" + UUID.randomUUID() + ",dc=keycloak,dc=org";
    private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "ldap-crl-server");
        thread.setDaemon(true);
        return thread;
    });
    private final List<Socket> clients = new CopyOnWriteArrayList<>();

    private LdapCrlServer(byte[] crl) throws IOException {
        this.crl = crl;
        this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        executor.submit(this::accept);
    }

    /**
     * Starts a server returning the given CRL for every search.
     */
    static LdapCrlServer servingCrl(byte[] crl) throws IOException {
        return new LdapCrlServer(crl);
    }

    /**
     * Starts a server that accepts connections but never answers any request.
     */
    static LdapCrlServer unresponsive() throws IOException {
        return new LdapCrlServer(null);
    }

    /**
     * @return the LDAP URL to use as CRL path
     */
    String crlUrl() {
        return "ldap://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + "/" + dn;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
        for (Socket client : clients) {
            client.close();
        }
        executor.shutdownNow();
    }

    private void accept() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                clients.add(client);
                executor.submit(() -> handle(client));
            } catch (IOException e) {
                // server closed
            }
        }
    }

    private void handle(Socket client) {
        try (client) {
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            Element message;
            while ((message = readElement(in)) != null) {
                if (crl == null) {
                    continue; // unresponsive server, never answer
                }
                InputStream content = new ByteArrayInputStream(message.content);
                byte[] messageId = readElement(content).content;
                switch (readElement(content).tag) {
                    case BIND_REQUEST -> out.write(ldapMessage(messageId, element(BIND_RESPONSE, success())));
                    case SEARCH_REQUEST -> {
                        out.write(ldapMessage(messageId, element(SEARCH_RESULT_ENTRY, searchEntry())));
                        out.write(ldapMessage(messageId, element(SEARCH_RESULT_DONE, success())));
                    }
                    case UNBIND_REQUEST -> {
                        return;
                    }
                    default -> {
                        // ignore anything else
                    }
                }
                out.flush();
            }
        } catch (IOException e) {
            // client went away, nothing to do
        }
    }

    private byte[] searchEntry() throws IOException {
        byte[] values = element(SET, element(OCTET_STRING, crl));
        byte[] attribute = element(SEQUENCE, concat(element(OCTET_STRING, CRL_ATTRIBUTE.getBytes(StandardCharsets.UTF_8)), values));
        return concat(element(OCTET_STRING, dn.getBytes(StandardCharsets.UTF_8)), element(SEQUENCE, attribute));
    }

    // LDAPResult with resultCode success, empty matchedDN and empty diagnosticMessage
    private static byte[] success() throws IOException {
        return concat(element(ENUMERATED, new byte[] { 0 }), element(OCTET_STRING, new byte[0]), element(OCTET_STRING, new byte[0]));
    }

    private static byte[] ldapMessage(byte[] messageId, byte[] protocolOp) throws IOException {
        return element(SEQUENCE, concat(element(INTEGER, messageId), protocolOp));
    }

    private static byte[] element(int tag, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length + 6);
        out.write(tag);
        if (content.length < 0x80) {
            out.write(content.length);
        } else {
            int lengthBytes = (Integer.SIZE - Integer.numberOfLeadingZeros(content.length) + 7) / 8;
            out.write(0x80 | lengthBytes);
            for (int i = lengthBytes - 1; i >= 0; i--) {
                out.write(content.length >>> (8 * i));
            }
        }
        out.write(content);
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... parts) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part);
        }
        return out.toByteArray();
    }

    private static Element readElement(InputStream in) throws IOException {
        int tag = in.read();
        if (tag < 0) {
            return null;
        }
        int length = in.read();
        if (length < 0) {
            throw new IOException("Unexpected end of stream");
        }
        if ((length & 0x80) != 0) {
            int lengthBytes = length & 0x7f;
            length = 0;
            for (int i = 0; i < lengthBytes; i++) {
                length = (length << 8) | in.read();
            }
        }
        byte[] content = in.readNBytes(length);
        if (content.length != length) {
            throw new IOException("Unexpected end of stream");
        }
        return new Element(tag, content);
    }

    private record Element(int tag, byte[] content) {
    }
}
