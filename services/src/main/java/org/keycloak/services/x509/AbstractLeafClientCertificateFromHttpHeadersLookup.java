package org.keycloak.services.x509;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.http.HttpRequest;

/**
 * This Provider extracts an end user X.509 certificate sent during TLS mutual
 * authentication and forwarded in an http header. <br>
 * The end user certificate chain is rebuilt with Keycloak truststore.
 */
public abstract class AbstractLeafClientCertificateFromHttpHeadersLookup
        extends AbstractClientCertificateFromHttpHeadersLookup {

    private static final Logger log = Logger.getLogger(AbstractLeafClientCertificateFromHttpHeadersLookup.class);

    private final boolean isTruststoreLoaded;
    private final Set<X509Certificate> trustedRootCerts;
    private final Set<X509Certificate> intermediateCerts;

    public AbstractLeafClientCertificateFromHttpHeadersLookup(String sslCientCertHttpHeader,
            String sslCertChainHttpHeaderPrefix, int certificateChainLength, Set<X509Certificate> intermediateCerts,
            Set<X509Certificate> trustedRootCerts, boolean isTruststoreLoaded) {

        super(sslCientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength);

        Objects.requireNonNull(intermediateCerts, "requireNonNull intermediateCerts");
        Objects.requireNonNull(trustedRootCerts, "requireNonNull trustedRootCerts");

        this.intermediateCerts = intermediateCerts;
        this.trustedRootCerts = trustedRootCerts;
        this.isTruststoreLoaded = isTruststoreLoaded;

        if (!this.isTruststoreLoaded) {
            log.warn("Keycloak Truststore is null or empty, but it's required for the x509cert-lookup provider");
            log.warn(
                    "   see Keycloak documentation here : https://www.keycloak.org/docs/latest/server_installation/index.html#_truststore");
        }

    }

    @Override
    public X509Certificate[] getCertificateChain(HttpRequest httpRequest) throws GeneralSecurityException {
        List<X509Certificate> chain = new ArrayList<>();

        // Get the client certificate
        X509Certificate clientCert = getCertificateFromHttpHeader(httpRequest, sslClientCertHttpHeader);

        if (clientCert != null) {
            log.debugf("End user certificate found : Subject DN=[%s]  SerialNumber=[%s]",
                    clientCert.getSubjectX500Principal(), clientCert.getSerialNumber());

            // Rebuilding the end user certificate chain using Keycloak Truststore
            X509Certificate[] certChain = buildChain(clientCert);
            if (certChain == null || certChain.length == 0) {
                log.info("Impossible to rebuild end user cert chain : client certificate authentication will fail.");
//                chain.add(clientCert);
            } else {
                for (X509Certificate caCert : certChain) {
                    chain.add(caCert);
                    log.debugf("Rebuilded user cert chain DN : %s", caCert.getSubjectX500Principal());
                }
            }
        }
        return chain.toArray(new X509Certificate[0]);
    }

    /**
     * As this proxy does not send the CA Chain in http header(s), we are rebuilding
     * here the end user certificate chain with Keycloak truststore. <br>
     * Please note that Keycloak truststore must contain root and intermediate CA's
     * certificates.
     * 
     * @param endUserAuthCert
     * @return
     */
    private X509Certificate[] buildChain(X509Certificate endUserAuthCert) {

        X509Certificate[] userCertChain = new X509Certificate[0];

        try {

            // No truststore : no way!
            if (!isTruststoreLoaded) {
                log.warn("Keycloak Truststore is null, but it is required !");
                log.warn("  see https://www.keycloak.org/docs/latest/server_installation/index.html#_truststore");
                return userCertChain;
            }

            // Create the selector that specifies the starting certificate
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(endUserAuthCert);

            // Create the trust anchors (set of root CA certificates)
            Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
            for (X509Certificate trustedRootCert : trustedRootCerts) {
                trustAnchors.add(new TrustAnchor(trustedRootCert, null));
            }
            // Configure the PKIX certificate builder algorithm parameters
            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

            // Disable CRL checks, as it's possibly done after depending on Keycloak
            // settings
            pkixParams.setRevocationEnabled(false);
            pkixParams.setExplicitPolicyRequired(false);
            pkixParams.setAnyPolicyInhibited(false);
            pkixParams.setPolicyQualifiersRejected(false);
            pkixParams.setMaxPathLength(certificateChainLength);

            // Adding the list of intermediate certificates + end user certificate
            intermediateCerts.add(endUserAuthCert);
            CollectionCertStoreParameters intermediateCAUserCert = new CollectionCertStoreParameters(intermediateCerts);
            CertStore intermediateCertStore = CryptoIntegration.getProvider().getCertStore(intermediateCAUserCert);
            pkixParams.addCertStore(intermediateCertStore);

            // Build and verify the certification chain (revocation status excluded)
            CertPathBuilder certPathBuilder = CryptoIntegration.getProvider().getCertPathBuilder();
            CertPath certPath = certPathBuilder.build(pkixParams).getCertPath();
            log.debug("Certification path building OK, and contains " + certPath.getCertificates().size()
                    + " X509 Certificates");

            userCertChain = convertCertPathToX509CertArray(certPath);

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            log.error(e.getLocalizedMessage(), e);
        } catch (CertPathBuilderException e) {
            if (log.isEnabled(Level.TRACE)) {
                log.debug(e.getLocalizedMessage(), e);
            } else {
                log.warn(e.getLocalizedMessage());
            }
        } finally {
            if (isTruststoreLoaded) {
                // Remove end user certificate
                intermediateCerts.remove(endUserAuthCert);
            }
        }

        return userCertChain;
    }

    private X509Certificate[] convertCertPathToX509CertArray(CertPath certPath) {

        X509Certificate[] x509certChain = new X509Certificate[0];
        if (certPath == null) {
            return x509certChain;
        }

        List<X509Certificate> trustedX509Chain = new ArrayList<X509Certificate>();
        for (Certificate certificate : certPath.getCertificates()) {
            if (certificate instanceof X509Certificate) {
                trustedX509Chain.add((X509Certificate) certificate);
            }
        }

        return trustedX509Chain.toArray(x509certChain);

    }

}
