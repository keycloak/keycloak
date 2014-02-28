package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
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
        model.setSecret(rep.getSecret());
        if (rep.getClaims() != null) {
            ClaimManager.setClaims(model, rep.getClaims());
        }
        return model;
    }

    public void update(OAuthClientRepresentation rep, OAuthClientModel model) {
        model.setEnabled(rep.isEnabled());
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
    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getClientId());
        rep.setEnabled(model.isEnabled());
        Set<String> redirectUris = model.getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = model.getWebOrigins();
        if (webOrigins != null) {
            rep.setWebOrigins(new LinkedList<String>(webOrigins));
        }
        return rep;
    }

    @JsonPropertyOrder({"realm", "realm-public-key", "auth-server-url", "ssl-not-required",
            "resource", "credentials"})
    public static class InstallationAdapterConfig extends BaseRealmConfig {
        @JsonProperty("resource")
        protected String resource;
        @JsonProperty("credentials")
        protected Map<String, String> credentials = new HashMap<String, String>();

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

    }


    public InstallationAdapterConfig toInstallationRepresentation(RealmModel realmModel, OAuthClientModel model, URI baseUri) {
        InstallationAdapterConfig rep = new InstallationAdapterConfig();
        rep.setRealm(realmModel.getName());
        rep.setRealmKey(realmModel.getPublicKeyPem());
        rep.setSslNotRequired(realmModel.isSslNotRequired());
        rep.setAuthServerUrl(baseUri.toString());

        rep.setResource(model.getClientId());

        Map<String, String> creds = new HashMap<String, String>();
        creds.put(CredentialRepresentation.SECRET, model.getSecret());
        rep.setCredentials(creds);

        return rep;
    }
}
