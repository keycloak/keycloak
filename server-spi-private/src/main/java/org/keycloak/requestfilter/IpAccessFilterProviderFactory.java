package org.keycloak.requestfilter;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;
import java.util.Optional;

public class IpAccessFilterProviderFactory implements RequestFilterProviderFactory {

    public static final String PROVIDER_ID = "ip-access";

    @Override
    public IpAccessFilterProvider create(KeycloakSession session) {
        return new IpAccessFilterProvider();
    }

    private boolean isEnabled = false;

    @Override
    public void init(Config.Scope config) {
        this.isEnabled = Optional.ofNullable(config.getBoolean("enabled")).orElse(false);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (isEnabled) {
            ResteasyProviderFactory.getInstance().getContainerRequestFilterRegistry()
                    .registerClass(IpAccessFilter.class);
        }
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("enabled")
                .type("boolean")
                .helpText("Enable the filter")
                .defaultValue(false)
                .add()
                .property()
                .name(IpAccessFilter.CFG_ALLOW)
                .type("String")
                .helpText("Configures the allow filter, format: `<PATH>:<CIDR>[|<CIDR>][,<PATH>:<CIDR>[|<CIDR>]]` , e.g.: `/admin/foo:127.0.0.1/24|192.168.80.1/16,/admin/master:127.0.0.1/24`. Note that allow takes precedence over deny configuration.")
                .defaultValue("none")
                .add()
                .property()
                .name(IpAccessFilter.CFG_DENY)
                .type("String")
                .helpText("Configures the deny filter, format: `<PATH>:<CIDR>[|<CIDR>][,<PATH>:<CIDR>[|<CIDR>]]` , e.g.: `/admin/foo:127.0.0.1/24|192.168.80.1/16,/admin/master:127.0.0.1/24`. Note that allow takes precedence over deny configuration.")
                .defaultValue("none")
                .add()
                .build();
        }

    }
