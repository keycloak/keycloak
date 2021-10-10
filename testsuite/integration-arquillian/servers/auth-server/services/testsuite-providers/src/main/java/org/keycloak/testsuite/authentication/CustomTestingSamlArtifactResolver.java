package org.keycloak.testsuite.authentication;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.ArtifactResolver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.keycloak.testsuite.authentication.CustomTestingSamlArtifactResolverFactory.TYPE_CODE;


/**
 * This ArtifactResolver should be used only for testing purposes.
 */
public class CustomTestingSamlArtifactResolver implements ArtifactResolver {

    public static List<String> list = new ArrayList<>();

    @Override
    public ClientModel selectSourceClient(KeycloakSession session, String artifact) {
        return null;
    }

    @Override
    public String buildArtifact(AuthenticatedClientSessionModel clientSessionModel, String entityId, String artifactResponse) {
        int artifactIndex = list.size();
        list.add(artifactResponse);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(TYPE_CODE);
            bos.write(artifactIndex);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] artifact = bos.toByteArray();
        return Base64.getEncoder().encodeToString(artifact);
    }

    @Override
    public String resolveArtifact(AuthenticatedClientSessionModel clientSessionModel, String artifact) {
        byte[] byteArray = Base64.getDecoder().decode(artifact);
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        bis.skip(2);
        int index = bis.read();

        return list.get(index);
    }

    @Override
    public void close() {

    }
}
