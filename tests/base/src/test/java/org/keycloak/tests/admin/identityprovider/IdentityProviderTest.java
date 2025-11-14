package org.keycloak.tests.admin.identityprovider;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.utils.Assert;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

@KeycloakIntegrationTest
public class IdentityProviderTest extends AbstractIdentityProviderTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void testFind() {
        create(createRep("twitter", "twitter idp","twitter", true, Collections.singletonMap("key1", "value1")));
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        create(createRep("google", "google"));
        create(createRep("github", "github"));
        create(createRep("facebook", "facebook"));

        Assert.assertNames(managedRealm.admin().identityProviders().findAll(), "facebook", "github", "google", "linkedin-openid-connect", "twitter");

        Assert.assertNames(managedRealm.admin().identityProviders().find(null, true, 0, 2), "facebook", "github");
        Assert.assertNames(managedRealm.admin().identityProviders().find(null, true, 2, 2), "google", "linkedin-openid-connect");
        Assert.assertNames(managedRealm.admin().identityProviders().find(null, true, 4, 2), "twitter");

        Assert.assertNames(managedRealm.admin().identityProviders().find("g", true, 0, 5), "github", "google");

        Assert.assertNames(managedRealm.admin().identityProviders().find("g*", true, 0, 5), "github", "google");
        Assert.assertNames(managedRealm.admin().identityProviders().find("g*", true, 0, 1), "github");
        Assert.assertNames(managedRealm.admin().identityProviders().find("g*", true, 1, 1), "google");

        Assert.assertNames(managedRealm.admin().identityProviders().find("*oo*", true, 0, 5), "google", "facebook");

        //based on display name search
        Assert.assertNames(managedRealm.admin().identityProviders().find("*ter i*", true, 0, 5), "twitter");

        List<IdentityProviderRepresentation> results = managedRealm.admin().identityProviders().find("\"twitter\"", true, 0, 5);
        Assert.assertNames(results, "twitter");
        Assertions.assertTrue(results.iterator().next().getConfig().isEmpty(), "Result is not in brief representation");
        results = managedRealm.admin().identityProviders().find("\"twitter\"", null, 0, 5);
        Assert.assertNames(results, "twitter");
        Assertions.assertFalse(results.iterator().next().getConfig().isEmpty(), "Config should be present in full representation");
    }

    @Test
    public void testFindForLoginPreservesOrderByAlias() {

        create(createRep("twitter", "twitter"));
        create(createRep("linkedin-openid-connect", "linkedin-openid-connect"));
        create(createRep("google", "google"));
        create(createRep("github", "github"));
        create(createRep("facebook", "facebook"));
        create(createRep("stackoverflow", "stackoverflow"));
        create(createRep("openshift-v4", "openshift-v4"));

        runOnServer.run(session -> {
            // fetch the list of idps available for login (should match all from above list) and ensure they come ordered by alias.
            List<String> aliases = session.identityProviders().getForLogin(IdentityProviderStorageProvider.FetchMode.ALL, null)
                    .map(IdentityProviderModel::getAlias).toList();
            assertThat(aliases, contains("facebook", "github", "google", "linkedin-openid-connect", "openshift-v4", "stackoverflow", "twitter"));
        });
    }

    @Test
    public void testInstalledIdentityProviders() {
        Response response = managedRealm.admin().identityProviders().getIdentityProviders("oidc");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        Map<String, String> body = response.readEntity(Map.class);
        assertProviderInfo(body, "oidc", "OpenID Connect v1.0");

        response = managedRealm.admin().identityProviders().getIdentityProviders("keycloak-oidc");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "keycloak-oidc", "Keycloak OpenID Connect");

        response = managedRealm.admin().identityProviders().getIdentityProviders("saml");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "saml", "SAML v2.0");

        response = managedRealm.admin().identityProviders().getIdentityProviders("google");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "google", "Google");

        response = managedRealm.admin().identityProviders().getIdentityProviders("facebook");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "facebook", "Facebook");

        response = managedRealm.admin().identityProviders().getIdentityProviders("github");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "github", "GitHub");

        response = managedRealm.admin().identityProviders().getIdentityProviders("twitter");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "twitter", "Twitter");

        response = managedRealm.admin().identityProviders().getIdentityProviders("linkedin-openid-connect");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "linkedin-openid-connect", "LinkedIn");

        response = managedRealm.admin().identityProviders().getIdentityProviders("microsoft");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "microsoft", "Microsoft");

        response = managedRealm.admin().identityProviders().getIdentityProviders("stackoverflow");
        Assertions.assertEquals(200, response.getStatus(), "Status");
        body = response.readEntity(Map.class);
        assertProviderInfo(body, "stackoverflow", "StackOverflow");

        response = managedRealm.admin().identityProviders().getIdentityProviders("nonexistent");
        Assertions.assertEquals(400, response.getStatus(), "Status");
    }
}
