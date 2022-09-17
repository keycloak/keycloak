/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import org.apache.http.client.methods.HttpGet;
import org.keycloak.common.crypto.CryptoConstants;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.utils.OCSPProvider;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.Time;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.util.XMLSignatureUtil;
import org.keycloak.services.ServicesLogger;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.utils.CRLUtils;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.CRLException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.cert.X509CertSelector;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static org.keycloak.authentication.authenticators.x509.AbstractX509ClientCertificateAuthenticator.CERTIFICATE_POLICY_MODE_ANY;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/30/2016
 */

public class CertificateValidator {

    private static final ServicesLogger logger = ServicesLogger.LOGGER;

    enum KeyUsageBits {
        DIGITAL_SIGNATURE(0, "digitalSignature"),
        NON_REPUDIATION(1, "nonRepudiation"),
        KEY_ENCIPHERMENT(2, "keyEncipherment"),
        DATA_ENCIPHERMENT(3, "dataEncipherment"),
        KEY_AGREEMENT(4, "keyAgreement"),
        KEYCERTSIGN(5, "keyCertSign"),
        CRLSIGN(6, "cRLSign"),
        ENCIPHERMENT_ONLY(7, "encipherOnly"),
        DECIPHER_ONLY(8, "decipherOnly");

        private int value;
        private String name;

        KeyUsageBits(int value, String name) {

            if (value < 0 || value > 8)
                throw new IllegalArgumentException("value");
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");
            this.value = value;
            this.name = name.trim();
        }

        public int getInt() { return this.value; }
        public String getName() {  return this.name; }

        static public KeyUsageBits parse(String name) throws IllegalArgumentException, IndexOutOfBoundsException {
            if (name == null || name.trim().length() == 0)
                throw new IllegalArgumentException("name");

            for (KeyUsageBits bit : KeyUsageBits.values()) {
                if (bit.getName().equalsIgnoreCase(name))
                    return bit;
            }
            throw new IndexOutOfBoundsException("name");
        }

        static public KeyUsageBits fromValue(int value) throws IndexOutOfBoundsException {
            if (value < 0 || value > 8)
                throw new IndexOutOfBoundsException("value");
            for (KeyUsageBits bit : KeyUsageBits.values())
                if (bit.getInt() == value)
                    return bit;
            throw new IndexOutOfBoundsException("value");
        }
    }

    public static class LdapContext {
        private final String ldapFactoryClassName;

        public LdapContext() {
            ldapFactoryClassName = "com.sun.jndi.ldap.LdapCtxFactory";
        }

        public LdapContext(String ldapFactoryClassName) {
            this.ldapFactoryClassName = ldapFactoryClassName;
        }

        public String getLdapFactoryClassName() {
            return ldapFactoryClassName;
        }
    }

    public abstract static class OCSPChecker {
        /**
         * Requests certificate revocation status using OCSP. The OCSP responder URI
         * is obtained from the certificate's AIA extension.
         * @param cert the certificate to be checked
         * @param issuerCertificate The issuer certificate
         * @return revocation status
         */
        public abstract OCSPProvider.OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate) throws CertPathValidatorException;
    }

    public abstract static class CRLLoaderImpl {
        /**
         * Returns a collection of {@link X509CRL}
         * @return
         * @throws GeneralSecurityException
         */
        public abstract Collection<X509CRL> getX509CRLs() throws GeneralSecurityException;
    }

    public static class BouncyCastleOCSPChecker extends OCSPChecker {

        private final KeycloakSession session;
        private final String responderUri;
        private final X509Certificate responderCert;

        BouncyCastleOCSPChecker(KeycloakSession session, String responderUri, X509Certificate responderCert) {
            this.session = session;
            this.responderUri = responderUri;
            this.responderCert = responderCert;
        }

        @Override
        public OCSPProvider.OCSPRevocationStatus check(X509Certificate cert, X509Certificate issuerCertificate) throws CertPathValidatorException {

            OCSPProvider.OCSPRevocationStatus ocspRevocationStatus = null;
            if (responderUri == null || responderUri.trim().length() == 0) {
                // Obtains revocation status of a certificate using OCSP and assuming
                // most common defaults. If responderUri is not specified,
                // then OCS responder URI is retrieved from the
                // certificate's AIA extension.
                // OCSP responses must be signed with the issuer certificate
                // or with another certificate that must be:
                // 1) signed by the issuer certificate,
                // 2) Includes the value of OCSPsigning in ExtendedKeyUsage v3 extension
                // 3) Certificate is valid at the time
                OCSPProvider ocspProvider = CryptoIntegration.getProvider().getOCSPProver(OCSPProvider.class);
                ocspRevocationStatus = ocspProvider.check(session, cert, issuerCertificate);
            }
            else {
                URI uri;
                try {
                    uri = new URI(responderUri);
                } catch (URISyntaxException e) {
                    String message = String.format("Unable to check certificate revocation status using OCSP.\n%s", e.getMessage());
                    throw new CertPathValidatorException(message, e);
                }
                logger.tracef("Responder URI \"%s\" will be used to verify revocation status of the certificate using OCSP with responderCert=%s",
                        uri.toString(), responderCert);
                // Obtains the revocation status of a certificate using OCSP.
                // OCSP responder's certificate is assumed to be the issuer's certificate
                // certificate.
                // responderUri overrides the contents (if any) of the certificate's AIA extension
                OCSPProvider ocspProvider = CryptoIntegration.getProvider().getOCSPProver(OCSPProvider.class);
                ocspRevocationStatus = ocspProvider.check(session, cert, issuerCertificate, uri, responderCert, null);
            }
            return ocspRevocationStatus;
        }
    }

    public static class CRLLoaderProxy extends CRLLoaderImpl {
        private final X509CRL _crl;
        public CRLLoaderProxy(X509CRL crl) {
            _crl = crl;
        }
        public Collection<X509CRL> getX509CRLs() throws GeneralSecurityException {
            return Collections.singleton(_crl);
        }
    }

    // Delegate to list of other CRLLoaders
    public static class CRLListLoader extends CRLLoaderImpl {

        private final List<CRLLoaderImpl> delegates;

        public CRLListLoader(KeycloakSession session, String cRLConfigValue) {
            String[] delegatePaths = Constants.CFG_DELIMITER_PATTERN.split(cRLConfigValue);
            this.delegates = Arrays.stream(delegatePaths)
                    .map(cRLPath -> new CRLFileLoader(session, cRLPath))
                    .collect(Collectors.toList());
        }


        @Override
        public Collection<X509CRL> getX509CRLs() throws GeneralSecurityException {
            Collection<X509CRL> result = new LinkedList<>();
            for (CRLLoaderImpl delegate : delegates) {
                result.addAll(delegate.getX509CRLs());
            }
            return result;
        }
    }

    public static class CRLFileLoader extends CRLLoaderImpl {

        private final KeycloakSession session;
        private final String cRLPath;
        private final LdapContext ldapContext;

        public CRLFileLoader(KeycloakSession session, String cRLPath) {
            this.session = session;
            this.cRLPath = cRLPath;
            ldapContext = new LdapContext();
        }

        public CRLFileLoader(KeycloakSession session, String cRLPath, LdapContext ldapContext) {
            this.session = session;
            this.cRLPath = cRLPath;
            this.ldapContext = ldapContext;

            if (ldapContext == null)
                throw new NullPointerException("Context cannot be null");
        }

        public Collection<X509CRL> getX509CRLs() throws GeneralSecurityException {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<X509CRL> crlColl = null;

            if (cRLPath != null) {
                if (cRLPath.startsWith("http") || cRLPath.startsWith("https")) {
                    // load CRL using remote URI
                    try {
                        crlColl = loadFromURI(cf, new URI(cRLPath));
                    } catch (URISyntaxException e) {
                        logger.error(e.getMessage());
                    }
                } else if (cRLPath.startsWith("ldap")) {
                    // load CRL from LDAP
                    try {
                        crlColl = loadCRLFromLDAP(cf, new URI(cRLPath));
                    } catch(URISyntaxException e) {
                        logger.error(e.getMessage());
                    }
                } else {
                    // load CRL from file
                    crlColl = loadCRLFromFile(cf, cRLPath);
                }
            }
            if (crlColl == null || crlColl.size() == 0) {
                String message = String.format("Unable to load CRL from \"%s\"", cRLPath);
                throw new GeneralSecurityException(message);
            }
            return crlColl;
        }

        private Collection<X509CRL> loadFromURI(CertificateFactory cf, URI remoteURI) throws GeneralSecurityException {
            try {
                logger.debugf("Loading CRL from %s", remoteURI.toString());

                CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
                HttpGet get = new HttpGet(remoteURI);
                get.setHeader("Pragma", "no-cache");
                get.setHeader("Cache-Control", "no-cache, no-store");
                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    try {
                        InputStream content = response.getEntity().getContent();
                        X509CRL crl = loadFromStream(cf, content);
                        return Collections.singleton(crl);
                    } finally {
                        EntityUtils.consumeQuietly(response.getEntity());
                    }
                }
            }
            catch(IOException ex) {
                logger.errorf(ex.getMessage());
            }
            return Collections.emptyList();

        }

        private Collection<X509CRL> loadCRLFromLDAP(CertificateFactory cf, URI remoteURI) throws GeneralSecurityException {
            Hashtable<String, String> env = new Hashtable<>(2);
            env.put(Context.INITIAL_CONTEXT_FACTORY, ldapContext.getLdapFactoryClassName());
            env.put(Context.PROVIDER_URL, remoteURI.toString());

            try {
                DirContext ctx = new InitialDirContext(env);
                try {
                    Attributes attrs = ctx.getAttributes("");
                    Attribute cRLAttribute = attrs.get("certificateRevocationList;binary");
                    byte[] data = (byte[])cRLAttribute.get();
                    if (data == null || data.length == 0) {
                        throw new CertificateException(String.format("Failed to download CRL from \"%s\"", remoteURI.toString()));
                    }
                    X509CRL crl = loadFromStream(cf, new ByteArrayInputStream(data));
                    return Collections.singleton(crl);
                } finally {
                    ctx.close();
                }
            } catch (NamingException e) {
                logger.error(e.getMessage());
            } catch(IOException e) {
                logger.error(e.getMessage());
            }

            return Collections.emptyList();
        }

        private Collection<X509CRL> loadCRLFromFile(CertificateFactory cf, String relativePath) throws GeneralSecurityException {
            try {
                String configDir = System.getProperty("jboss.server.config.dir");
                if (configDir != null) {
                    File f = new File(configDir + File.separator + relativePath);
                    if (f.isFile()) {
                        logger.debugf("Loading CRL from %s", f.getAbsolutePath());

                        if (!f.canRead()) {
                            throw new IOException(String.format("Unable to read CRL from \"%s\"", f.getAbsolutePath()));
                        }
                        try (FileInputStream is = new FileInputStream(f.getAbsolutePath())) {
                            X509CRL crl = loadFromStream(cf, is);
                            return Collections.singleton(crl);
                        }
                    }
                }
            }
            catch(IOException ex) {
                logger.errorf(ex.getMessage());
            }
            return Collections.emptyList();
        }
        private X509CRL loadFromStream(CertificateFactory cf, InputStream is) throws IOException, CRLException {
            DataInputStream dis = new DataInputStream(is);
            X509CRL crl = (X509CRL)cf.generateCRL(dis);
            dis.close();
            return crl;
        }
    }

    KeycloakSession session;
    X509Certificate[] _certChain;
    int _keyUsageBits;
    List<String> _extendedKeyUsage;
    List<String> _certificatePolicy;
    String _certificatePolicyMode;
    boolean _crlCheckingEnabled;
    boolean _crldpEnabled;
    CRLLoaderImpl _crlLoader;
    boolean _ocspEnabled;
    boolean _ocspFailOpen;
    OCSPChecker ocspChecker;
    boolean _timestampValidationEnabled;
    boolean _trustValidationEnabled;

    public CertificateValidator() {

    }
    protected CertificateValidator(X509Certificate[] certChain,
                         int keyUsageBits, List<String> extendedKeyUsage,
                                   List<String> certificatePolicy, String certificatePolicyMode,
                                   boolean cRLCheckingEnabled,
                                   boolean cRLDPCheckingEnabled,
                                   CRLLoaderImpl crlLoader,
                                   boolean oCSPCheckingEnabled,
                                   boolean ocspFailOpen,
                                   OCSPChecker ocspChecker,
                                   KeycloakSession session,
                                   boolean timestampValidationEnabled,
                                   boolean trustValidationEnabled) {
        _certChain = certChain;
        _keyUsageBits = keyUsageBits;
        _extendedKeyUsage = extendedKeyUsage;
        _certificatePolicy = certificatePolicy;
        _certificatePolicyMode = certificatePolicyMode;
        _crlCheckingEnabled = cRLCheckingEnabled;
        _crldpEnabled = cRLDPCheckingEnabled;
        _crlLoader = crlLoader;
        _ocspEnabled = oCSPCheckingEnabled;
        _ocspFailOpen = ocspFailOpen;
        this.ocspChecker = ocspChecker;
        this.session = session;
        _timestampValidationEnabled = timestampValidationEnabled;
        _trustValidationEnabled = trustValidationEnabled;

        if (ocspChecker == null)
            throw new IllegalArgumentException("ocspChecker");
    }

    private static void validateKeyUsage(X509Certificate[] certs, int expected) throws GeneralSecurityException {
        boolean[] keyUsageBits = certs[0].getKeyUsage();
        if (keyUsageBits == null) {
            if (expected != 0) {
                String message = "Key usage extension is expected, but unavailable.";
                throw new GeneralSecurityException(message);
            }
            return;
        }

        boolean isCritical = false;
        Set<String> critSet = certs[0].getCriticalExtensionOIDs();
        if (critSet != null) {
            isCritical = critSet.contains("2.5.29.15");
        }

        int n = expected;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyUsageBits.length; i++, n >>= 1) {
            boolean value = keyUsageBits[i];
            if ((n & 1) == 1 && !value) {
                String message = String.format("Key Usage bit \'%s\' is not set.", CertificateValidator.KeyUsageBits.fromValue(i).getName());
                if (sb.length() > 0) sb.append("\n");
                sb.append(message);

                logger.warn(message);
            }
        }
        if (sb.length() > 0) {
            if (isCritical) {
                throw new GeneralSecurityException(sb.toString());
            }
        }
    }

    private static void validateExtendedKeyUsage(X509Certificate[] certs, List<String> expectedEKU) throws GeneralSecurityException {
        if (expectedEKU == null || expectedEKU.size() == 0) {
            logger.debug("Extended Key Usage validation is not enabled.");
            return;
        }
        List<String> extendedKeyUsage = certs[0].getExtendedKeyUsage();
        if (extendedKeyUsage == null) {
            String message = "Extended key usage extension is expected, but unavailable";
            throw new GeneralSecurityException(message);
        }

        boolean isCritical = false;
        Set<String> critSet = certs[0].getCriticalExtensionOIDs();
        if (critSet != null) {
            isCritical = critSet.contains("2.5.29.37");
        }

        List<String> ekuList = new LinkedList<>();
        extendedKeyUsage.forEach(s -> ekuList.add(s.toLowerCase()));

        for (String eku : expectedEKU) {
            if (!ekuList.contains(eku.toLowerCase())) {
                String message = String.format("Extended Key Usage \'%s\' is missing.", eku);
                if (isCritical) {
                    throw new GeneralSecurityException(message);
                }
                logger.warn(message);
            }
        }
    }


    private static void validatePolicy(X509Certificate[] certs, List<String> expectedPolicies, String policyCheckMode) throws GeneralSecurityException {
        if (expectedPolicies == null || expectedPolicies.size() == 0) {
            logger.debug("Certificate Policy validation is not enabled.");
            return;
        }

        List<String> policyList = CryptoIntegration.getProvider().getCertificateUtils().getCertificatePolicyList(certs[0]);

        logger.debugf("Certificate policies found: %s", String.join(",", policyList));

        if (policyCheckMode == CERTIFICATE_POLICY_MODE_ANY)
        {
            boolean hasMatch = expectedPolicies.stream().anyMatch(p -> policyList.contains(p.toLowerCase()));
            if (!hasMatch) {
                String message = String.format("Certificate Policy check failed: mode = ANY, found = \'%s\', expected = \'%s\'.",
                    String.join(",", policyList), String.join(",", expectedPolicies));
                throw new GeneralSecurityException(message);
            }
        }
        else
        {
            for (String policy : expectedPolicies) {
                if (!policyList.contains(policy.toLowerCase())) {
                    String message = String.format("Certificate Policy check failed: mode = ALL, certificate policy \'%s\' is missing.", policy);
                    throw new GeneralSecurityException(message);
                }
            }
        }
    }

    public CertificateValidator validateKeyUsage() throws GeneralSecurityException {
        validateKeyUsage(_certChain, _keyUsageBits);
        return this;
    }

    public CertificateValidator validateExtendedKeyUsage() throws GeneralSecurityException {
        validateExtendedKeyUsage(_certChain, _extendedKeyUsage);
        return this;
    }

    public CertificateValidator validatePolicy() throws GeneralSecurityException {
        validatePolicy(_certChain, _certificatePolicy, _certificatePolicyMode);
        return this;
    }

    public CertificateValidator validateTimestamps() throws GeneralSecurityException {
        if (!_timestampValidationEnabled)
            return this;

        for (int i = 0; i < _certChain.length; i++)
        {
            X509Certificate x509Certificate = _certChain[i];
            if (x509Certificate.getNotBefore().getTime() > Time.currentTimeMillis()) {
                String serialNumber = x509Certificate.getSerialNumber().toString(16).replaceAll("..(?!$)",
                  "$0 ");
                String message =
                  "certificate with serialnumber '" + serialNumber
                    + "' is not valid yet: " + x509Certificate.getNotBefore().toString();
                throw new GeneralSecurityException(message);
            }
            if (x509Certificate.getNotAfter().getTime() < Time.currentTimeMillis()) {
                String serialNumber = x509Certificate.getSerialNumber().toString(16).replaceAll("..(?!$)",
                  "$0 ");
                String message = "certificate with serialnumber '" + serialNumber
                                   + "' has expired on: " + x509Certificate.getNotAfter().toString();
                throw new GeneralSecurityException(message);
            }
        }

        return this;
    }

    public CertificateValidator validateTrust() throws GeneralSecurityException {
        if (!_trustValidationEnabled)
            return this;

        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            logger.error("Cannot validate client certificate trust: Truststore not available");
        }
        else
        {
            Set<X509Certificate> trustedRootCerts = truststoreProvider.getRootCertificates().entrySet().stream().map(t -> t.getValue()).collect(Collectors.toSet());
            Set<X509Certificate> trustedIntermediateCerts = truststoreProvider.getIntermediateCertificates().entrySet().stream().map(t -> t.getValue()).collect(Collectors.toSet());

            logger.debugf("Found %d trusted root certs, %d trusted intermediate certs", trustedRootCerts.size(), trustedIntermediateCerts.size());

            verifyCertificateTrust(_certChain, trustedRootCerts, trustedIntermediateCerts);
        }

        return this;
    }

    /**
    * Attempts to build a certification chain for given certificate and to verify
    * it. Relies on a set of root CA certificates (trust anchors) and a set of
    * intermediate certificates (to be used as part of the chain).
    * @param certChain - client chain presented for validation. cert to validate is assumed to be the first in the chain
    * @param trustedRootCerts - set of trusted root CA certificates
    * @param trustedIntermediateCerts - set of intermediate certificates
    * @return the certification chain (if verification is successful)
    * @throws GeneralSecurityException - if the verification is not successful
    *       (e.g. certification path cannot be built or some certificate in the
    *       chain is expired)
    */
    private static PKIXCertPathBuilderResult verifyCertificateTrust(X509Certificate[] certChain, Set<X509Certificate> trustedRootCerts,
        Set<X509Certificate> trustedIntermediateCerts) throws GeneralSecurityException {

        // Create the selector that specifies the starting certificate
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(certChain[0]);

        // Create the trust anchors (set of root CA certificates)
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate trustedRootCert : trustedRootCerts) {
            trustAnchors.add(new TrustAnchor(trustedRootCert, null));
        }

        // Configure the PKIX certificate builder algorithm parameters
        PKIXBuilderParameters pkixParams =
            new PKIXBuilderParameters(trustAnchors, selector);

        // Disable CRL checks (this is done manually as additional step)
        pkixParams.setRevocationEnabled(false);

        // Specify a list of intermediate certificates
        Set<X509Certificate> intermediateCerts = new HashSet<X509Certificate>();
        for (X509Certificate intermediateCert : trustedIntermediateCerts) {
            intermediateCerts.add(intermediateCert);
        }
        // Client certificates have to be added to the list of intermediate certs
        for (X509Certificate clientCert : certChain) {
            intermediateCerts.add(clientCert);
        }
        CertStore intermediateCertStore = CertStore.getInstance("Collection",
            new CollectionCertStoreParameters(intermediateCerts), BouncyIntegration.PROVIDER);
        pkixParams.addCertStore(intermediateCertStore);

        // Build and verify the certification chain
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", BouncyIntegration.PROVIDER);
        PKIXCertPathBuilderResult result =
            (PKIXCertPathBuilderResult) builder.build(pkixParams);
        return result;
    }

    private X509Certificate findCAInTruststore(X500Principal issuer) throws GeneralSecurityException {
        TruststoreProvider truststoreProvider = session.getProvider(TruststoreProvider.class);
        if (truststoreProvider == null || truststoreProvider.getTruststore() == null) {
            return null;
        }
        Map<X500Principal, X509Certificate> rootCerts = truststoreProvider.getRootCertificates();
        X509Certificate ca = rootCerts.get(issuer);
        if (ca == null) {
            // fallback to lookup the issuer from the list of intermediary CAs
            ca = truststoreProvider.getIntermediateCertificates().get(issuer);
        }
        if (ca != null) {
            ca.checkValidity();
        }
        return ca;
    }

    private void checkRevocationUsingOCSP(X509Certificate[] certs) throws GeneralSecurityException {

        if (logger.isDebugEnabled() && certs != null) {
            for (X509Certificate cert : certs) {
                logger.debugf("Certificate: %s", cert.getSubjectDN().getName());
            }
        }

        X509Certificate cert = null;
        X509Certificate issuer = null;

        if (certs == null || certs.length == 0) {
             throw new GeneralSecurityException("No certificates sent");
        } else if (certs.length > 1) {
            cert = certs[0];
            issuer = certs[1];
        } else {
            // only one cert => find the CA certificate using the truststore SPI
            cert = certs[0];
            issuer = findCAInTruststore(cert.getIssuerX500Principal());
            if (issuer == null) {
                throw new GeneralSecurityException(
                        String.format("No trusted CA in certificate found: %s. Add it to truststore SPI if valid.",
                                cert.getIssuerX500Principal()));
            }
        }

        try {
            OCSPProvider.OCSPRevocationStatus rs = ocspChecker.check(cert, issuer);

            if (rs == null) {
                if (_ocspFailOpen)
                    logger.warnf("Unable to check client revocation status using OCSP - continuing certificate authentication because of fail-open OCSP configuration setting");
                else
                    throw new GeneralSecurityException("Unable to check client revocation status using OCSP");
            }

            if (rs.getRevocationStatus() == OCSPProvider.RevocationStatus.UNKNOWN) {
                if (_ocspFailOpen)
                    logger.warnf("Unable to determine certificate's revocation status - continuing certificate authentication because of fail-open OCSP configuration setting");
                else
                    throw new GeneralSecurityException("Unable to determine certificate's revocation status.");
            }
            else if (rs.getRevocationStatus() == OCSPProvider.RevocationStatus.REVOKED) {

                StringBuilder sb = new StringBuilder();
                sb.append("Certificate's been revoked.");
                sb.append("\n");
                sb.append(rs.getRevocationReason().toString());
                sb.append("\n");
                sb.append(String.format("Revoked on: %s",rs.getRevocationTime().toString()));

                throw new GeneralSecurityException(sb.toString());
            }
        } catch (CertPathValidatorException e) {
            if (_ocspFailOpen)
                logger.warnf("Unable to check client revocation status using OCSP - continuing certificate authentication because of fail-open OCSP configuration setting");
            else
                throw e;
        }
    }

    private static void checkRevocationStatusUsingCRL(X509Certificate[] certs, CRLLoaderImpl crLoader, KeycloakSession session) throws GeneralSecurityException {
        Collection<X509CRL> crlColl = crLoader.getX509CRLs();
        if (crlColl != null && crlColl.size() > 0) {
            for (X509CRL it : crlColl) {
                CRLUtils.check(certs, it, session);
            }
        }
    }

    private static List<String> getCRLDistributionPoints(X509Certificate cert) {
        try {
            return CryptoIntegration.getProvider().getCertificateUtils().getCRLDistributionPoints(cert);
        }
        catch(IOException e) {
            logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    private static void checkRevocationStatusUsingCRLDistributionPoints(X509Certificate[] certs, KeycloakSession session) throws GeneralSecurityException {

        List<String> distributionPoints = getCRLDistributionPoints(certs[0]);
        if (distributionPoints == null || distributionPoints.size() == 0) {
            throw new GeneralSecurityException("Could not find any CRL distribution points in the certificate, unable to check the certificate revocation status using CRL/DP.");
        }
        for (String dp : distributionPoints) {
            logger.tracef("CRL Distribution point: \"%s\"", dp);
            checkRevocationStatusUsingCRL(certs, new CRLFileLoader(session, dp), session);
        }
    }

    public CertificateValidator checkRevocationStatus() throws GeneralSecurityException {
        if (!(_crlCheckingEnabled || _ocspEnabled)) {
            return this;
        }
        if (_crlCheckingEnabled) {
            if (!_crldpEnabled) {
                checkRevocationStatusUsingCRL(_certChain, _crlLoader, session);
            } else {
                checkRevocationStatusUsingCRLDistributionPoints(_certChain, session);
            }
        }
        if (_ocspEnabled) {
            checkRevocationUsingOCSP(_certChain);
        }
        return this;
    }

    /**
     * Configure Certificate validation
     */
    public static class CertificateValidatorBuilder {
        // A hand written DSL that walks through successive steps to configure
        // instances of CertificateValidator type. The design is an adaption of
        // the approach described in http://programmers.stackexchange.com/questions/252067/learning-to-write-dsls-utilities-for-unit-tests-and-am-worried-about-extensablit

        KeycloakSession session;
        int _keyUsageBits;
        List<String> _extendedKeyUsage;
        List<String> _certificatePolicy;
        String _certificatePolicyMode;
        boolean _crlCheckingEnabled;
        boolean _crldpEnabled;
        CRLLoaderImpl _crlLoader;
        boolean _ocspEnabled;
        boolean _ocspFailOpen;
        String _responderUri;
        X509Certificate _responderCert;
        boolean _timestampValidationEnabled;
        boolean _trustValidationEnabled;

        public CertificateValidatorBuilder() {
            _extendedKeyUsage = new LinkedList<>();
            _certificatePolicy = new LinkedList<>();
            _keyUsageBits = 0;
        }

        public class KeyUsageValidationBuilder {

            CertificateValidatorBuilder _parent;
            KeyUsageValidationBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public KeyUsageValidationBuilder enableDigitalSignatureBit() {
                _keyUsageBits |= 1 << KeyUsageBits.DIGITAL_SIGNATURE.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enablecRLSignBit() {
                _keyUsageBits |= 1 << KeyUsageBits.CRLSIGN.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableDataEncriphermentBit() {
                _keyUsageBits |= 1 << KeyUsageBits.DATA_ENCIPHERMENT.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableDecipherOnlyBit() {
                _keyUsageBits |= 1 << KeyUsageBits.DECIPHER_ONLY.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableEnciphermentOnlyBit() {
                _keyUsageBits |= 1 << KeyUsageBits.ENCIPHERMENT_ONLY.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableKeyAgreementBit() {
                _keyUsageBits |= 1 << KeyUsageBits.KEY_AGREEMENT.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableKeyEnciphermentBit() {
                _keyUsageBits |= 1 << KeyUsageBits.KEY_ENCIPHERMENT.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableKeyCertSign() {
                _keyUsageBits |= 1 << KeyUsageBits.KEYCERTSIGN.getInt();
                return this;
            }
            public KeyUsageValidationBuilder enableNonRepudiationBit() {
                _keyUsageBits |= 1 << KeyUsageBits.NON_REPUDIATION.getInt();
                return this;
            }

            public CertificateValidatorBuilder back() {
                return _parent;
            }

            CertificateValidatorBuilder parse(String keyUsage) {
                if (keyUsage == null || keyUsage.trim().length() == 0)
                    return _parent;

                String[] strs = keyUsage.split("[,]");

                for (String s : strs) {
                    try {
                        KeyUsageBits bit = KeyUsageBits.parse(s.trim());
                        switch(bit) {
                            case CRLSIGN: enablecRLSignBit(); break;
                            case DATA_ENCIPHERMENT: enableDataEncriphermentBit(); break;
                            case DECIPHER_ONLY: enableDecipherOnlyBit(); break;
                            case DIGITAL_SIGNATURE: enableDigitalSignatureBit(); break;
                            case ENCIPHERMENT_ONLY: enableEnciphermentOnlyBit(); break;
                            case KEY_AGREEMENT: enableKeyAgreementBit(); break;
                            case KEY_ENCIPHERMENT: enableKeyEnciphermentBit(); break;
                            case KEYCERTSIGN: enableKeyCertSign(); break;
                            case NON_REPUDIATION: enableNonRepudiationBit(); break;
                        }
                    }
                    catch(IllegalArgumentException e) {
                        logger.warnf("Unable to parse key usage bit: \"%s\"", s);
                    }
                    catch(IndexOutOfBoundsException e) {
                        logger.warnf("Invalid key usage bit: \"%s\"", s);
                    }
                }

                return _parent;
            }
        }

        public class ExtendedKeyUsageValidationBuilder {

            CertificateValidatorBuilder _parent;
            protected ExtendedKeyUsageValidationBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public CertificateValidatorBuilder parse(String extendedKeyUsage) {
                if (extendedKeyUsage == null || extendedKeyUsage.trim().length() == 0)
                    return _parent;

                String[] strs = extendedKeyUsage.split("[,;:]");
                for (String str : strs) {
                    _extendedKeyUsage.add(str.trim());
                }
                return _parent;
            }
        }

        public class CertificatePolicyValidationBuilder {

            CertificateValidatorBuilder _parent;
            protected CertificatePolicyValidationBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public CertificatePolicyValidationBuilder mode(String mode) {
                _certificatePolicyMode = mode;
                return this;
            }

            public CertificateValidatorBuilder parse(String certificatePolicy) {
                if (certificatePolicy == null || certificatePolicy.trim().length() == 0)
                    return _parent;

                String[] strs = certificatePolicy.split("[,;:]");
                for (String str : strs) {
                    _certificatePolicy.add(str.trim());
                }
                return _parent;
            }
        }

        public class RevocationStatusCheckBuilder {

            CertificateValidatorBuilder _parent;
            protected RevocationStatusCheckBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public GotCRL cRLEnabled(boolean value) {
                _crlCheckingEnabled = value;
                return new GotCRL();
            }

            public class GotCRL {
                public GotCRLDP cRLDPEnabled(boolean value) {
                    _crldpEnabled = value;
                    return new GotCRLDP();
                }
            }

            public class GotCRLRelativePath {
                public GotOCSP oCSPEnabled(boolean value) {
                    _ocspEnabled = value;
                    return new GotOCSP();
                }
            }
            public class GotCRLDP {
                public GotCRLRelativePath cRLrelativePath(String value) {
                    if (value != null)
                        _crlLoader = new CRLListLoader(session, value);
                    return new GotCRLRelativePath();
                }

                public GotCRLRelativePath cRLLoader(CRLLoaderImpl cRLLoader) {
                    if (cRLLoader != null)
                        _crlLoader = cRLLoader;
                    return new GotCRLRelativePath();
                }
            }

            public class GotOCSP {
                public GotOCSPFailOpen oCSPFailOpen(boolean ocspFailOpen) {
                    _ocspFailOpen = ocspFailOpen;
                    return new GotOCSPFailOpen();
                }
            }

            public class GotOCSPFailOpen {
                public GotOCSPFailOpen oCSPResponseCertificate(String responderCert) {
                    if (responderCert != null && !responderCert.isEmpty()) {
                        try {
                            _responderCert = XMLSignatureUtil.getX509CertificateFromKeyInfoString(responderCert);
                            _responderCert.checkValidity();
                        } catch(CertificateException e) {
                            logger.warnf("Ignoring invalid certificate: %s", _responderCert);
                            _responderCert = null;
                        } catch (ProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return new GotOCSPFailOpen();
                }

                public CertificateValidatorBuilder oCSPResponderURI(String responderURI) {
                    _responderUri = responderURI;
                    return _parent;
                }
            }
        }

        public class TimestampValidationBuilder {

            CertificateValidatorBuilder _parent;
            protected TimestampValidationBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public CertificateValidatorBuilder enabled(boolean timestampValidationEnabled) {
                _timestampValidationEnabled = timestampValidationEnabled;
                return _parent;
            }
        }

        public class TrustValidationBuilder {

            CertificateValidatorBuilder _parent;
            protected TrustValidationBuilder(CertificateValidatorBuilder parent) {
                _parent = parent;
            }

            public CertificateValidatorBuilder enabled(boolean value) {
                _trustValidationEnabled = value;
                return _parent;
            }
        }

        public CertificateValidatorBuilder session(KeycloakSession session) {
            this.session = session;
            return this;
        }

        public KeyUsageValidationBuilder keyUsage() {
            return new KeyUsageValidationBuilder(this);
        }

        public ExtendedKeyUsageValidationBuilder extendedKeyUsage() {
            return new ExtendedKeyUsageValidationBuilder(this);
        }

        public CertificatePolicyValidationBuilder certificatePolicy() {
            return new CertificatePolicyValidationBuilder(this);
        }

        public RevocationStatusCheckBuilder revocation() {
            return new RevocationStatusCheckBuilder(this);
        }

        public TimestampValidationBuilder timestampValidation() {
            return new TimestampValidationBuilder(this);
        }

        public TrustValidationBuilder trustValidation() {
            return new TrustValidationBuilder(this);
        }

        public CertificateValidator build(X509Certificate[] certs) {
            if (_crlLoader == null) {
                 _crlLoader = new CRLFileLoader(session, "");
            }
            return new CertificateValidator(certs, _keyUsageBits, _extendedKeyUsage,
                    _certificatePolicy, _certificatePolicyMode,
                    _crlCheckingEnabled, _crldpEnabled, _crlLoader, _ocspEnabled, _ocspFailOpen,
                    new BouncyCastleOCSPChecker(session, _responderUri, _responderCert), session, _timestampValidationEnabled, _trustValidationEnabled);
        }
    }


}
