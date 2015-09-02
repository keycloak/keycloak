package org.keycloak.testsuite.arquillian.provider;

import org.keycloak.testsuite.arquillian.TestContext;
import java.lang.annotation.Annotation;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 *
 * @author tkyjovsk
 */
public class TestContextProvider implements ResourceProvider {

    @Inject
    Instance<TestContext> testContext;

    @Override
    public boolean canProvide(Class<?> type) {
        return TestContext.class.isAssignableFrom(type);
    }

    @Override
    @ClassInjection
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return testContext.get();
    }

}
