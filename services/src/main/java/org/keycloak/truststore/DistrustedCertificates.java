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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Certificates that are never trusted, regardless of the configured truststores.
 * <p>
 * Revocation is not checked by default for outgoing TLS connections, and the JDK and OS truststores still
 * contain the roots that issued these certificates, so they have to be rejected explicitly.
 */
public final class DistrustedCertificates {

    /**
     * SHA-256 hashes of the SubjectPublicKeyInfo of the eMudhra intermediate CAs whose private keys were
     * compromised in October 2026, see https://emudhra.we-have-a-problem.com/. Matching on the key covers
     * every certificate issued for it, including the cross-signs by Sectigo's AAA Certificate Services.
     */
    private static final Set<String> DISTRUSTED_SPKI_SHA256 = Set.of(
            "EBED6475C2C2D80FCB199F3FE8DD6E6B3CE60B77EB15D4E42C3DEC0C6F5B5105", // EM DV TLS CA - G3B
            "49DD353DC2B919A3770645117D0E60C16115E7D1BC259EEFE734B00EA434FE5F", // EM DV TLS CA - G3C
            "43BFFA19D6EF651451F6841E019931E96F6F2EAEFF28AB4B6F1CE4576DE2717B", // EM OV TLS CA - G3B
            "63ECEFEE431D49523BC1136C4E72D0206A477086CAF606CD5CF025686C109CF8", // EM OV TLS CA - G3C
            "E0254C7FCDC265E2F0F8A428DABB75AB4AE039B4872D62F5EE57F0877D5A8A41", // EM EV TLS CA - G3B
            "5C0660A2B164910D8FF589C18E14552E9E0EC237DCEFE2CB736D41A4811F81A0", // EM EV TLS CA - G3C
            "EC4FFA1AD0AC88081314B7AAFFEF6A91F97CD57291BB5BA686C5415F5EB4F6B6", // EM OV CS CA - G3B
            "92C063D67E8F14DB71D288E11266FFDAF69CBF115F7BAB14F3BC2F68D25E0E30"  // EM OV CS CA - G3C
    );

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
     * @throws CertificateException if any certificate in the chain is distrusted
     */
    public static void checkChain(X509Certificate[] chain) throws CertificateException {
        if (chain == null) {
            return;
        }
        for (X509Certificate certificate : chain) {
            if (isDistrusted(certificate)) {
                throw new CertificateException("Certificate chain contains a distrusted certificate: "
                        + certificate.getSubjectX500Principal().getName());
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
     * Wraps each {@link X509TrustManager} so that it also rejects chains containing a distrusted certificate.
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
            if (isDistrusted(cert)) {
                String name = cert instanceof X509Certificate x509Cert ? x509Cert.getSubjectX500Principal().getName() : cert.getType();
                throw new CertPathValidatorException("Distrusted certificate: " + name);
            }
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
