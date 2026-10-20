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

package org.keycloak.truststore;

import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Certificates that are never trusted, regardless of the configured truststores.
 * <p>
 * Revocation is not checked by default for outgoing TLS connections, and the JDK and OS truststores still
 * contain the roots that issued these certificates, so they have to be rejected explicitly.
 */
public final class DistrustedCertificates {

    /**
     * The eMudhra intermediate CAs whose private keys were compromised in October 2026, see
     * https://emudhra.we-have-a-problem.com/, with their SubjectPublicKeyInfo. Matching on the key covers every
     * certificate issued for it, including the cross-signs by Sectigo's AAA Certificate Services and copies with a
     * re-encoded signature.
     */
    private static final List<DistrustedKey> DISTRUSTED_KEYS = List.of(
            new DistrustedKey("CN=EM DV TLS CA - G3B,O=eMudhra BV,C=NL",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE557ZwEqXc8RzXlERHahawr1eZKs3S4DhLLwgOOweTw47gS/JhZ4eZ6+wTi+LyX90hbplqpEe6KTkW2LVzAPVa+Z4s8OMtNyTfKJGDiOEU8iSk10p1E+R4VYGtjzO2rRo"),
            new DistrustedKey("CN=EM DV TLS CA - G3C,O=eMudhra Inc,C=US",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEOnp4YsjETTJN3WkDOmeCwO/u/kBV5ooSP6A3G6O9kZRRTuFypDPjYw8CvRiAN9nlFJ+bd9uUooHfmHsv6oiLdL3+j9pMEJnubKGVZv4+8OX2sJpVbMyLpQVh3xsqRB0B"),
            new DistrustedKey("CN=EM OV TLS CA - G3B,O=eMudhra BV,C=NL",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEc4l6oOs8/cqLEkAv+vCrxOEhK6+N7neKye4tbbg/rCgMXgVjiueirNccMiqONhzeufacQwK53Y1z0cxsqrLMCu5OIWzped1Rikr0tiblaEzdZYA37QyFTkzBL0z4ZG0N"),
            new DistrustedKey("CN=EM OV TLS CA - G3C,O=eMudhra Inc,C=US",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEE4JRzVKskpjByKrZdzId65fnCb0LTKCspV3IrVHc/J0ViaFZfjpRIOHv1jHGPhg1ysrimGmmLhYx6KKBgatWYW3EXZGKvAnzqw5jz3kqpNztc4e+N7ulaVSfHAKycO1n"),
            new DistrustedKey("CN=EM EV TLS CA - G3B,O=eMudhra BV,C=NL",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEJduoWmTTl9oLEFLp6lRuSHvv6pydnWigxerc/u91ppu11vwB9Cv7ym60C81kwHZ+Nv/QXZeoE8WqDGyVMOec9fCGZGjxOAZCCqXIUYQLHiUlQOeQ5LZUw4Uu1krx6GV9"),
            new DistrustedKey("CN=EM EV TLS CA - G3C,O=eMudhra Inc,C=US",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAELVboKab7gsZKRpfZYK+VHpBnrpH5JtNDBtpIqKrreGm2pn5tJ0JuBEKqA7NuGYjf9jZRTuHPB/W6Sz/yoGZFrn0x1YZq0xqj0kbrCzF37pnLdWbFFs50gIpmj5HwxH/K"),
            new DistrustedKey("CN=EM OV CS CA - G3B,O=eMudhra BV,C=NL",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEqrsPsGZqz88B+tUr/P1MK/LLPDSX3nOVDlgZth/mlQmHQcjEkUObNKBDHK/+W/m0wcjorgAB+fTcitgsWfNjnpbNbD1NsM7+wmw9YlF7N3YTcnQ/jordd7EAZPKr5eBQ"),
            new DistrustedKey("CN=EM OV CS CA - G3C,O=eMudhra Inc,C=US",
                    "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEufDgY0cCppymqt1EPP1ixBubpLIJJGl9eXTnaRoSb7lq+z40wv07xfSDBh8mXcKlhDY0cyNRBDayjwTzhiNjBnmA4lA+AlMtZUYjWnFlolmh70lvdBtf3ud4cgjkuPrv")
    );

    private static final Set<String> DISTRUSTED_SPKI_SHA256 = DISTRUSTED_KEYS.stream()
            .map(DistrustedKey::spkiSha256)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * SHA-256 fingerprints of the known certificates for the keys above. These do not depend on how a
     * security provider re-encodes the public key.
     */
    private static final Set<String> DISTRUSTED_CERTIFICATE_SHA256 = Set.of(
            "42C4ACCD12C4F59597925E1A08A7A90102608543809308D50EC427E118BA1493", // EM DV TLS CA - G3B, emSign Root TLS CA - G3
            "45A883F349AEDFD5F3A649B5498FFB121712BA8B448F3F2A91DBC5A766FCDDBE", // EM DV TLS CA - G3B, AAA Certificate Services
            "0164D7CEAE5BB909C272AF151FF44B1177AE9B47D9424B94F1D9558C30934D19", // EM DV TLS CA - G3C, emSign Root TLS CA - G3
            "62B7F9868263F72F6911FC78CEAF72C1979158B7368BF648683BB2AA39B02E72", // EM DV TLS CA - G3C, AAA Certificate Services
            "98B896625C0C230ADEEC3EB2CF6AAD5001ECF9156A68644E82374CE5833BC25B", // EM OV TLS CA - G3B, emSign Root TLS CA - G3
            "5F1DED4DD590AF74631290CF073B122A2AAC6A1FA479778A0BD43579BD832699", // EM OV TLS CA - G3B, AAA Certificate Services
            "CB6E15165AC802E9B3F993F16C25D41F4E81C21EBDE71AE1B012AC00BF9C7C3F", // EM OV TLS CA - G3C, emSign Root TLS CA - G3
            "7485B51C0ED192878FA1D2F948FB529AE1842CC5086C4B67F7BE5B2039DE9489", // EM OV TLS CA - G3C, AAA Certificate Services
            "E7568FE6E12331F4FD7DE61DA4C7559D0F8EB192A218D4741A3BA75184333838", // EM EV TLS CA - G3B, emSign Root TLS CA - G3
            "579480AEA166290BD3C643589E795FC0D0E54E86505D180F1DFF99EFD3199512", // EM EV TLS CA - G3B, AAA Certificate Services
            "432EC25F0D28DE9006D97BF752F17BBB2CD41F669933FCB072D9A1148D0261F3", // EM EV TLS CA - G3C, emSign Root TLS CA - G3
            "FD9D5C2DBBD4995AB69B8D50C439D9527B00529199C2FD01B2174EB58F07021F", // EM EV TLS CA - G3C, AAA Certificate Services
            "9E39DE2881273ACC225A149C72BA3E0F51424963669AB633EE1DC7DF5DB067B4", // EM OV CS CA - G3B, emSign Root CS CA - G3
            "A496E23AC0CAB8155FC9B117B97EAE368EFCBB9041AEC8804D5A308D2D01C2D8", // EM OV CS CA - G3B, AAA Certificate Services
            "4B2460A466F95FD40113BCDCCA2CEB29B3B093921DE45474DA7DF48874C630B1", // EM OV CS CA - G3C, emSign Root CS CA - G3
            "84112972B8E59D14560AE7E7119654474AAD119990CF3722BFC1E3B91615E1FD"  // EM OV CS CA - G3C, AAA Certificate Services
    );

    private DistrustedCertificates() {
    }

    public static boolean isDistrusted(Certificate certificate) {
        if (certificate == null) {
            return false;
        }
        try {
            return DISTRUSTED_CERTIFICATE_SHA256.contains(sha256(certificate.getEncoded()))
                    || DISTRUSTED_SPKI_SHA256.contains(sha256(certificate.getPublicKey().getEncoded()));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Failed to encode certificate", e);
        }
    }

    /**
     * Rejects chains that contain a distrusted certificate, or a certificate issued with a distrusted key. The
     * latter covers chains where the distrusted certificate is not presented, for example because it is a trust
     * anchor, it is fetched from the AIA extension, or the chain could not be rebuilt.
     *
     * @throws CertificateException if the chain is rejected
     */
    public static void checkChain(X509Certificate[] chain) throws CertificateException {
        if (chain == null) {
            return;
        }
        for (X509Certificate certificate : chain) {
            String reason = getDistrustReason(certificate);
            if (reason != null) {
                throw new CertificateException("Certificate chain contains a " + reason);
            }
        }
    }

    /**
     * Creates trust managers for the given truststore that also reject distrusted certificates, both in the
     * presented chain and as trust anchors. The given truststore is not modified.
     *
     * @param truststore the truststore, or {@code null} for the default truststore of the JVM
     */
    public static TrustManager[] getTrustManagers(KeyStore truststore) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        List<X509Certificate> anchors = getAcceptedIssuers(trustManagers);
        if (anchors.stream().anyMatch(DistrustedCertificates::isDistrusted)) {
            // a truststore that was not merged by TruststoreBuilder, such as a legacy or JVM default truststore
            KeyStore filtered = TruststoreBuilder.createGeneratedTrustStore();
            for (X509Certificate anchor : anchors) {
                TruststoreBuilder.setCertificateEntry(filtered, anchor);
            }
            tmf.init(filtered);
            trustManagers = tmf.getTrustManagers();
        }
        return wrap(trustManagers);
    }

    /**
     * Wraps each {@link X509TrustManager} so that it also rejects chains as described in {@link #checkChain}.
     */
    public static TrustManager[] wrap(TrustManager[] trustManagers) {
        if (trustManagers == null) {
            return null;
        }
        TrustManager[] wrapped = new TrustManager[trustManagers.length];
        for (int i = 0; i < trustManagers.length; i++) {
            TrustManager trustManager = trustManagers[i];
            if (trustManager instanceof X509ExtendedTrustManager extendedTrustManager) {
                wrapped[i] = new DistrustingExtendedTrustManager(extendedTrustManager);
            } else if (trustManager instanceof X509TrustManager x509TrustManager) {
                wrapped[i] = new DistrustingTrustManager(x509TrustManager);
            } else {
                wrapped[i] = trustManager;
            }
        }
        return wrapped;
    }

    /**
     * Creates a checker that fails certification path building and validation on a distrusted certificate.
     */
    public static PKIXCertPathChecker certPathChecker() {
        return new DistrustedCertPathChecker();
    }

    private static String getDistrustReason(Certificate certificate) {
        if (isDistrusted(certificate)) {
            return "distrusted certificate: " + getSubjectName(certificate);
        }
        if (certificate instanceof X509Certificate x509Cert && isIssuedByDistrustedKey(x509Cert)) {
            return "certificate issued by a distrusted certificate: " + x509Cert.getIssuerX500Principal().getName();
        }
        return null;
    }

    private static boolean isIssuedByDistrustedKey(X509Certificate certificate) {
        for (DistrustedKey key : DISTRUSTED_KEYS) {
            // paths are built by name, so a certificate can only chain to the key if it names its subject as issuer
            if (key.subject().equals(certificate.getIssuerX500Principal()) && isSignedBy(certificate, key.publicKey())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSignedBy(X509Certificate certificate, PublicKey publicKey) {
        try {
            certificate.verify(publicKey);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static String getSubjectName(Certificate certificate) {
        return certificate instanceof X509Certificate x509Cert ? x509Cert.getSubjectX500Principal().getName() : certificate.getType();
    }

    private static PublicKey decodePublicKey(String spki) {
        try {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(spki)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decode distrusted public key", e);
        }
    }

    private static List<X509Certificate> getAcceptedIssuers(TrustManager[] trustManagers) {
        List<X509Certificate> issuers = new ArrayList<>();
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager x509TrustManager && x509TrustManager.getAcceptedIssuers() != null) {
                issuers.addAll(Arrays.asList(x509TrustManager.getAcceptedIssuers()));
            }
        }
        return issuers;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static final class DistrustedCertPathChecker extends PKIXCertPathChecker {

        @Override
        public void init(boolean forward) {
        }

        @Override
        public boolean isForwardCheckingSupported() {
            return true;
        }

        @Override
        public Set<String> getSupportedExtensions() {
            return null;
        }

        @Override
        public void check(Certificate cert, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
            String reason = getDistrustReason(cert);
            if (reason != null) {
                throw new CertPathValidatorException("Certification path contains a " + reason);
            }
        }
    }

    private record DistrustedKey(X500Principal subject, PublicKey publicKey, String spkiSha256) {

        private DistrustedKey(String subject, String spki) {
            this(new X500Principal(subject), decodePublicKey(spki), sha256(Base64.getDecoder().decode(spki)));
        }
    }

    private static final class DistrustingExtendedTrustManager extends X509ExtendedTrustManager {

        private final X509ExtendedTrustManager delegate;

        private DistrustingExtendedTrustManager(X509ExtendedTrustManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkChain(chain);
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            checkChain(chain);
            delegate.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            checkChain(chain);
            delegate.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkChain(chain);
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            checkChain(chain);
            delegate.checkServerTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            checkChain(chain);
            delegate.checkServerTrusted(chain, authType, engine);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }

    private static final class DistrustingTrustManager implements X509TrustManager {

        private final X509TrustManager delegate;

        private DistrustingTrustManager(X509TrustManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkChain(chain);
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            checkChain(chain);
            delegate.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }
    }
}
