package org.keycloak.testsuite.arquillian.provider;

import org.keycloak.testsuite.arquillian.SuiteContext;
import java.lang.annotation.Annotation;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 *
 * @author tkyjovsk
 */
public class SuiteContextProvider implements ResourceProvider {

    @Inject
    Instance<SuiteContext> suiteContext;

    @Override
    public boolean canProvide(Class<?> type) {
        return SuiteContext.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return suiteContext.get();
    }

}
