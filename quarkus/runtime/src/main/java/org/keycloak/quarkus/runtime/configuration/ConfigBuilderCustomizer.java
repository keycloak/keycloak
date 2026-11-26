package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class ConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {

    @Override
    public void configBuilder(SmallRyeConfigBuilder builder) {
        if (builder.getClassLoader() == Thread.currentThread().getContextClassLoader()) {
            // the Keycloak interceptors shouldn't be used when the classloader doesn't match
            // as our logic will implicitly create a config via Configuration.getConfig in effectively
            // the wrong classloader
            addInterceptors(builder); 
        }
    }

    static SmallRyeConfigBuilder addInterceptors(SmallRyeConfigBuilder builder) {
        return builder.withInterceptors(new PropertyMappingInterceptor(), new NestedPropertyMappingInterceptor());
    }
    
}
