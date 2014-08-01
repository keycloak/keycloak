package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.CredentialRepresentation;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientManager {

    private RealmManager realmManager;

    public OAuthClientManager() {
    }

    public OAuthClientManager(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    public boolean removeClient(RealmModel realm, OAuthClientModel client) {
        if (realm.removeOAuthClient(client.getId())) {
            UserSessionProvider sessions = realmManager.getSession().sessions();
            if (sessions != null) {
                realmManager.getSession().sessions().onClientRemoved(realm, client);
            }
            return true;
        } else {
            return false;
        }
    }

    @JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-required",
            "resource", "public-client", "credentials"})
    public static class InstallationAdapterConfig extends BaseRealmConfig {
        @JsonProperty("public-client")
        protected Boolean publicClient;
        @JsonProperty("resource")
        protected String resource;
        @JsonProperty("credentials")
        protected Map<String, String> credentials;

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public Map<String, String> getCredentials() {
            return credentials;
        }

        public void setCredentials(Map<String, String> credentials) {
            this.credentials = credentials;
        }

        public Boolean getPublicClient() {
            return publicClient;
        }

        public void setPublicClient(Boolean publicClient) {
            this.publicClient = publicClient;
        }
    }


    public InstallationAdapterConfig toInstallationRepresentation(RealmModel realmModel, OAuthClientModel model, URI baseUri) {
        InstallationAdapterConfig rep = new InstallationAdapterConfig();
        rep.setRealm(realmModel.getName());
        rep.setRealmKey(realmModel.getPublicKeyPem());
        rep.setSslRequired(realmModel.getSslRequired().name().toLowerCase());
        rep.setAuthServerUrl(baseUri.toString());
        if (model.isPublicClient()) rep.setPublicClient(true);

        rep.setResource(model.getClientId());

        if (!model.isPublicClient()) {
            Map<String, String> creds = new HashMap<String, String>();
            creds.put(CredentialRepresentation.SECRET, model.getSecret());
            rep.setCredentials(creds);
        }

        return rep;
    }
}
