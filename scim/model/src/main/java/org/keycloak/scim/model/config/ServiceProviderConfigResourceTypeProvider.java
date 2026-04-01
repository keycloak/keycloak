package org.keycloak.scim.model.config;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.scim.protocol.ForbiddenException;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.config.ServiceProviderConfig;
import org.keycloak.scim.resource.config.ServiceProviderConfig.AuthenticationScheme;
import org.keycloak.scim.resource.config.ServiceProviderConfig.BulkSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.FilterSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.Supported;
import org.keycloak.scim.resource.schema.ModelSchema;
import org.keycloak.scim.resource.spi.SingletonResourceTypeProvider;

public class ServiceProviderConfigResourceTypeProvider implements SingletonResourceTypeProvider<ServiceProviderConfig> {

    private final KeycloakSession session;

    public ServiceProviderConfigResourceTypeProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ServiceProviderConfig getSingleton() {
        ServiceProviderConfig config = new ServiceProviderConfig();

        config.setId("");
        config.setBulk(new BulkSupport());
        config.setPatch(Supported.TRUE);
        config.setEtag(Supported.FALSE);
        config.setAuthenticationSchemes(List.of());
        config.setChangePassword(Supported.FALSE);
        config.setCreatedTimestamp(Time.currentTimeMillis());
        config.setSort(Supported.FALSE);
        config.setFilter(getFilterSupport());
        config.setAuthenticationSchemes(getAuthenticationSchemes());

        return config;
    }

    private FilterSupport getFilterSupport() {
        FilterSupport filter = new FilterSupport();

        filter.setSupported(true);

        return filter;
    }

    private List<AuthenticationScheme> getAuthenticationSchemes() {
        AuthenticationScheme scheme = new AuthenticationScheme();

        scheme.setName("OAuth Bearer Token");
        scheme.setDescription("Authentication scheme using the OAuth Bearer Token standard");
        scheme.setSpecUri("https://tools.ietf.org/html/rfc6750");
        scheme.setType("oauthbearertoken");

        return List.of(scheme);
    }

    @Override
    public Stream<ServiceProviderConfig> getAll(SearchRequest searchRequest) {
        if (!session.getContext().getPermissions().hasPermission(AdminPermissionsSchema.REALMS_RESOURCE_TYPE, AdminPermissionsSchema.VIEW)) {
            throw new ForbiddenException();
        }
        return Stream.of(getSingleton());
    }

    @Override
    public Class<ServiceProviderConfig> getResourceType() {
        return ServiceProviderConfig.class;
    }

    @Override
    public String getSchema() {
        return ServiceProviderConfig.SCHEMA;
    }

    @Override
    public <M extends Model> List<ModelSchema<M, ServiceProviderConfig>> getSchemas() {
        return List.of();
    }
}
