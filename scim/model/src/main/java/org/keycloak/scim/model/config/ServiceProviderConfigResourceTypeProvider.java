package org.keycloak.scim.model.config;

import java.util.List;

import org.keycloak.common.util.Time;
import org.keycloak.scim.resource.config.ServiceProviderConfig;
import org.keycloak.scim.resource.config.ServiceProviderConfig.BulkSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.FilterSupport;
import org.keycloak.scim.resource.config.ServiceProviderConfig.Supported;
import org.keycloak.scim.resource.spi.SingletonResourceTypeProvider;

public class ServiceProviderConfigResourceTypeProvider implements SingletonResourceTypeProvider<ServiceProviderConfig> {

    @Override
    public ServiceProviderConfig getSingleton() {
        ServiceProviderConfig config = new ServiceProviderConfig();

        config.setId("");
        config.setBulk(new BulkSupport());
        config.setPatch(Supported.FALSE);
        config.setEtag(Supported.FALSE);
        config.setAuthenticationSchemes(List.of());
        config.setChangePassword(Supported.FALSE);
        config.setCreatedTimestamp(Time.currentTimeMillis());
        config.setSort(Supported.FALSE);
        config.setFilter(new FilterSupport());

        return config;
    }

    @Override
    public Class<ServiceProviderConfig> getResourceType() {
        return ServiceProviderConfig.class;
    }

    @Override
    public String getSchema() {
        return ServiceProviderConfig.SCHEMA;
    }
}
