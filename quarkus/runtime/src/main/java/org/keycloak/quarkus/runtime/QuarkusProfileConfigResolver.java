package org.keycloak.quarkus.runtime;

import org.keycloak.common.CommaSeparatedListProfileConfigResolver;
import org.keycloak.quarkus.runtime.configuration.Configuration;

public class QuarkusProfileConfigResolver extends CommaSeparatedListProfileConfigResolver {

    public QuarkusProfileConfigResolver() {
        super(Configuration.getRawValue("kc.features"), Configuration.getRawValue("kc.features-disabled"));
    }

}
