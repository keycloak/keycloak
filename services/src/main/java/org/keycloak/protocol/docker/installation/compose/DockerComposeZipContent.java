package org.keycloak.protocol.docker.installation.compose;

import java.net.URL;
import java.security.cert.Certificate;

public class DockerComposeZipContent {

    private final DockerComposeYamlFile yamlFile;
    private final String dataDirectoryName;
    private final DockerComposeCertsDirectory certsDirectory;

    public DockerComposeZipContent(final Certificate realmCert, final URL realmBaseUrl, final String realmName, final String clientId) {
        final String dataDirectoryName = "data";
        final String certsDirectoryName = "certs";
        final String registryCertFilename = "localhost.crt";
        final String registryKeyFilename = "localhost.key";
        final String idpCertTrustChainFilename = "localhost_trust_chain.pem";

        this.yamlFile = new DockerComposeYamlFile(dataDirectoryName, certsDirectoryName, "/opt/" + certsDirectoryName, registryCertFilename, registryKeyFilename, idpCertTrustChainFilename, realmBaseUrl, realmName, clientId);
        this.dataDirectoryName = dataDirectoryName;
        this.certsDirectory = new DockerComposeCertsDirectory(certsDirectoryName, realmCert, registryCertFilename, registryKeyFilename, idpCertTrustChainFilename, realmName);
    }

    public DockerComposeYamlFile getYamlFile() {
        return yamlFile;
    }

    public String getDataDirectoryName() {
        return dataDirectoryName;
    }

    public DockerComposeCertsDirectory getCertsDirectory() {
        return certsDirectory;
    }
}
