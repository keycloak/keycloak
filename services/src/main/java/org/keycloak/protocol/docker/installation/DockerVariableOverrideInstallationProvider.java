package org.keycloak.protocol.docker.installation;

import java.net.URI;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;

public class DockerVariableOverrideInstallationProvider implements ClientInstallationProvider {

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
        return "docker-v2-variable-override";
    }

    // TODO "auth" is not guaranteed to be the endpoint, fix it
    @Override
    public Response generateInstallation(final KeycloakSession session, final RealmModel realm, final ClientModel client, final URI serverBaseUri) {
        final StringBuilder builder = new StringBuilder()
                .append("-e REGISTRY_AUTH_TOKEN_REALM=").append(serverBaseUri).append("/realms/").append(realm.getName()).append("/protocol/").append(DockerAuthV2Protocol.LOGIN_PROTOCOL).append("/auth \\\n")
                .append("-e REGISTRY_AUTH_TOKEN_SERVICE=").append(client.getClientId()).append(" \\\n")
                .append("-e REGISTRY_AUTH_TOKEN_ISSUER=").append(serverBaseUri).append("/realms/").append(realm.getName()).append(" \\\n");
        return Response.ok(builder.toString(), MediaType.TEXT_PLAIN_TYPE).build();
    }

    @Override
    public String getProtocol() {
        return DockerAuthV2Protocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "Variable Override";
    }

    @Override
    public String getHelpText() {
        return "Configures environment variable overrides, typically used with a docker-compose.yaml configuration for a docker registry";
    }

    @Override
    public String getFilename() {
        return "docker-env.txt";
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
