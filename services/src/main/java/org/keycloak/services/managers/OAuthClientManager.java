package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientManager {
    protected RealmModel realm;

    public OAuthClientManager(RealmModel realm) {
        this.realm = realm;
    }

    public UserCredentialModel generateSecret(OAuthClientModel app) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        app.setSecret(secret.getValue());
        return secret;
    }


    public OAuthClientModel create(String name) {
        OAuthClientModel model = realm.addOAuthClient(name);
        generateSecret(model);
        return model;
    }

    public OAuthClientModel create(OAuthClientRepresentation rep) {
        OAuthClientModel model = create(rep.getName());
        update(rep, model);
        return model;
    }

    public void update(OAuthClientRepresentation rep, OAuthClientModel model) {
        if (rep.getName() != null) model.setClientId(rep.getName());
        if (rep.isEnabled() != null) model.setEnabled(rep.isEnabled());
        if (rep.isPublicClient() != null) model.setPublicClient(rep.isPublicClient());
        if (rep.getClaims() != null) {
            ClaimManager.setClaims(model, rep.getClaims());
        }
        if (rep.getNotBefore() != null) {
            model.setNotBefore(rep.getNotBefore());
        }
        if (rep.getSecret() != null) model.setSecret(rep.getSecret());
        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            model.setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            model.setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getClaims() != null) {
            ClaimManager.setClaims(model, rep.getClaims());
        }

        if (rep.getNotBefore() != null) {
            model.setNotBefore(rep.getNotBefore());
        }

    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getClientId());
        rep.setEnabled(model.isEnabled());
        rep.setPublicClient(model.isPublicClient());
        Set<String> redirectUris = model.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = model.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }
        rep.setNotBefore(model.getNotBefore());
        return rep;
    }

    @JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-not-required",
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
        rep.setSslNotRequired(realmModel.isSslNotRequired());
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
