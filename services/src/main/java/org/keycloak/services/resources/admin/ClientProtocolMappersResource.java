package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientProtocolMappersResource {
    protected static final Logger logger = Logger.getLogger(ClientProtocolMappersResource.class);
    protected ClientModel client;
    protected RealmModel realm;
    protected RealmAuth auth;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public ClientProtocolMappersResource(RealmModel realm, RealmAuth auth, ClientModel client) {
        this.auth = auth;
        this.realm = realm;
        this.client = client;

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Map of mappers by name for a specific protocol attached to the client
     *
     * @param protocol
     * @return
     */
    @GET
    @NoCache
    @Path("protocol/{protocol}")
    @Produces("application/json")
    public List<ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol) {
        auth.requireView();
        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : client.getProtocolMappers()) {
            mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    /**
     * Add mappers to client.
     *
     * @param mapperIds List of mapper ids
     */
    @Path("models")
    @POST
    @NoCache
    @Consumes("application/json")
    public void addMappers(Set<String> mapperIds) {
        auth.requireManage();
        client.addProtocolMappers(mapperIds);
    }

    /**
     * remove client mappers.
     *
     * @param mapperIds  List of mapper ids
     */
    @Path("models")
    @DELETE
    @NoCache
    @Consumes("application/json")
    public void removeMappers(Set<String> mapperIds) {
        auth.requireManage();
        client.removeProtocolMappers(mapperIds);
    }

    @GET
    @NoCache
    @Path("models")
    @Produces("application/json")
    public List<ProtocolMapperRepresentation> getMappers() {
        auth.requireView();
        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : realm.getProtocolMappers()) {
            mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }




}
