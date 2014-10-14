package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.OAuthClientRepresentation;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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

    /*
    @Context
    protected ResourceContext resourceContext;

    */
    private RealmAuth auth;

    public OAuthClientsResource(RealmModel realm, RealmAuth auth, KeycloakSession session) {
        this.auth = auth;
        this.realm = realm;
        this.session = session;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    /**
     * Get a list of oauth clients in this realm.
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<OAuthClientRepresentation> getOAuthClients() {
        List<OAuthClientRepresentation> rep = new ArrayList<OAuthClientRepresentation>();
        List<OAuthClientModel> oauthModels = realm.getOAuthClients();

        boolean view = auth.hasView();
        for (OAuthClientModel oauth : oauthModels) {
            if (view) {
                rep.add(ModelToRepresentation.toRepresentation(oauth));
            } else {
                OAuthClientRepresentation client = new OAuthClientRepresentation();
                client.setName(oauth.getClientId());
                rep.add(client);
            }
        }
        return rep;
    }

    /**
     * Create an oauth client
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOAuthClient(final @Context UriInfo uriInfo, final OAuthClientRepresentation rep) {
        auth.requireManage();

        try {
            OAuthClientModel oauth = RepresentationToModel.createOAuthClient(rep, realm);
            return Response.created(uriInfo.getAbsolutePathBuilder().path(getClientPath(oauth)).build()).build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Client " + rep.getName() + " already exists");
        }
    }

    protected String getClientPath(OAuthClientModel oauth) {
        return oauth.getClientId();
    }

    /**
     * Base path to manage one specific oauth client
     *
     * @param clientId oauth client's clientId
     * @return
     */
    @Path("{clientId}")
    public OAuthClientResource getOAuthClient(final @PathParam("clientId") String clientId) {
        auth.requireView();

        OAuthClientModel oauth = getOAuthClientModel(clientId);
        if (oauth == null) {
            throw new NotFoundException("OAuth Client not found");
        }
        OAuthClientResource oAuthClientResource = new OAuthClientResource(realm, auth, oauth, session);
        ResteasyProviderFactory.getInstance().injectProperties(oAuthClientResource);
        //resourceContext.initResource(oAuthClientResource);
        return oAuthClientResource;
    }

    protected OAuthClientModel getOAuthClientModel(String clientId) {
        return realm.getOAuthClient(clientId);
    }

}
