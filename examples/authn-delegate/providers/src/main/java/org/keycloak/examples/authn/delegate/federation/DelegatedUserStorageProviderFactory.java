package org.keycloak.examples.authn.delegate.federation;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

public class DelegatedUserStorageProviderFactory implements UserStorageProviderFactory<DelegatedUserStorageProvider>  {
    // TODO: if considering internationalization, need to move text onto message file for each locale.
    
    public static final String PROVIDER_NAME = "readonly-delegated-server";
 
    protected static final List<ProviderConfigProperty> configProperties  = new ArrayList<ProviderConfigProperty>();
    protected static final List<ProviderConfigProperty> configMetadata;

    public static final String AS_USERINFO_URI = "userstorage.external.authentication.server.userinfo.uri";
    public static final String IS_BACKEND_COMM_SSL_REQUIRED = "userstorage.external.authentication.backend.communication.ssl.required";
    
    static {
        configMetadata = ProviderConfigurationBuilder.create()
                .property().name(AS_USERINFO_URI)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("External Authentication Server User Information URI")
                //.defaultValue("${jboss.server.config.dir}/example-users.properties")
                .helpText("Endpoint for obtaining User Information by User ID.")
                .add()                
                .property().name(IS_BACKEND_COMM_SSL_REQUIRED)
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .label("TLS Required for Back End Connmunication")
                .defaultValue(true)
                .helpText("If true, Back End Commnication between keycloak and External Authentication Server requires TLS connections.")
                .add()                
                .build();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configMetadata;
    }
    
    @Override
    public String getId() {
        // Auto-generated method stub
        return PROVIDER_NAME;
    }


    @Override
    public void init(Config.Scope config) {
        // Auto-generated method stub
    }

    @Override
    public DelegatedUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        // Auto-generated method stub
        return new DelegatedUserStorageProvider(session, model);
    }
}
