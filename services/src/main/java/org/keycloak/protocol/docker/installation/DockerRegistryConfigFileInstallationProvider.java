package org.keycloak.protocol.docker.installation;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

public class DockerRegistryConfigFileInstallationProvider implements ClientInstallationProvider {

    @Override
    public ClientInstallationProvider create(final KeycloakSession session) {
        return this;
    }

    @Override
    public void init(final Config.Scope config) {
        // no-op
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public String getId() {
        return "docker-v2-registry-config-file";
    }

    @Override
    public Response generateInstallation(final KeycloakSession session, final RealmModel realm, final ClientModel client, final URI serverBaseUri) {
        final StringBuilder responseString = new StringBuilder("auth:\n")
                .append("  token:\n")
                .append("    realm: ").append(serverBaseUri).append("/realms/").append(realm.getName()).append("/protocol/").append(DockerAuthV2Protocol.LOGIN_PROTOCOL).append("/auth\n")
                .append("    service: ").append(client.getClientId()).append("\n")
                .append("    issuer: ").append(serverBaseUri).append("/realms/").append(realm.getName()).append("\n");
        return Response.ok(responseString.toString(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return DockerAuthV2Protocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Registry Config File";
    }

    @Override
    public String getHelpText() {
        return "Provides a registry configuration file snippet for use with this client";
    }

    @Override
    public String getFilename() {
        return "config.yml";
    }

    @Override
    public String getMediaType() {
        return MediaType.TEXT_PLAIN;
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }
}
