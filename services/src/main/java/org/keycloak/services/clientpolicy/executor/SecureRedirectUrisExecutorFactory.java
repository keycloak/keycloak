package org.keycloak.services.clientpolicy.executor;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class SecureRedirectUrisExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-redirect-uris";
    public static final String ALLOW_PRIVATE_USE_SCHEMA = "allow-private-use-schema";
    public static final String ALLOW_LOOPBACK_INTERFACE = "allow-loopback-interface";
    public static final String ALLOW_HTTP = "allow-http";
    public static final String ALLOW_WILDCARD_CONTEXT_PATH = "allow-wildcard-context-path";
    public static final String ALLOW_OPEN = "allow-open";
    public static final String PERMITTED_DOMAINS = "permitted-domains";

    @Override
    public String getHelpText() {
        return "Check the configured redirect-uris for a client when created or updated";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureRedirectUrisExecutor();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()

            .property()
            .name(ALLOW_PRIVATE_USE_SCHEMA)
            .label("Allow private-use schema redirection")
            .helpText("If ON, then it will allow private-use URI schemes. For example, " +
                "an app that controls the domain name 'app.example.com' " +
                "can use 'com.example.app' as their scheme.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_LOOPBACK_INTERFACE)
            .label("Allow loopback interface redirection")
            .helpText("If ON, then it will allow loopback network interface. For example, " +
                "'http://127.0.0.1:{port}/{path}' for IPv4, and " +
                "'http://[::1]:{port}/{path}' for IPv6")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_PRIVATE_USE_SCHEMA)
            .label("Allow private-use schema")
            .helpText("If ON, then it will allow private-use URI schemes. For example, " +
                "an app that controls the domain name \"app.example.com\" " +
                "can use \"com.example.app\" as their scheme.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(true)
            .add()

            .property()
            .name(ALLOW_HTTP)
            .label("Allow http schema redirection")
            .helpText("If ON, then it will allow http uris.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_WILDCARD_CONTEXT_PATH)
            .label("Allow wildcard in context-path")
            .helpText("If ON, then it will allow wildcard in context-path uris. For example, " +
                "domain.example.com/*")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(true)
            .add()

            .property()
            .name(ALLOW_OPEN)
            .label("Allow open redirection")
            .helpText("If ON, then it will allow open uris. " +
                "It is only available in developer environments.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(PERMITTED_DOMAINS)
            .label("Allow permitted domains")
            .helpText("If ON, it will allow wildcard in hostname and " +
                "check the uri matches one of these permitted domains")
            .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
            .add()

            .build();
    }
}
