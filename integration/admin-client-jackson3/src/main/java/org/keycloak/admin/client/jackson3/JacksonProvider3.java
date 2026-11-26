package org.keycloak.admin.client.jackson3;

import dev.resteasy.providers.jackson.ResteasyJacksonProvider;

public class JacksonProvider3 extends ResteasyJacksonProvider {

    public JacksonProvider3() {
        setMapper(Jackson3MapperHolder.MAPPER);
    }
}
