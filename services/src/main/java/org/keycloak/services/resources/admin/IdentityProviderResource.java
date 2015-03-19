package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pedro Igor
 */
public class IdentityProviderResource {

    private static Logger logger = Logger.getLogger(IdentityProviderResource.class);

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
        removeClientIdentityProviders(this.realm.getOAuthClients(), this.identityProviderModel);
        this.realm.removeIdentityProviderByAlias(this.identityProviderModel.getAlias());
        return Response.noContent().build();
    }

    @PUT
    @Consumes("application/json")
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

                updateClientsAfterProviderAliasChange(this.realm.getApplications(), oldProviderId, newProviderId);
                updateClientsAfterProviderAliasChange(this.realm.getOAuthClients(), oldProviderId, newProviderId);
                updateUsersAfterProviderAliasChange(this.session.users().getUsers(this.realm), oldProviderId, newProviderId);
            }

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Identity Provider " + providerRep.getAlias() + " already exists");
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

    private void updateClientsAfterProviderAliasChange(List<? extends ClientModel> clients, String oldProviderId, String newProviderId) {
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
                if (providerMappingModel.getIdentityProvider().equals(identityProvider.getAlias())) {
                    identityProviders.remove(providerMappingModel);
                    clientModel.updateIdentityProviders(identityProviders);
                    break;
                }
            }
        }
    }

}
