package org.keycloak.services.resources.admin;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.ui.extend.UiScriptProvider;
import org.keycloak.services.ui.extend.UiScriptProviderFactory;
import org.keycloak.services.ui.extend.UiScriptProviderUtils;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.utils.MediaType;

public class UiScriptsResource {

    private final KeycloakSession session;
    private final AdminPermissionEvaluator auth;

    public UiScriptsResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
    }

    @Path("{providerId}")
    public UiScriptProviderResource getProvider(@PathParam("providerId") String providerId) {
        ProviderFactory<?> factory = session.getKeycloakSessionFactory().getProviderFactory(UiScriptProvider.class, providerId);
        if (!(factory instanceof UiScriptProviderFactory<?> scriptFactory)) {
            throw new NotFoundException();
        }
        return new UiScriptProviderResource(auth, scriptFactory);
    }

    public static class UiScriptProviderResource {

        private final AdminPermissionEvaluator auth;
        private final UiScriptProviderFactory<?> factory;

        public UiScriptProviderResource(AdminPermissionEvaluator auth, UiScriptProviderFactory<?> factory) {
            this.auth = auth;
            this.factory = factory;
        }

        @GET
        @Path("script.js")
        @Produces(MediaType.TEXT_PLAIN_JAVASCRIPT)
        public Response getScript(@jakarta.ws.rs.HeaderParam(HttpHeaders.IF_NONE_MATCH) String etag) {
            auth.realm().requireViewRealm();

            String tagName = factory.getTagName();
            String scriptPath = factory.getScriptPath();
            UiScriptProviderUtils.validateTagName(tagName);
            UiScriptProviderUtils.validateScriptPath(scriptPath);

            try (var resource = factory.getClass().getClassLoader().getResourceAsStream(scriptPath)) {
                if (resource == null) {
                    throw new NotFoundException();
                }

                byte[] content = resource.readAllBytes();
                String contentEtag = "\"" + Integer.toHexString(java.util.Arrays.hashCode(content)) + "\"";

                if (etag != null && etag.equals(contentEtag)) {
                    return Response.notModified()
                            .header(HttpHeaders.ETAG, contentEtag)
                            .cacheControl(CacheControlUtil.getDefaultCacheControl())
                            .build();
                }

                return Response.ok(content)
                        .type(MediaType.TEXT_PLAIN_JAVASCRIPT)
                        .header(HttpHeaders.ETAG, contentEtag)
                        .cacheControl(CacheControlUtil.getDefaultCacheControl())
                        .build();
            } catch (IOException e) {
                throw new NotFoundException();
            }
        }
    }
}
