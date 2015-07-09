package org.keycloak.testsuite.adapter.relative;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;

/**
 *
 * @author tkyjovsk
 */
public class RelativeServletsAdapterTest extends AbstractServletsAdapterTest {

    // FIXME - The 'app.server.base.url' property provided to auth server via arquillian.xml 
    // is pre-set for non-relative scenario. Need to inject correct property to auth server for relative scenario.
    @Override
    @Test
    @Ignore
    public void testSavedPostRequest() throws InterruptedException {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

    @Override
    @Test
    @Ignore
    public void testLoginSSOAndLogout() {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

    @Override
    @Test
    @Ignore
    public void testServletRequestLogout() {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

    @Override
    @Test
    @Ignore
    public void testLoginSSOIdle() {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

    @Override
    @Test
    @Ignore
    public void testLoginSSOIdleRemoveExpiredUserSessions() {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

    @Override
    @Test
    @Ignore
    public void testLoginSSOMax() throws InterruptedException {
        throw new UnsupportedOperationException("doesn't work with relative scenario yet");
    }

}
