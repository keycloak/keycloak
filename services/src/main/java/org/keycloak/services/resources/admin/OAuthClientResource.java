package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.services.managers.ModelToRepresentation;
import org.keycloak.services.managers.OAuthClientManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientResource  {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    protected OAuthClientModel oauthClient;
    protected KeycloakSession session;
    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication application;

    protected KeycloakApplication getApplication() {
        return (KeycloakApplication)application;
    }

    public OAuthClientResource(RealmModel realm, RealmAuth auth, OAuthClientModel oauthClient, KeycloakSession session) {
        this.realm = realm;
        this.auth = auth;
        this.oauthClient = oauthClient;
        this.session = session;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @Path("claims")
    public ClaimResource getClaimResource() {
        return new ClaimResource(oauthClient);
    }


    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final OAuthClientRepresentation rep) {
        auth.requireManage();

        OAuthClientManager manager = new OAuthClientManager(realm);
        try {
            manager.update(rep, oauthClient);
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Client " + rep.getName() + " already exists");
        }
    }


    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public OAuthClientRepresentation getOAuthClient() {
        auth.requireView();

        return OAuthClientManager.toRepresentation(oauthClient);
    }

    @GET
    @NoCache
    @Path("installation")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstallation() throws IOException {
        auth.requireView();

        OAuthClientManager manager = new OAuthClientManager(realm);
        Object rep = manager.toInstallationRepresentation(realm, oauthClient, getApplication().getBaseUri(uriInfo));

        // TODO Temporary solution to pretty-print
        return JsonSerialization.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rep);
    }

    @DELETE
    @NoCache
    public void deleteOAuthClient() {
        auth.requireManage();

        realm.removeOAuthClient(oauthClient.getId());
    }

    @Path("client-secret")
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public CredentialRepresentation regenerateSecret() {
        auth.requireManage();

        logger.debug("regenerateSecret");
        UserCredentialModel cred = UserCredentialModel.generateSecret();
        oauthClient.setSecret(cred.getValue());
        CredentialRepresentation rep = ModelToRepresentation.toRepresentation(cred);
        return rep;
    }

    @Path("client-secret")
    @GET
    @Produces("application/json")
    public CredentialRepresentation getClientSecret() {
        auth.requireView();

        logger.debug("getClientSecret");
        UserCredentialModel model = UserCredentialModel.secret(oauthClient.getSecret());
        if (model == null) throw new NotFoundException("Application does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }

    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, oauthClient, session);
    }


}
