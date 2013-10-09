package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.OAuthClientManager;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientResource  {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    protected OAuthClientModel oauthClient;
    protected KeycloakSession session;

    public OAuthClientResource(RealmModel realm, OAuthClientModel oauthClient, KeycloakSession session) {
        this.realm = realm;
        this.oauthClient = oauthClient;
        this.session = session;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(final OAuthClientRepresentation rep) {
        OAuthClientManager manager = new OAuthClientManager(realm);
        manager.update(rep, oauthClient);
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthClientRepresentation getApplication() {
        OAuthClientManager manager = new OAuthClientManager(realm);
        return OAuthClientManager.toRepresentation(oauthClient);
    }

    @Path("credentials")
    @PUT
    @Consumes("application/json")
    public void updateCredentials(List<CredentialRepresentation> credentials) {
        logger.info("updateCredentials");
        if (credentials == null) return;

        for (CredentialRepresentation rep : credentials) {
            UserCredentialModel cred = RealmManager.fromRepresentation(rep);
            realm.updateCredential(oauthClient.getOAuthAgent(), cred);
        }
    }

    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, oauthClient.getOAuthAgent(), session);
    }


}
