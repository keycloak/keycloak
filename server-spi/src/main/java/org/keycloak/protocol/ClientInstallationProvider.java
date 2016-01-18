package org.keycloak.protocol;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Provides a template/sample client config adapter file.  For example keycloak.json for our OIDC adapter.  keycloak-saml.xml for our SAML client adapter
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientInstallationProvider extends Provider, ProviderFactory<ClientInstallationProvider> {
    Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri);
    String getProtocol();
    String getDisplayType();
    String getHelpText();
    String getFilename();
    String getMediaType();
    boolean isDownloadOnly();
}
