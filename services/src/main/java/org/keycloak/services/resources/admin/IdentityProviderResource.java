package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.social.SocialIdentityProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
public class IdentityProviderResource {

    private final RealmModel realm;
    private final KeycloakSession session;

    public IdentityProviderResource(RealmModel realm, KeycloakSession session) {
        this.realm = realm;
        this.session = session;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public List<IdentityProviderModel> getIdentityProviders() {
        return realm.getIdentityProviders();
    }

    @Path("/providers/{provider_id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getIdentityProviders(@PathParam("provider_id") String providerId) {
        IdentityProviderFactory providerFactory = getProviderFactorytById(providerId);

        if (providerFactory != null) {
            return Response.ok(providerFactory).build();
        }

        return Response.status(BAD_REQUEST).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Context UriInfo uriInfo, IdentityProviderModel providerModel) {
        realm.addIdentityProvider(providerModel);

        return Response.created(uriInfo.getAbsolutePathBuilder().path(providerModel.getProviderId()).build()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createWithFile(@Context UriInfo uriInfo, MultipartFormDataInput input) throws IOException {
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();

        String id = formDataMap.get("id").get(0).getBodyAsString();
        String name = formDataMap.get("name").get(0).getBodyAsString();
        String providerId = formDataMap.get("providerId").get(0).getBodyAsString();
        String enabled = formDataMap.get("enabled").get(0).getBodyAsString();
        String updateProfileFirstLogin = formDataMap.get("updateProfileFirstLogin").get(0).getBodyAsString();
        InputPart file = formDataMap.get("file").get(0);
        InputStream inputStream = file.getBody(InputStream.class, null);
        IdentityProviderFactory providerFactory = getProviderFactorytById(providerId);
        Map config = providerFactory.parseConfig(inputStream);
        IdentityProviderModel providerModel = new IdentityProviderModel();

        providerModel.setId(id);
        providerModel.setName(name);
        providerModel.setProviderId(providerId);
        providerModel.setEnabled(Boolean.valueOf(enabled));
        providerModel.setUpdateProfileFirstLogin(Boolean.valueOf(updateProfileFirstLogin));
        providerModel.setConfig(config);

        return create(uriInfo, providerModel);
    }

    @Path("{id}")
    @GET
    @NoCache
    @Produces("application/json")
    public Response getIdentityProvider(@PathParam("id") String providerId) {
        for (IdentityProviderModel identityProviderModel : this.realm.getIdentityProviders()) {
            if (identityProviderModel.getId().equals(providerId)) {
                return Response.ok(identityProviderModel).build();
            }
        }

        return Response.noContent().build();
    }

    @Path("{id}")
    @DELETE
    @NoCache
    public Response delete(@PathParam("id") String providerId) {
        this.realm.removeIdentityProviderById(providerId);
        return Response.noContent().build();
    }

    @PUT
    @Consumes("application/json")
    public Response update(IdentityProviderModel model) {
        this.realm.updateIdentityProvider(model);
        return Response.noContent().build();
    }

    private IdentityProviderFactory getProviderFactorytById(String providerId) {
        List<ProviderFactory> allProviders = new ArrayList<ProviderFactory>();

        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(IdentityProvider.class));
        allProviders.addAll(this.session.getKeycloakSessionFactory().getProviderFactories(SocialIdentityProvider.class));

        for (ProviderFactory providerFactory : allProviders) {
            if (providerFactory.getId().equals(providerId)) {
                return (IdentityProviderFactory) providerFactory;
            }
        }

        return null;
    }
}
