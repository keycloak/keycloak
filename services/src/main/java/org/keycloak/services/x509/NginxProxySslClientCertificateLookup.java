package org.keycloak.services.x509;

import java.nio.charset.StandardCharsets;
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

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.PemException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.http.HttpRequest;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * The NGINX Provider extract end user X.509 certificate send during TLS mutual authentication,
 * and forwarded in an http header.
 *
 * NGINX configuration must have :
 * <code>
 * server {
 *    ...
 *    ssl_client_certificate                  path-to-my-trustyed-cas-for-client-auth.pem;
 *    ssl_verify_client                       on|optional_no_ca;
 *    ssl_verify_depth                        2;
 *    ...
 *    location / {
 *    ...
 *      proxy_set_header ssl-client-cert        $ssl_client_escaped_cert;
 *    ...
 *  }
 * </code>
 *
 * Note that $ssl_client_cert is deprecated, use only $ssl_client_escaped_cert with this implementation
 *
 * @author <a href="mailto:arnault.michel@toad-consulting.com">Arnault MICHEL</a>
 * @version $Revision: 1 $
 * @since 10/09/2018
 */

public class NginxProxySslClientCertificateLookup extends AbstractClientCertificateFromHttpHeadersLookup {

    private static final Logger log = Logger.getLogger(NginxProxySslClientCertificateLookup.class);

    private final boolean isTruststoreLoaded;
    private final boolean certIsUrlEncoded;
    private final Set<X509Certificate> trustedRootCerts;
    private final Set<X509Certificate> intermediateCerts;


    public NginxProxySslClientCertificateLookup(String sslClientCertHttpHeader,
                                                String sslCertChainHttpHeaderPrefix,
                                                int certificateChainLength,
                                                Set<X509Certificate> intermediateCerts,
                                                Set<X509Certificate> trustedRootCerts,
                                                boolean isTruststoreLoaded,
                                                boolean certIsUrlEncoded
                                                ) {
        super(sslClientCertHttpHeader, sslCertChainHttpHeaderPrefix, certificateChainLength);

      Objects.requireNonNull(intermediateCerts,"requireNonNull intermediateCerts");
      Objects.requireNonNull(trustedRootCerts,"requireNonNull trustedRootCerts");
      this.intermediateCerts = intermediateCerts;
      this.trustedRootCerts = trustedRootCerts;
      this.isTruststoreLoaded = isTruststoreLoaded;
      this.certIsUrlEncoded = certIsUrlEncoded;

        if (!this.isTruststoreLoaded) {
            log.warn("Keycloak Truststore is null or empty, but it's required for NGINX x509cert-lookup provider");
            log.warn("   see Keycloak documentation here : https://www.keycloak.org/docs/latest/server_installation/index.html#_truststore");
        }
    }

    /**
     * Removing PEM Headers and end of lines
     *
     * @param pem
     * @return
     */
    private static String removeBeginEnd(String pem) {
        pem = pem.replace(PemUtils.BEGIN_CERT, "");
        pem = pem.replace(PemUtils.END_CERT, "");
        pem = pem.replace("\r\n", "");
        pem = pem.replace("\n", "");
        return pem.trim();
    }

    /**
     * Decoding end user certificate, including URL decodeding due to ssl_client_escaped_cert nginx variable.
     */
    @Override
    protected X509Certificate decodeCertificateFromPem(String pem) throws PemException {

        if (pem == null) {
            log.warn("End user TLS Certificate is NULL! ");
            return null;
        }
        if (certIsUrlEncoded) {
            pem = java.net.URLDecoder.decode(pem, StandardCharsets.UTF_8);
        }

        if (pem.startsWith(PemUtils.BEGIN_CERT)) {
            pem = removeBeginEnd(pem);
        }

        return PemUtils.decodeCertificate(pem);
    }

    @Override
    protected void buildChain(HttpRequest httpRequest, List<X509Certificate> chain, X509Certificate clientCert) {
        log.debugf("End user certificate found : Subject DN=[%s]  SerialNumber=[%s]", clientCert.getSubjectX500Principal(), clientCert.getSerialNumber());

        // Rebuilding the end user certificate chain using Keycloak Truststore
        X509Certificate[] certChain = buildChain(clientCert);
        if (certChain == null || certChain.length == 0) {
            log.info("Impossible to rebuild end user cert chain : client certificate authentication will fail." );
            chain.add(clientCert);
        } else {
            for (X509Certificate caCert : certChain) {
                chain.add(caCert);
                log.debugf("Rebuilded user cert chain DN : %s", caCert.getSubjectX500Principal());
            }
        }
    }

    /**
     *  As NGINX cannot actually send the CA Chain in http header(s),
     *  we are rebuilding here the end user certificate chain with Keycloak truststore.
     *  <br>
     *  Please note that Keycloak truststore must contain root and intermediate CA's certificates.
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
            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters( trustAnchors, selector);

            // Disable CRL checks, as it's possibly done after depending on Keycloak settings
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
            log.debug("Certification path building OK, and contains " + certPath.getCertificates().size() + " X509 Certificates");

            userCertChain = convertCertPathToX509CertArray(certPath);

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            log.error(e.getLocalizedMessage(),e);
        } catch (CertPathBuilderException e) {
            if (log.isEnabled(Level.TRACE)) {
                log.debug(e.getLocalizedMessage(),e);
            } else {
                log.warn(e.getLocalizedMessage());
            }
        } finally {
            if (isTruststoreLoaded) {
                //Remove end user certificate
                intermediateCerts.remove(endUserAuthCert);
            }
        }

        return userCertChain;
    }


    private X509Certificate[] convertCertPathToX509CertArray(CertPath certPath ) {

        X509Certificate[] x509certChain = new X509Certificate[0];
        if (certPath == null){
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
