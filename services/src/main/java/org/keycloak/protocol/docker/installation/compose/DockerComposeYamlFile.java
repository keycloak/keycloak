package org.keycloak.protocol.docker.installation.compose;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Representation of the docker-compose.yaml file
 */
public class DockerComposeYamlFile {

    private final String registryDataDirName;
    private final String localCertDirName;
    private final String containerCertPath;
    private final String localhostCrtFileName;
    private final String localhostKeyFileName;
    private final String authServerTrustChainFileName;
    private final URL authServerUrl;
    private final String realmName;
    private final String serviceId;

    /**
     * @param registryDataDirName Directory name to be used for both the container's storage directory, as well as the local data directory name
     * @param localCertDirName Name of the (relative) local directory that holds the certs
     * @param containerCertPath Path at which the local certs directory should be mounted on the container
     * @param localhostCrtFileName SSL Cert file name for the registry
     * @param localhostKeyFileName SSL Key file name for the registry
     * @param authServerTrustChainFileName IDP trust chain, used for auth token validation
     * @param authServerUrl Root URL for Keycloak, commonly something like http://localhost:8080/auth for dev environments
     * @param realmName Name of the realm for which the docker client is configured
     * @param serviceId Docker's Service ID, corresponds to Keycloak's client ID
     */
    public DockerComposeYamlFile(final String registryDataDirName, final String localCertDirName, final String containerCertPath, final String localhostCrtFileName, final String localhostKeyFileName, final String authServerTrustChainFileName, final URL authServerUrl, final String realmName, final String serviceId) {
        this.registryDataDirName = registryDataDirName;
        this.localCertDirName = localCertDirName;
        this.containerCertPath = containerCertPath;
        this.localhostCrtFileName = localhostCrtFileName;
        this.localhostKeyFileName = localhostKeyFileName;
        this.authServerTrustChainFileName = authServerTrustChainFileName;
        this.authServerUrl = authServerUrl;
        this.realmName = realmName;
        this.serviceId = serviceId;
    }

    public byte[] generateDockerComposeFileBytes() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(output);

        writer.print("registry:\n");
        writer.print("  image: registry:2\n");
        writer.print("  ports:\n");
        writer.print("    - 127.0.0.1:5000:5000\n");
        writer.print("  environment:\n");
        writer.print("    REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY: /" + registryDataDirName + "\n");
        writer.print("    REGISTRY_HTTP_TLS_CERTIFICATE: " + containerCertPath + "/" + localhostCrtFileName + "\n");
        writer.print("    REGISTRY_HTTP_TLS_KEY: " + containerCertPath + "/" + localhostKeyFileName + "\n");
        writer.print("    REGISTRY_AUTH_TOKEN_REALM: " + authServerUrl + "realms/" + realmName + "/protocol/docker-v2/auth\n");
        writer.print("    REGISTRY_AUTH_TOKEN_SERVICE: " + serviceId + "\n");
        writer.print("    REGISTRY_AUTH_TOKEN_ISSUER: " + authServerUrl + "realms/" + realmName + "\n");
        writer.print("    REGISTRY_AUTH_TOKEN_ROOTCERTBUNDLE: " + containerCertPath + "/" + authServerTrustChainFileName + "\n");
        writer.print("  volumes:\n");
        writer.print("    - ./" + registryDataDirName + ":/" + registryDataDirName + ":z\n");
        writer.print("    - ./" + localCertDirName + ":" + containerCertPath + ":z");

        writer.flush();
        writer.close();

        return output.toByteArray();
    }
}
