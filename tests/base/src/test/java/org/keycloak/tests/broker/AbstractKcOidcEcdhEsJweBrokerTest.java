package org.keycloak.tests.broker;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.representations.idm.ComponentRepresentation;

public abstract class AbstractKcOidcEcdhEsJweBrokerTest extends AbstractKcOidcJweBrokerTest {

    protected abstract String getCurve();

    @Override
    protected ComponentRepresentation getProviderKeyComponent() {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setName("ecdsa-generated");
        component.setProviderId("ecdsa-generated");
        component.setProviderType(KeyProvider.class.getName());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("ecdsaEllipticCurveKey", getCurve());
        component.setConfig(config);
        return component;
    }

    @Override
    protected ComponentRepresentation getConsumerKeyComponent() {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setName("ecdh-generated");
        component.setProviderId("ecdh-generated");
        component.setProviderType(KeyProvider.class.getName());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("ecdhAlgorithm", getEncAlg());
        config.putSingle("ecdhEllipticCurveKey", getCurve());
        component.setConfig(config);
        return component;
    }
}
