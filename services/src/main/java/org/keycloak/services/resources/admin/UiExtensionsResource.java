package org.keycloak.services.resources.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.component.ComponentFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.ui.extend.UiExtensionSupport;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiTabProvider;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.NoCache;

@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class UiExtensionsResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public UiExtensionsResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }

    @GET
    @Path("tabs/{providerId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation(summary = "Returns runtime configuration properties for a declarative UI tab")
    public List<ConfigPropertyRepresentation> getTabConfig(
            @PathParam("providerId") String providerId,
            @QueryParam("clientId") String clientId,
            @QueryParam("id") String id,
            @QueryParam("alias") String alias) {
        UiExtensionSupport factory = getFactory(UiTabProvider.class, providerId);
        UiExtensionPermissions.requireView(auth, factory);
        return toRepresentation(factory.getConfigProperties(session, contextParams(clientId, id, alias)));
    }

    @GET
    @Path("pages/{providerId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation(summary = "Returns runtime configuration properties for a declarative UI page")
    public List<ConfigPropertyRepresentation> getPageConfig(
            @PathParam("providerId") String providerId,
            @QueryParam("componentId") String componentId) {
        UiExtensionSupport factory = getFactory(UiPageProvider.class, providerId);
        UiExtensionPermissions.requireView(auth, factory);
        Map<String, String> params = new HashMap<>();
        if (componentId != null) {
            params.put("componentId", componentId);
        }
        return toRepresentation(factory.getConfigProperties(session, params));
    }

    private <T extends Provider> UiExtensionSupport getFactory(Class<T> providerClass, String providerId) {
        ProviderFactory<T> factory = session.getKeycloakSessionFactory().getProviderFactory(providerClass, providerId);
        if (!(factory instanceof UiExtensionSupport extensionSupport)) {
            throw new NotFoundException("Could not find UI extension provider");
        }
        if (factory instanceof ComponentFactory<?, ?> componentFactory && componentFactory.isInternal()) {
            throw new NotFoundException("Could not find UI extension provider");
        }
        return extensionSupport;
    }

    private Map<String, String> contextParams(String clientId, String id, String alias) {
        Map<String, String> params = new HashMap<>();
        if (clientId != null) {
            params.put("clientId", clientId);
        }
        if (id != null) {
            params.put("id", id);
        }
        if (alias != null) {
            params.put("alias", alias);
        }
        return params;
    }

    private List<ConfigPropertyRepresentation> toRepresentation(List<ProviderConfigProperty> properties) {
        return ModelToRepresentation.toRepresentation(properties);
    }
}
