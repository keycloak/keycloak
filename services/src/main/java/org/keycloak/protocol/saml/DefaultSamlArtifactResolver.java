package org.keycloak.protocol.saml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.util.ArtifactBindingUtils;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.protocol.saml.DefaultSamlArtifactResolverFactory.TYPE_CODE;
import static org.keycloak.protocol.saml.SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER;

/**
 * ArtifactResolver for artifact-04 format.
 * Other kind of format for artifact are allowed by standard but not specified.
 * Artifact 04 is the only one specified in SAML2.0 specification.
 */
public class DefaultSamlArtifactResolver implements ArtifactResolver {


    protected static final Logger logger = Logger.getLogger(SamlService.class);

    @Override
    public String resolveArtifact(AuthenticatedClientSessionModel clientSessionModel, String artifact) throws ArtifactResolverProcessingException {
        String artifactResponseString = clientSessionModel.getNote(GeneralConstants.SAML_ARTIFACT_KEY + "=" + artifact);
        clientSessionModel.removeNote(GeneralConstants.SAML_ARTIFACT_KEY + "=" + artifact);

        logger.tracef("Artifact response for artifact %s, is %s", artifact, artifactResponseString);

        if (StringUtil.isNullOrEmpty(artifactResponseString)) {
            throw new ArtifactResolverProcessingException("Artifact not present in ClientSession.");
        }

        return artifactResponseString;
    }

    @Override
    public ClientModel selectSourceClient(KeycloakSession session, String artifact) throws ArtifactResolverProcessingException {
        byte[] source = extractSourceFromArtifact(artifact);
        String identifier = ArtifactBindingUtils.getArtifactBindingIdentifierString(source);

        return session.clients().searchClientsByAttributes(session.getContext().getRealm(),
                Collections.singletonMap(SAML_ARTIFACT_BINDING_IDENTIFIER, identifier), 0, 1)
                .findFirst().orElseThrow(() -> new ArtifactResolverProcessingException("No client matching the artifact source found"));
    }

    @Override
    public String buildArtifact(AuthenticatedClientSessionModel clientSessionModel, String entityId, String artifactResponse) throws ArtifactResolverProcessingException {
        String artifact = createArtifact(entityId);

        clientSessionModel.setNote(GeneralConstants.SAML_ARTIFACT_KEY + "=" + artifact, artifactResponse);

        return artifact;
    }

    private void assertSupportedArtifactFormat(String artifactString) throws ArtifactResolverProcessingException {
        byte[] artifact = Base64.getDecoder().decode(artifactString);

        if (artifact.length != 44) {
            throw new ArtifactResolverProcessingException("Artifact " + artifactString + " has a length of " + artifact.length + ". It should be 44");
        }
        if (artifact[0] != TYPE_CODE[0] || artifact[1] != TYPE_CODE[1]) {
            throw new ArtifactResolverProcessingException("Artifact " + artifactString + " does not start with 0x0004");
        }
    }

    private byte[] extractSourceFromArtifact(String artifactString) throws ArtifactResolverProcessingException {
        assertSupportedArtifactFormat(artifactString);

        byte[] artifact = Base64.getDecoder().decode(artifactString);

        byte[] source = new byte[20];
        System.arraycopy(artifact, 4, source, 0, source.length);

        return source;
    }

    /**
     * Creates an artifact. Format is:
     * <p>
     * SAML_artifact := B64(TypeCode EndpointIndex RemainingArtifact)
     * <p>
     * TypeCode := 0x0004
     * EndpointIndex := Byte1Byte2
     * RemainingArtifact := SourceID MessageHandle
     * <p>
     * SourceID := 20-byte_sequence, used by the artifact receiver to determine artifact issuer
     * MessageHandle := 20-byte_sequence
     *
     * @param entityId the entity id to encode in the sourceId
     * @return an artifact
     * @throws ArtifactResolverProcessingException
     */
    public String createArtifact(String entityId) throws ArtifactResolverProcessingException {
        try {
            SecureRandom handleGenerator = new SecureRandom();
            byte[] trimmedIndex = new byte[2];

            byte[] source = ArtifactBindingUtils.computeArtifactBindingIdentifier(entityId);

            byte[] assertionHandle = new byte[20];
            handleGenerator.nextBytes(assertionHandle);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bos.write(TYPE_CODE);
            bos.write(trimmedIndex);
            bos.write(source);
            bos.write(assertionHandle);

            byte[] artifact = bos.toByteArray();

            return Base64.getEncoder().encodeToString(artifact);
        } catch (IOException e) {
            throw new ArtifactResolverProcessingException(e);
        }

    }

    @Override
    public void close() {

    }

}
