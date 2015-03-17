package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
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
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.social.SocialIdentityProvider;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @author Pedro Igor
 */
public class IdentityProvidersResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private RealmAuth auth;

    public IdentityProvidersResource(RealmModel realm, KeycloakSession session, RealmAuth auth) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.auth.init(RealmAuth.Resource.IDENTITY_PROVIDER);
    }

    @GET
    @NoCache
    @Produces("application/json")
    public List<IdentityProviderRepresentation> getIdentityProviders() {
        this.auth.requireView();

        List<IdentityProviderRepresentation> representations = new ArrayList<IdentityProviderRepresentation>();

        for (IdentityProviderModel identityProviderModel : realm.getIdentityProviders()) {
            representations.add(ModelToRepresentation.toRepresentation(identityProviderModel));
        }

        return representations;
    }

    @Path("/providers/{provider_id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getIdentityProviders(@PathParam("provider_id") String providerId) {
        this.auth.requireView();
        IdentityProviderFactory providerFactory = getProviderFactorytById(providerId);

        if (providerFactory != null) {
            return Response.ok(providerFactory).build();
        }

        return Response.status(BAD_REQUEST).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context UriInfo uriInfo, IdentityProviderRepresentation representation) {
        this.auth.requireManage();

        try {
            this.realm.addIdentityProvider(RepresentationToModel.toModel(representation));

            updateClientIdentityProviders(this.realm.getApplications(), representation);
            updateClientIdentityProviders(this.realm.getOAuthClients(), representation);

            return Response.created(uriInfo.getAbsolutePathBuilder().path(representation.getProviderId()).build()).build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Identity Provider " + representation.getId() + " already exists");
        }
    }

    @POST
    @Path("import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importFrom(@Context UriInfo uriInfo, MultipartFormDataInput input) throws IOException {
        this.auth.requireManage();
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        String id = formDataMap.get("id").get(0).getBodyAsString();
        String name = formDataMap.get("name").get(0).getBodyAsString();
        String providerId = formDataMap.get("providerId").get(0).getBodyAsString();
        String enabled = formDataMap.get("enabled").get(0).getBodyAsString();
        String updateProfileFirstLogin = formDataMap.get("updateProfileFirstLogin").get(0).getBodyAsString();
        String storeToken = "false";

        if (formDataMap.containsKey("storeToken")) {
            storeToken = formDataMap.get("storeToken").get(0).getBodyAsString();
        }

        InputPart file = formDataMap.get("file").get(0);
        InputStream inputStream = file.getBody(InputStream.class, null);
        IdentityProviderFactory providerFactory = getProviderFactorytById(providerId);
        Map config = providerFactory.parseConfig(inputStream);
        IdentityProviderRepresentation representation = new IdentityProviderRepresentation();

        representation.setId(id);
        representation.setName(name);
        representation.setProviderId(providerId);
        representation.setEnabled(Boolean.valueOf(enabled));
        representation.setUpdateProfileFirstLogin(Boolean.valueOf(updateProfileFirstLogin));
        representation.setStoreToken(Boolean.valueOf(storeToken));
        representation.setConfig(config);

        return create(uriInfo, representation);
    }

    @POST
    @Path("import")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response importFrom(@Context UriInfo uriInfo, Map<String, Object> data) throws IOException {
        this.auth.requireManage();

        String id = data.get("id").toString();
        String name = data.get("name").toString();
        String providerId = data.get("providerId").toString();
        String enabled = data.get("enabled").toString();
        String updateProfileFirstLogin = data.get("updateProfileFirstLogin").toString();
        String storeToken = "false";

        if (data.containsKey("storeToken")) {
            storeToken = data.get("storeToken").toString();
        }

        String from = data.get("fromUrl").toString();
        ApacheHttpClient4Executor executor = ResourceAdminManager.createExecutor();
        InputStream inputStream = null;
        try {
            inputStream = executor.createRequest(from).getTarget(InputStream.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        IdentityProviderFactory providerFactory = getProviderFactorytById(providerId);
        Map config = providerFactory.parseConfig(inputStream);
        IdentityProviderRepresentation representation = new IdentityProviderRepresentation();

        representation.setId(id);
        representation.setName(name);
        representation.setProviderId(providerId);
        representation.setEnabled(Boolean.valueOf(enabled));
        representation.setUpdateProfileFirstLogin(Boolean.valueOf(updateProfileFirstLogin));
        representation.setStoreToken(Boolean.valueOf(storeToken));
        representation.setConfig(config);

        return create(uriInfo, representation);
    }

    @Path("instances/{id}")
    public IdentityProviderResource getIdentityProvider(@PathParam("id") String providerId) {
        this.auth.requireView();
        IdentityProviderModel identityProviderModel = null;

        for (IdentityProviderModel storedIdentityProvider : this.realm.getIdentityProviders()) {
            if (storedIdentityProvider.getId().equals(providerId)
                    || storedIdentityProvider.getInternalId().equals(providerId)) {
                identityProviderModel = storedIdentityProvider;
            }
        }

        if (identityProviderModel == null) {
            throw new NotFoundException("Could not find identity provider: " + providerId);
        }

        IdentityProviderResource identityProviderResource = new IdentityProviderResource(this.auth, realm, session, identityProviderModel);
        ResteasyProviderFactory.getInstance().injectProperties(identityProviderResource);

        return identityProviderResource;
    }

    private IdentityProviderFactory getProviderFactorytById(String providerId) {
        List<ProviderFactory> allProviders = getProviderFactories();

        for (ProviderFactory providerFactory : allProviders) {
            if (providerFactory.getId().equals(providerId)) {
                return (IdentityProviderFactory) providerFactory;
            }
        }

        return null;
    }

    private List<ProviderFactory> getProviderFactories() {
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        return allProviders;
    }

    private void updateClientIdentityProviders(List<? extends ClientModel> clients, IdentityProviderRepresentation identityProvider) {
        for (ClientModel clientModel : clients) {
            List<ClientIdentityProviderMappingModel> allowedIdentityProviders = clientModel.getIdentityProviders();
            ClientIdentityProviderMappingModel providerMappingModel = new ClientIdentityProviderMappingModel();

            providerMappingModel.setIdentityProvider(identityProvider.getId());

            allowedIdentityProviders.add(providerMappingModel);

            clientModel.updateAllowedIdentityProviders(allowedIdentityProviders);
        }
    }
}
