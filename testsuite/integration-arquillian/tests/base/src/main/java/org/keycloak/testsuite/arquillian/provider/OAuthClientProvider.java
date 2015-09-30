package org.keycloak.testsuite.arquillian.provider;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.keycloak.testsuite.util.OAuthClient;

import java.lang.annotation.Annotation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClientProvider implements ResourceProvider {

    @Inject
    Instance<OAuthClient> oauthClient;

    @Override
    public boolean canProvide(Class<?> type) {
        return OAuthClient.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return oauthClient.get();
    }

}
