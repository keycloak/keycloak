package org.keycloak.testsuite.arquillian.provider;

import java.lang.annotation.Annotation;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.keycloak.admin.client.Keycloak;

/**
 *
 * @author tkyjovsk
 */
public class AdminClientProvider implements ResourceProvider {

    @Inject
    Instance<Keycloak> adminClient;
    
    @Override
    public boolean canProvide(Class<?> type) {
        return Keycloak.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return adminClient.get();
    }
    
}
