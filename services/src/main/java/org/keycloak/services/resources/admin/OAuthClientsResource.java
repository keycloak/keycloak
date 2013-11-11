package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.services.managers.OAuthClientManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthClientsResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmModel realm;

    protected KeycloakSession session;

    public OAuthClientsResource(RealmModel realm, KeycloakSession session) {
        this.realm = realm;
        this.session = session;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<OAuthClientRepresentation> getOAuthClients() {
        List<OAuthClientRepresentation> rep = new ArrayList<OAuthClientRepresentation>();
        List<OAuthClientModel> oauthModels = realm.getOAuthClients();
        for (OAuthClientModel oauth : oauthModels) {
            rep.add(OAuthClientManager.toRepresentation(oauth));
        }
        return rep;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOAuthClient(final @Context UriInfo uriInfo, final OAuthClientRepresentation rep) {
        OAuthClientManager resourceManager = new OAuthClientManager(realm);
        OAuthClientModel oauth = resourceManager.create(rep);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(oauth.getOAuthAgent().getLoginName()).build()).build();
    }

    @Path("{id}")
    public OAuthClientResource getOAuthClient(final @PathParam("id") String id) {
        OAuthClientModel oauth = realm.getOAuthClient(id);
        if (oauth == null) {
            throw new NotFoundException();
        }
        OAuthClientResource oAuthClientResource = new OAuthClientResource(realm, oauth, session);
        return oAuthClientResource;
    }

}
