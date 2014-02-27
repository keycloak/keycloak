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
import javax.ws.rs.container.ResourceContext;
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

    @Context
    protected ResourceContext resourceContext;
    private RealmAuth auth;

    public OAuthClientsResource(RealmModel realm, RealmAuth auth, KeycloakSession session) {
        this.auth = auth;
        this.realm = realm;
        this.session = session;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<OAuthClientRepresentation> getOAuthClients() {
        List<OAuthClientRepresentation> rep = new ArrayList<OAuthClientRepresentation>();
        List<OAuthClientModel> oauthModels = realm.getOAuthClients();

        boolean view = auth.hasView();
        for (OAuthClientModel oauth : oauthModels) {
            if (view) {
                rep.add(OAuthClientManager.toRepresentation(oauth));
            } else {
                OAuthClientRepresentation client = new OAuthClientRepresentation();
                client.setName(oauth.getClientId());
                rep.add(client);
            }
        }
        return rep;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOAuthClient(final @Context UriInfo uriInfo, final OAuthClientRepresentation rep) {
        auth.requireManage();

        OAuthClientManager resourceManager = new OAuthClientManager(realm);
        OAuthClientModel oauth = resourceManager.create(rep);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(oauth.getId()).build()).build();
    }

    @Path("{id}")
    public OAuthClientResource getOAuthClient(final @PathParam("id") String id) {
        auth.requireView();

        OAuthClientModel oauth = realm.getOAuthClientById(id);
        if (oauth == null) {
            throw new NotFoundException();
        }
        OAuthClientResource oAuthClientResource = new OAuthClientResource(realm, auth, oauth, session);
        resourceContext.initResource(oAuthClientResource);
        return oAuthClientResource;
    }

}
