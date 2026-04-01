package org.keycloak.protocol.oauth2.cimd.provider;

import java.net.URI;
import java.net.URISyntaxException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.representations.idm.ClientRepresentation;

import org.jboss.logging.Logger;

import static org.keycloak.models.ClientScopeModel.CONSENT_SCREEN_TEXT;
import static org.keycloak.models.ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN;

/**
 *  The class is a concrete class of {@link AbstractPersistentClientIdMetadataDocumentProvider}.
 *
 * <p>Client Metadata Augmentation in {@link ClientRepresentation}:
 * The class provide the following policies:
 * <ul>
 *     <li>Consent required: to mitigate the risk of phishing,
 *     the CIMD and MCP specification requires an authorization server to show information on a client on the consent screen.</li>
 *     <li>Full scope allowed: to follow least-privilege principle, only required scopes are permitted to a client.</li>
 * </ul>
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class PersistentClientIdMetadataDocumentProvider extends AbstractPersistentClientIdMetadataDocumentProvider<ClientIdMetadataDocumentExecutor.Configuration> {

    protected Logger logger = Logger.getLogger(PersistentClientIdMetadataDocumentProvider.class);

    public Logger getLogger() {
        return logger;
    }

    public PersistentClientIdMetadataDocumentProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    public ClientIdMetadataDocumentExecutor.Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(ClientIdMetadataDocumentExecutor.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void augmentClientMetadata(ClientRepresentation clientRep) {
        clientRep.setConsentRequired(true);
        clientRep.setFullScopeAllowed(false);

        // to show information on a client on the consent screen.
        clientRep.getAttributes().put(DISPLAY_ON_CONSENT_SCREEN, "true");
        URI uri = null;
        try {
            uri = new URI(clientRep.getClientId());
        } catch (URISyntaxException e) {
            return;
        }

        // The authorization server SHOULD display the hostname of the client_id on the authorization interface,
        // in addition to displaying the fetched client information if any.
        // TODO: better show other information on a client but not determined what information should be shown.
        clientRep.getAttributes().put(CONSENT_SCREEN_TEXT, "The client's hostname is " + uri.getHost());
    }
}
