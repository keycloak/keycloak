package org.keycloak.protocol.oidc;

import org.keycloak.Config;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.representations.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientDescriptionConverter implements ClientDescriptionConverter, ClientDescriptionConverterFactory {

    @Override
    public boolean isSupported(String description) {
        description = description.trim();
        return (description.startsWith("{") && description.endsWith("}") && description.contains("\"redirect_uris\""));
    }

    @Override
    public ClientRepresentation convertToInternal(String description) {
        try {
            OIDCClientRepresentation oidcRep = JsonSerialization.readValue(description, OIDCClientRepresentation.class);

            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(KeycloakModelUtils.generateId());
            client.setName(oidcRep.getClientName());
            client.setRedirectUris(oidcRep.getRedirectUris());
            client.setBaseUrl(oidcRep.getClientUri());

            return client;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ClientDescriptionConverter create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "openid-connect";
    }

}
