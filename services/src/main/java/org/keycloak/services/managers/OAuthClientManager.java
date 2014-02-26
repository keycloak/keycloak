package org.keycloak.services.managers;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.Constants;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.adapters.config.BaseAdapterConfig;
import org.keycloak.representations.adapters.config.BaseRealmConfig;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.services.resources.flows.Urls;

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

    public UserCredentialModel generateSecret(RealmModel realm, OAuthClientModel app) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        realm.updateCredential(app.getOAuthAgent(), secret);
        return secret;
    }


    public OAuthClientModel create(String name) {
        OAuthClientModel model = realm.addOAuthClient(name);
        RoleModel role = realm.getRole(Constants.IDENTITY_REQUESTER_ROLE);
        realm.grantRole(model.getOAuthAgent(), role);
        generateSecret(realm, model);
        return model;
    }

    public OAuthClientModel create(OAuthClientRepresentation rep) {
        OAuthClientModel model = create(rep.getName());
        update(rep, model);
        UserModel resourceUser = model.getOAuthAgent();
        if (rep.getCredentials() != null) {
            for (CredentialRepresentation cred : rep.getCredentials()) {
                UserCredentialModel credential = new UserCredentialModel();
                credential.setType(cred.getType());
                credential.setValue(cred.getValue());
                realm.updateCredential(resourceUser, credential);
            }
        }
        if (rep.getClaims() != null) {
            ClaimManager.setClaims(model, rep.getClaims());
        } else {
            model.setAllowedClaimsMask(ClaimMask.USERNAME);
        }

        return model;
    }

    public void update(OAuthClientRepresentation rep, OAuthClientModel model) {
        model.getOAuthAgent().setEnabled(rep.isEnabled());
        List<String> redirectUris = rep.getRedirectUris();
        if (redirectUris != null) {
            model.getOAuthAgent().setRedirectUris(new HashSet<String>(redirectUris));
        }

        List<String> webOrigins = rep.getWebOrigins();
        if (webOrigins != null) {
            model.getOAuthAgent().setWebOrigins(new HashSet<String>(webOrigins));
        }

        if (rep.getClaims() != null) {
            ClaimManager.setClaims(model, rep.getClaims());
        }
    }

    public static OAuthClientRepresentation toRepresentation(OAuthClientModel model) {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setId(model.getId());
        rep.setName(model.getOAuthAgent().getLoginName());
        rep.setEnabled(model.getOAuthAgent().isEnabled());
        Set<String> redirectUris = model.getOAuthAgent().getRedirectUris();
        if (redirectUris != null) {
            rep.setRedirectUris(new LinkedList<String>(redirectUris));
        }

        Set<String> webOrigins = model.getOAuthAgent().getWebOrigins();
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

        rep.setResource(model.getOAuthAgent().getLoginName());

        Map<String, String> creds = new HashMap<String, String>();
        creds.put(CredentialRepresentation.SECRET, realmModel.getSecret(model.getOAuthAgent()).getValue());
        rep.setCredentials(creds);

        return rep;
    }
}
