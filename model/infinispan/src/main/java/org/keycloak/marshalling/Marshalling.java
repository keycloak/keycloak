package org.keycloak.marshalling;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.keycloak.models.sessions.infinispan.changes.ReplaceFunction;

@SuppressWarnings("removal")
public final class Marshalling {

    private Marshalling() {
    }

    // Note: Min ID is 2500
    public static final Integer REPLACE_FUNCTION_ID = 2500;

    // For Infinispan 10, we go with the JBoss marshalling.
    // TODO: This should be replaced later with the marshalling recommended by infinispan. Probably protostream.
    // See https://infinispan.org/docs/stable/titles/developing/developing.html#marshalling for the details
    public static void configure(GlobalConfigurationBuilder builder) {
        builder.serialization()
                .marshaller(new JBossUserMarshaller())
                .addAdvancedExternalizer(ReplaceFunction.INSTANCE);
    }

}
