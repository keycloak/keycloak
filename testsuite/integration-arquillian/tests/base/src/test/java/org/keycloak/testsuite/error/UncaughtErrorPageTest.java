package org.keycloak.testsuite.error;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.pages.ErrorPage;

import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UncaughtErrorPageTest extends AbstractKeycloakTest {

    @Page
    private ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    public void invalidResource() throws MalformedURLException {
        checkPageNotFound("/auth/nosuch");
    }

    @Test
    public void invalidRealm() throws MalformedURLException {
        checkPageNotFound("/auth/realms/nosuch");
    }

    @Test
    public void invalidRealmResource() throws MalformedURLException {
        checkPageNotFound("/auth/realms/master/nosuch");
    }

    @Test
    public void uncaughtErrorJson() {
        Response response = testingClient.testing().uncaughtError();
        assertNull(response.getEntity());
        assertEquals(500, response.getStatus());
    }

    @Test
    public void uncaughtError() throws MalformedURLException {
        URI uri = suiteContext.getAuthServerInfo().getUriBuilder().path("/auth/realms/master/testing/uncaught-error").build();
        driver.navigate().to(uri.toURL());

        assertTrue(errorPage.isCurrent());
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    private void checkPageNotFound(String path) throws MalformedURLException {
        URI uri = suiteContext.getAuthServerInfo().getUriBuilder().path(path).build();
        driver.navigate().to(uri.toURL());

        assertTrue(errorPage.isCurrent());
        assertEquals("Page not found", errorPage.getError());
    }

}
