package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class IdentityProviderResource {

    private final RealmAuth auth;
    private final RealmModel realm;
    private final KeycloakSession session;
    private final IdentityProviderModel identityProviderModel;

    public IdentityProviderResource(RealmAuth auth, RealmModel realm, KeycloakSession session, IdentityProviderModel identityProviderModel) {
        this.realm = realm;
        this.session = session;
        this.identityProviderModel = identityProviderModel;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public IdentityProviderRepresentation getIdentityProvider() {
        return ModelToRepresentation.toRepresentation(this.identityProviderModel);
    }

    @DELETE
    @NoCache
    public Response delete() {
        this.auth.requireManage();
        removeClientIdentityProviders(this.realm.getApplications(), this.identityProviderModel);
        removeClientIdentityProviders(this.realm.getApplications(), this.identityProviderModel);
        this.realm.removeIdentityProviderById(this.identityProviderModel.getId());
        return Response.noContent().build();
    }

    @PUT
    @Consumes("application/json")
    public Response update(IdentityProviderRepresentation model) {
        try {
            this.auth.requireManage();
            this.realm.updateIdentityProvider(RepresentationToModel.toModel(model));
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Identity Provider " + model.getId() + " already exists");
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
    public Response export(@Context UriInfo uriInfo, @QueryParam("format") String format) {
        try {
            this.auth.requireView();
            IdentityProviderFactory factory = getIdentityProviderFactory();
            return factory.create(identityProviderModel).export(uriInfo, realm, format);
        } catch (Exception e) {
            return Flows.errors().error("Could not export public broker configuration for identity provider [" + identityProviderModel.getProviderId() + "].", Response.Status.NOT_FOUND);
        }
    }


    private void removeClientIdentityProviders(List<? extends ClientModel> clients, IdentityProviderModel identityProvider) {
        for (ClientModel clientModel : clients) {
            List<ClientIdentityProviderMappingModel> identityProviders = clientModel.getIdentityProviders();

            for (ClientIdentityProviderMappingModel providerMappingModel : new ArrayList<ClientIdentityProviderMappingModel>(identityProviders)) {
                if (providerMappingModel.getIdentityProvider().equals(identityProvider.getId())) {
                    identityProviders.remove(providerMappingModel);
                }
            }

            clientModel.updateAllowedIdentityProviders(identityProviders);
        }
    }

}
