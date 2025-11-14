package org.keycloak.protocol.docker.installation.compose;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.AbstractMap;
import java.util.Map;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.crypto.KeyType;

public class DockerComposeCertsDirectory {

    private final String directoryName;
    private final Map.Entry<String, byte[]> localhostCertFile;
    private final Map.Entry<String, byte[]> localhostKeyFile;
    private final Map.Entry<String, byte[]> idpTrustChainFile;

    public DockerComposeCertsDirectory(final String directoryName, final Certificate realmCert, final String registryCertFilename, final String registryKeyFilename, final String idpCertTrustChainFilename, final String realmName) {
        this.directoryName = directoryName;

        try {
            final KeyPairGenerator keyGen = CryptoIntegration.getProvider().getKeyPairGen(KeyType.RSA);
            keyGen.initialize(2048, new SecureRandom());

            final KeyPair keypair = keyGen.generateKeyPair();
            final PrivateKey privateKey = keypair.getPrivate();
            final Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(keypair, realmName);

            localhostCertFile = new AbstractMap.SimpleImmutableEntry<>(registryCertFilename, DockerCertFileUtils.formatCrtFileContents(certificate).getBytes());
            localhostKeyFile = new AbstractMap.SimpleImmutableEntry<>(registryKeyFilename, DockerCertFileUtils.formatPrivateKeyContents(privateKey).getBytes());
            idpTrustChainFile = new AbstractMap.SimpleEntry<>(idpCertTrustChainFilename, DockerCertFileUtils.formatCrtFileContents(realmCert).getBytes());

        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateEncodingException e) {
            // TODO throw error here descritively
            throw new RuntimeException(e);
        }
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public Map.Entry<String, byte[]> getLocalhostCertFile() {
        return localhostCertFile;
    }

    public Map.Entry<String, byte[]> getLocalhostKeyFile() {
        return localhostKeyFile;
    }

    public Map.Entry<String, byte[]> getIdpTrustChainFile() {
        return idpTrustChainFile;
    }
}
