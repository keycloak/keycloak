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

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.jsse.BCSNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.BCSSLSocket;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Run with Java 21+ and the POC Keycloak distribution's lib/lib/main/* on the classpath. */
public class DualIdentityHttps {
    private static final String HYBRID_GROUP = "X25519MLKEM768";
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static final BouncyCastleProvider CRYPTO = new BouncyCastleProvider();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java -cp '<keycloak>/lib/lib/main/*' DualIdentityHttps.java <keycloak>");
        }
        Path home = Path.of(args[0]).toAbsolutePath();
        if (!Files.isRegularFile(home.resolve("bin/kc.sh"))) {
            throw new IllegalArgumentException("Not a Keycloak distribution: " + home);
        }
        Path work = Files.createTempDirectory("keycloak-dual-identity-");
        System.out.println("Runtime: " + Runtime.version());
        System.out.println("Artifacts and server log: " + work);
        var store = KeyStore.getInstance("PKCS12", CRYPTO);
        store.load(null, null);
        var rsa = identity(store, "rsa", "RSA", "SHA256withRSA");
        var mlDsa = identity(store, "mldsa", "ML-DSA-65", "ML-DSA-65");
        Path file = work.resolve("identities.p12");
        try (var out = Files.newOutputStream(file)) {
            store.store(out, PASSWORD);
        }
        System.out.println("Generated one PKCS12 keystore with TWO private-key entries: rsa, mldsa");
        System.out.println("Both certificates have SAN DNS:localhost; neither connection uses an alias option.");
        int port;
        try (var reservation = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = reservation.getLocalPort();
        }
        Path log = work.resolve("server.log");
        var builder = new ProcessBuilder("bash", home.resolve("bin/kc.sh").toString(), "start-dev",
                "--cache=local", "--http-host=127.0.0.1", "--http-port=0", "--https-port=" + port,
                "--http-management-port=0", "--db=dev-mem",
                "--https-key-store-file=" + file, "--https-key-store-type=PKCS12",
                "--https-key-store-password=changeit", "--https-protocols=TLSv1.3");
        builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
        var server = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            awaitServer(server, log, port);
            System.out.println("One server process, PID=" + server.pid() + ", endpoint=https://localhost:" + port);
            // Fresh contexts prevent session resumption from disguising certificate selection.
            // Both clients trust BOTH identities; only the advertised signature policy changes.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var ready = new CountDownLatch(2);
                var go = new CountDownLatch(1);
                var rsaResult = executor.submit(() -> connect(store, rsa, port, "rsa", ready, go));
                var mlDsaResult = executor.submit(() -> connect(store, mlDsa, port, "mldsa", ready, go));
                if (!ready.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Clients did not become ready");
                }
                go.countDown();
                System.out.println(rsaResult.get(30, TimeUnit.SECONDS));
                System.out.println(mlDsaResult.get(30, TimeUnit.SECONDS));
            }
            if (!server.isAlive()) {
                throw new IllegalStateException("Server exited unexpectedly");
            }
            System.out.println("PASS: both identities served concurrently by the same listener, without restart or reload.");
            System.out.println("PASS: both fresh TLS 1.3 connections required hybrid key exchange; no classical fallback was permitted.");
            System.out.println("This is negotiated authentication, not a composite RSA+ML-DSA signature.");
        } catch (Exception e) {
            System.err.println("Server log: " + log);
            System.err.println(Files.readString(log));
            throw e;
        } finally {
            server.destroy();
            if (!server.waitFor(20, TimeUnit.SECONDS)) {
                server.destroyForcibly();
                server.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    private static X509Certificate identity(KeyStore store, String alias, String algorithm, String signature) throws Exception {
        var generator = KeyPairGenerator.getInstance(algorithm, CRYPTO);
        if (algorithm.equals("RSA")) {
            generator.initialize(3072);
        }
        var keys = generator.generateKeyPair();
        var name = new X500Name("CN=localhost");
        var now = Instant.now();
        var certificate = new JcaX509v3CertificateBuilder(name,
                new BigInteger(160, new java.security.SecureRandom()),
                Date.from(now.minusSeconds(60)), Date.from(now.plusSeconds(86400)), name, keys.getPublic())
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
                .addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature))
                .addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth))
                .addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(new GeneralName(GeneralName.dNSName, "localhost")))
                .build(new JcaContentSignerBuilder(signature).setProvider(CRYPTO).build(keys.getPrivate()));
        var x509 = new JcaX509CertificateConverter().setProvider(CRYPTO).getCertificate(certificate);
        x509.verify(keys.getPublic(), CRYPTO);
        store.setKeyEntry(alias, keys.getPrivate(), PASSWORD, new X509Certificate[] {x509});
        System.out.println("Generated " + alias + ": " + x509.getPublicKey().getAlgorithm() + ", SHA-256=" + fingerprint(x509));
        return x509;
    }

    private static String connect(KeyStore identities, X509Certificate expected, int port, String policy,
            CountDownLatch ready, CountDownLatch go) throws Exception {
        var tls = new BouncyCastleJsseProvider(CRYPTO);
        // Trust certificates only: the clients neither possess nor present a client identity.
        var anchors = KeyStore.getInstance("PKCS12", CRYPTO);
        anchors.load(null, null);
        anchors.setCertificateEntry("rsa", identities.getCertificate("rsa"));
        anchors.setCertificateEntry("mldsa", identities.getCertificate("mldsa"));
        var trust = TrustManagerFactory.getInstance("PKIX", tls);
        trust.init(anchors);
        var context = SSLContext.getInstance("TLSv1.3", tls);
        context.init(null, trust.getTrustManagers(), null);
        ready.countDown();
        if (!go.await(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Client start barrier timed out");
        }
        try (var tcp = new Socket("127.0.0.1", port);
             var socket = (SSLSocket) context.getSocketFactory().createSocket(tcp, "localhost", port, true)) {
            socket.setSoTimeout(15000);
            var bc = (BCSSLSocket) socket;
            var parameters = bc.getParameters();
            parameters.setProtocols(new String[] {"TLSv1.3"});
            // A fresh TLS 1.3 connection with this single group cannot silently
            // fall back to a classical-only key exchange.
            parameters.setNamedGroups(new String[] {HYBRID_GROUP});
            parameters.setServerNames(List.of(new BCSNIHostName("localhost")));
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
            parameters.setApplicationProtocols(new String[] {"http/1.1"});
            parameters.setSignatureSchemes(policy.equals("rsa")
                    ? new String[] {"rsa_pss_rsae_sha256"} : new String[] {"mldsa65"});
            parameters.setSignatureSchemesCert(policy.equals("rsa")
                    ? new String[] {"rsa_pkcs1_sha256"} : new String[] {"mldsa65"});
            bc.setParameters(parameters);
            if (!Arrays.equals(new String[] {HYBRID_GROUP}, bc.getParameters().getNamedGroups())) {
                throw new IllegalStateException("Client did not retain the hybrid-only group policy");
            }
            socket.startHandshake();
            if (!"TLSv1.3".equals(socket.getSession().getProtocol())) {
                throw new IllegalStateException("Expected TLSv1.3");
            }
            var actual = (X509Certificate) socket.getSession().getPeerCertificates()[0];
            if (!Arrays.equals(expected.getEncoded(), actual.getEncoded())) {
                throw new IllegalStateException(policy + " received unexpected certificate: " + fingerprint(actual));
            }
            socket.getOutputStream().write(("GET /realms/master HTTP/1.1\r\nHost: localhost:" + port
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!response.contains("200 OK") || !response.contains("\"realm\":\"master\"")) {
                throw new IllegalStateException("Unexpected HTTP response: " + response);
            }
            return "PASS: " + policy + "-only client -> " + actual.getPublicKey().getAlgorithm()
                    + ", SHA-256=" + fingerprint(actual) + ", TLS=" + socket.getSession().getProtocol()
                    + ", key exchange=" + HYBRID_GROUP + " (only permitted group), HTTP 200";
        }
    }

    private static String fingerprint(X509Certificate certificate) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }

    private static void awaitServer(Process server, Path log, int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (System.nanoTime() < deadline) {
            if (!server.isAlive()) {
                throw new IllegalStateException("Keycloak exited with " + server.exitValue());
            }
            if (Files.readString(log).contains("Listening on:")) {
                try (var socket = new Socket()) {
                    socket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 1000);
                    return;
                } catch (IOException ignored) {
                    // The listener may not yet be accepting connections.
                }
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("Keycloak did not become ready within 90 seconds");
    }
}
