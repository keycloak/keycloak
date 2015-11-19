package org.keycloak.testsuite.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientInitialAccessResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InitialAccessTokenTest extends AbstractClientRegistrationTest {

    private ClientInitialAccessResource resource;

    @Before
    public void before() throws Exception {
        super.before();

        resource = adminClient.realm(REALM_NAME).clientInitialAccess();
    }

    @Test
    public void create() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation());

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        Thread.sleep(2);

        ClientRepresentation created = reg.create(rep);
        Assert.assertNotNull(created);

        try {
            reg.create(rep);
        } catch (ClientRegistrationException e) {
            Assert.assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createMultiple() throws ClientRegistrationException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation(0, 2));

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        ClientRepresentation created = reg.create(rep);
        Assert.assertNotNull(created);

        created = reg.create(rep);
        Assert.assertNotNull(created);

        try {
            reg.create(rep);
        } catch (ClientRegistrationException e) {
            Assert.assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createExpired() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation(1, 1));

        reg.auth(Auth.token(response));

        ClientRepresentation rep = new ClientRepresentation();

        Thread.sleep(2);

        try {
            reg.create(rep);
        } catch (ClientRegistrationException e) {
            Assert.assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

    @Test
    public void createDeleted() throws ClientRegistrationException, InterruptedException {
        ClientInitialAccessPresentation response = resource.create(new ClientInitialAccessCreatePresentation());

        reg.auth(Auth.token(response));

        resource.delete(response.getId());

        ClientRepresentation rep = new ClientRepresentation();

        try {
            reg.create(rep);
        } catch (ClientRegistrationException e) {
            Assert.assertEquals(403, ((HttpErrorException) e.getCause()).getStatusLine().getStatusCode());
        }
    }

}
