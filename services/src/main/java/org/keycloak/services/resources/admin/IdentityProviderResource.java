package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperTypeRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.social.SocialIdentityProvider;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class IdentityProviderResource {

    private static Logger logger = Logger.getLogger(IdentityProviderResource.class);

    private final RealmAuth auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final IdentityProviderModel identityProviderModel;

    @Context private UriInfo uriInfo;

    public IdentityProviderResource(RealmAuth auth, RealmModel realm, KeycloakSession session, IdentityProviderModel identityProviderModel) {
        this.realm = realm;
        this.session = session;
        this.identityProviderModel = identityProviderModel;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProviderRepresentation getIdentityProvider() {
        this.auth.requireView();
        IdentityProviderRepresentation rep = ModelToRepresentation.toRepresentation(this.identityProviderModel);

        return rep;
    }

    @DELETE
    @NoCache
    public Response delete() {
        this.auth.requireManage();

        removeClientIdentityProviders(this.realm.getClients(), this.identityProviderModel);

        this.realm.removeIdentityProviderByAlias(this.identityProviderModel.getAlias());

        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response update(IdentityProviderRepresentation providerRep) {
        try {
            this.auth.requireManage();

            String internalId = providerRep.getInternalId();
            String newProviderId = providerRep.getAlias();
            String oldProviderId = getProviderIdByInternalId(this.realm, internalId);

            this.realm.updateIdentityProvider(RepresentationToModel.toModel(providerRep));

            if (oldProviderId != null && !oldProviderId.equals(newProviderId)) {

                // Admin changed the ID (alias) of identity provider. We must update all clients and users
                logger.debug("Changing providerId in all clients and linked users. oldProviderId=" + oldProviderId + ", newProviderId=" + newProviderId);

                updateClientsAfterProviderAliasChange(this.realm.getClients(), oldProviderId, newProviderId);
                updateUsersAfterProviderAliasChange(this.session.users().getUsers(this.realm), oldProviderId, newProviderId);
            }

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Identity Provider " + providerRep.getAlias() + " already exists");
        }
    }

    // return ID of IdentityProvider from realm based on internalId of this provider
    private String getProviderIdByInternalId(RealmModel realm, String providerInternalId) {
        List<IdentityProviderModel> providerModels = realm.getIdentityProviders();
        for (IdentityProviderModel providerModel : providerModels) {
            if (providerModel.getInternalId().equals(providerInternalId)) {
                return providerModel.getAlias();
            }
        }

        return null;
    }

    private void updateClientsAfterProviderAliasChange(List<ClientModel> clients, String oldProviderId, String newProviderId) {
        for (ClientModel client : clients) {
            List<ClientIdentityProviderMappingModel> clientIdentityProviders = client.getIdentityProviders();
            boolean found = true;

            for (ClientIdentityProviderMappingModel mappingModel : clientIdentityProviders) {
                if (mappingModel.getIdentityProvider().equals(oldProviderId)) {
                    mappingModel.setIdentityProvider(newProviderId);
                    found = true;
                    break;
                }
            }

            if (found) {
                client.updateIdentityProviders(clientIdentityProviders);
            }
        }
    }

    private void updateUsersAfterProviderAliasChange(List<UserModel> users, String oldProviderId, String newProviderId) {
        for (UserModel user : users) {
            FederatedIdentityModel federatedIdentity = this.session.users().getFederatedIdentity(user, oldProviderId, this.realm);
            if (federatedIdentity != null) {
                // Remove old link first
                this.session.users().removeFederatedIdentity(this.realm, user, oldProviderId);

                // And create new
                FederatedIdentityModel newFederatedIdentity = new FederatedIdentityModel(newProviderId, federatedIdentity.getUserId(), federatedIdentity.getUserName(),
                        federatedIdentity.getToken());
                this.session.users().addFederatedIdentity(this.realm, user, newFederatedIdentity);
            }
        }
    }


    private IdentityProviderFactory getIdentityProviderFactory() {
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        for (ProviderFactory providerFactory : allProviders) {
            if (providerFactory.getId().equals(identityProviderModel.getProviderId())) return (IdentityProviderFactory)providerFactory;
        }

        return null;
    }


    @GET
    @Path("export")
    @NoCache
    public Response export(@Context UriInfo uriInfo, @QueryParam("format") String format) {
        try {
            this.auth.requireView();
            IdentityProviderFactory factory = getIdentityProviderFactory();
            return factory.create(identityProviderModel).export(uriInfo, realm, format);
        } catch (Exception e) {
            return ErrorResponse.error("Could not export public broker configuration for identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.NOT_FOUND);
        }
    }

    @GET
    @Path("mapper-types")
    @NoCache
    public Map<String, IdentityProviderMapperTypeRepresentation> getMapperTypes() {
        this.auth.requireView();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        Map<String, IdentityProviderMapperTypeRepresentation> types = new HashMap<>();
        List<ProviderFactory> factories = sessionFactory.getProviderFactories(IdentityProviderMapper.class);
        for (ProviderFactory factory : factories) {
            IdentityProviderMapper mapper = (IdentityProviderMapper)factory;
            for (String type : mapper.getCompatibleProviders()) {
                if (type.equals(identityProviderModel.getProviderId())) {
                    IdentityProviderMapperTypeRepresentation rep = new IdentityProviderMapperTypeRepresentation();
                    rep.setId(mapper.getId());
                    rep.setCategory(mapper.getDisplayCategory());
                    rep.setName(mapper.getDisplayType());
                    rep.setHelpText(mapper.getHelpText());
                    List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();
                    for (ProviderConfigProperty prop : configProperties) {
                        ConfigPropertyRepresentation propRep = new ConfigPropertyRepresentation();
                        propRep.setName(prop.getName());
                        propRep.setLabel(prop.getLabel());
                        propRep.setType(prop.getType());
                        propRep.setDefaultValue(prop.getDefaultValue());
                        propRep.setHelpText(prop.getHelpText());
                        rep.getProperties().add(propRep);
                    }
                    types.put(rep.getId(), rep);

                }
            }
        }
        return types;
    }

    @GET
    @Path("mappers")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<IdentityProviderMapperRepresentation> getMappers() {
        this.auth.requireView();
        List<IdentityProviderMapperRepresentation> mappers = new LinkedList<>();
        for (IdentityProviderMapperModel model : realm.getIdentityProviderMappersByAlias(identityProviderModel.getAlias())) {
            mappers.add(ModelToRepresentation.toRepresentation(model));
        }
        return mappers;
    }

    @POST
    @Path("mappers")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMapper(IdentityProviderMapperRepresentation mapper) {
        auth.requireManage();
        IdentityProviderMapperModel model = RepresentationToModel.toModel(mapper);
        model = realm.addIdentityProviderMapper(model);
        return Response.created(uriInfo.getAbsolutePathBuilder().path(model.getId()).build()).build();

    }

    @GET
    @NoCache
    @Path("mappers/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProviderMapperRepresentation getMapperById(@PathParam("id") String id) {
        auth.requireView();
        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        return ModelToRepresentation.toRepresentation(model);
    }

    @PUT
    @NoCache
    @Path("mappers/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void update(@PathParam("id") String id, IdentityProviderMapperRepresentation rep) {
        auth.requireManage();
        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        model = RepresentationToModel.toModel(rep);
        realm.updateIdentityProviderMapper(model);
    }

    @DELETE
    @NoCache
    @Path("mappers/{id}")
    public void delete(@PathParam("id") String id) {
        auth.requireManage();
        IdentityProviderMapperModel model = realm.getIdentityProviderMapperById(id);
        if (model == null) throw new NotFoundException("Model not found");
        realm.removeIdentityProviderMapper(model);
    }

    private void removeClientIdentityProviders(List<ClientModel> clients, IdentityProviderModel identityProvider) {
        for (ClientModel clientModel : clients) {
            List<ClientIdentityProviderMappingModel> identityProviders = clientModel.getIdentityProviders();

            for (ClientIdentityProviderMappingModel providerMappingModel : new ArrayList<>(identityProviders)) {
                if (providerMappingModel.getIdentityProvider().equals(identityProvider.getAlias())) {
                    identityProviders.remove(providerMappingModel);
                    clientModel.updateIdentityProviders(identityProviders);
                    break;
                }
            }
        }
    }

}
