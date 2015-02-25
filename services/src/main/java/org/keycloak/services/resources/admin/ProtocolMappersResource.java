package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.KerberosConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserFederationProviderFactoryRepresentation;
import org.keycloak.representations.idm.UserFederationProviderRepresentation;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.timer.TimerProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base resource for managing users
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMappersResource {
    protected static final Logger logger = Logger.getLogger(ProtocolMappersResource.class);

    protected RealmModel realm;

    protected  RealmAuth auth;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakSession session;

    public ProtocolMappersResource(RealmModel realm, RealmAuth auth) {
        this.auth = auth;
        this.realm = realm;

        auth.init(RealmAuth.Resource.USER);
    }

    /**
     * Map of mappers by name for a specific protocol
     *
     * @param protocol
     * @return
     */
    @GET
    @NoCache
    @Path("protocol/{protocol}")
    @Produces("application/json")
    public Map<String, ProtocolMapperRepresentation> getMappersPerProtocol(@PathParam("protocol") String protocol) {
        auth.requireView();
        Map<String, ProtocolMapperRepresentation> mappers = new HashMap<String, ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : realm.getProtocolMappers()) {
            mappers.put(mapper.getName(), ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    /**
     * createa mapper
     *
     * @param rep
     */
    @Path("models")
    @POST
    @NoCache
    @Consumes("application/json")
    public Response createMapper(ProtocolMapperRepresentation rep) {
        auth.requireManage();
        ProtocolMapperModel model = RepresentationToModel.toModel(rep);
        realm.addProtocolMapper(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();
    }

    @GET
    @NoCache
    @Path("models")
    @Produces("application/json")
    public List<ProtocolMapperRepresentation> getMappersPerProtocol() {
        auth.requireView();
        List<ProtocolMapperRepresentation> mappers = new LinkedList<ProtocolMapperRepresentation>();
        for (ProtocolMapperModel mapper : realm.getProtocolMappers()) {
            mappers.add(ModelToRepresentation.toRepresentation(mapper));
        }
        return mappers;
    }

    @GET
    @NoCache
    @Path("models/{id}")
    @Produces("application/json")
    public ProtocolMapperRepresentation getMapperById(@PathParam("id") String id) {
        auth.requireView();
        ProtocolMapperModel model = realm.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    @PUT
    @NoCache
    @Path("models/{id}")
    @Consumes("application/json")
    public void update(@PathParam("id") String id, ProtocolMapperRepresentation rep) {
        auth.requireManage();
        ProtocolMapperModel model = realm.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);
        realm.updateProtocolMapper(model);
    }

    @DELETE
    @NoCache
    @Path("models/{id}")
    public void delete(@PathParam("id") String id) {
        auth.requireManage();
        ProtocolMapperModel model = realm.getProtocolMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        realm.removeProtocolMapper(model);
    }



}
