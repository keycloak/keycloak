package org.keycloak.testsuite.authz;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.cache.infinispan.authorization.PermissionCacheManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.PermissionRequest;
import org.keycloak.representations.idm.authorization.PermissionResponse;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.authorization.TestingCachedAuthorizationProvider;
import org.keycloak.testsuite.authorization.TestingCachedAuthorizationProviderFactory;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.OAuthClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.OperationException;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CachedAuthorizationProviderTest extends AbstractResourceServerTest {

    private ResourceRepresentation resourceA;
    private ResourcePermissionRepresentation permissionA;
    private AuthorizationResource authorization;
    private static final String ALLOW_ALL = "Default Policy";
    private static final String DENY_ALL = "Deny Policy";

    @Deployment
    public static WebArchive deploy() {
        MavenResolverSystem resolver = Maven.resolver();
        MavenFormatStage dependencies = resolver
                .loadPomFromFile("pom.xml")
                .importTestDependencies()
                .resolve("org.wildfly.extras.creaper:creaper-commands").withTransitivity();

        return RunOnServerDeployment.create(CachedAuthorizationProviderTest.class,
                AbstractResourceServerTest.class,
                AbstractAuthzTest.class,
                AuthorizationDeniedException.class).addAsLibraries(dependencies.asFile());
    }

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    protected OAuthClient oauth;

    @Before
    public void enableCachedProvider() throws Exception {
        setAuthorizationProviderSpi("cached");
    }

    @Before
    public void configureAuthorization() throws Exception {
        ClientResource client = getClient(getRealm());
        authorization = client.authorization();

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName(ALLOW_ALL);
        policy.setCode("$evaluation.grant();");

        Response response = authorization.policies().js().create(policy);
        response.close();

        permissionA = new ResourcePermissionRepresentation();
        resourceA = addResource("Resource A", "ScopeA", "ScopeB", "ScopeC");

        permissionA.setName(resourceA.getName() + " Permission");
        permissionA.addResource(resourceA.getName());
        permissionA.addPolicy(policy.getName());

        response = authorization.permissions().resource().create(permissionA);
        response.close();

        policy = new JSPolicyRepresentation();

        policy.setName(DENY_ALL);
        policy.setCode("$evaluation.deny();");

        response = authorization.policies().js().create(policy);
        response.close();
    }

    @After
    public void cleanTestState() throws Exception {
        setAuthorizationProviderSpi("default");
    }

    private void setAuthorizationProviderSpi(String name) throws Exception {
        if (Boolean.getBoolean("auth.server.undertow")) {
            controller.stop(suiteContext.getAuthServerInfo().getQualifier());
            System.setProperty("keycloak.authorizationProvider.provider", name);
            controller.start(suiteContext.getAuthServerInfo().getQualifier());
        } else {
            OnlineManagementClient client = AuthServerTestEnricher.getManagementClient();
            Administration administration = new Administration(client);
            Operations operations = new Operations(client);

            try {
                if (operations.exists(Address.subsystem("keycloak-server").and("spi", "authorization"))) {
                    client.execute("/subsystem=keycloak-server/spi=authorization/:remove");
                }

                client.execute("/subsystem=keycloak-server/spi=authorization/:add(default-provider=" + name + ")");
                administration.reload();
            } catch (IOException | TimeoutException | InterruptedException | OperationException | CliException e) {
                throw new RuntimeException(e);
            } finally {
                client.close();
            }
        }

        testingClient.server().run(session -> {
            TestingCachedAuthorizationProviderFactory.permissionCacheManager = null;
        }); // clear cache
        TestingCachedAuthorizationProviderFactory.permissionCacheManager = null; // Clear static cache
        reconnectAdminClient();
    }

    private void setPolicy(String name) {
        authorization = getClient(adminClient.realm(REALM_NAME)).authorization();
        permissionA = authorization.permissions().resource().findByName(permissionA.getName());

        permissionA.setPolicies(Collections.singleton(name));
        authorization.permissions().resource().findById(permissionA.getId()).update(permissionA);
    }

    @Test
    public void testInvalidationsCalledWithAccessToken() throws Exception {
        setAuthorizationProviderSpi("testing_cached");

        testingClient.server().run(session -> {
            TestingCachedAuthorizationProvider.wasCleared = false;
            TestingCachedAuthorizationProviderFactory.wasCleared = false;
        });

        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));

        setPolicy(DENY_ALL); // Invalidations should be called

        testingClient.server().run(session -> {
            assertThat(TestingCachedAuthorizationProvider.wasCleared).isTrue(); // AuthzProvider should be called
            assertThat(TestingCachedAuthorizationProviderFactory.wasCleared).isFalse();
            // AuthzProviderFactory should not be called because events serve only for invalidations in cluster
        });
    }

    @Test
    public void testCacheWithScopes() throws Exception {
        setAuthorizationProviderSpi("testing_cached");

        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        JWSInput jws = new JWSInput(accessTokenResponse.getToken());
        String tokenID = jws.readJsonContent(AccessToken.class).getId();

        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));

        Collection<Permission> permissions = toAccessToken(response.getToken()).getAuthorization().getPermissions();
        assertPermissions(permissions, resourceA.getName(), "ScopeA", "ScopeB");

        String resourceId = resourceA.getId();
        String resourceName = resourceA.getName();

        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();

            List<Permission> cacheList = cacheManager.get(tokenID);
            assertThat(cacheList)
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Stream.of("ScopeA", "ScopeB").collect(Collectors.toSet()), Collections.emptyMap()));

            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Stream.of("ScopeA", "ScopeB").collect(Collectors.toSet()), Collections.emptyMap()));
        });

        response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeC"));

        permissions = toAccessToken(response.getToken()).getAuthorization().getPermissions();
        assertPermissions(permissions, resourceA.getName(), "ScopeC");

        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();

            List<Permission> cacheList = cacheManager.get(tokenID);
            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeC"), Collections.emptyMap()));


        });
    }

    @Test
    public void testMoreEntries() throws Exception {
        setAuthorizationProviderSpi("testing_cached");

        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        JWSInput jws = new JWSInput(accessTokenResponse.getToken());
        String tokenID = jws.readJsonContent(AccessToken.class).getId();

        AuthorizationResponse response = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA"));
        Collection<Permission> permissions = toAccessToken(response.getToken()).getAuthorization().getPermissions();
        assertPermissions(permissions, resourceA.getName(), "ScopeA");

        String resourceId = resourceA.getId();
        String resourceName = resourceA.getName();

        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();
            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeA"), Collections.emptyMap()));
        });

        accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        jws = new JWSInput(accessTokenResponse.getToken());
        String tokenID2 = jws.readJsonContent(AccessToken.class).getId();

        assertThat(tokenID).isNotEqualTo(tokenID2);

        AuthorizationResponse response2 = authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A","ScopeB"));
        permissions = toAccessToken(response2.getToken()).getAuthorization().getPermissions();
        assertPermissions(permissions, resourceA.getName(), "ScopeB");


        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();
            assertThat(cacheManager.containsKey(tokenID2)).isTrue();

            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeA"), Collections.emptyMap()));
            assertThat(cacheManager.get(tokenID2))
                    .extracting("resourceId", "resourceName", "scopes", "claims")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeB"), Collections.emptyMap()));
        });
    }

    @Test
    public void testCacheWithClaims() throws Exception {
        setAuthorizationProviderSpi("testing_cached");

        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");

        JWSInput jws = new JWSInput(accessTokenResponse.getToken());
        String tokenID = jws.readJsonContent(AccessToken.class).getId();

        AuthzClient authzClient = getAuthzClient();
        PermissionRequest permissionRequest = new PermissionRequest(resourceA.getId());

        permissionRequest.addScope("ScopeA");
        permissionRequest.setClaim("my.bank.account.withdraw.value", "50.5");

        PermissionResponse response = authzClient.protection("marta", "password").permission().create(permissionRequest);

        AuthorizationRequest request = new AuthorizationRequest();

        request.setTicket(response.getTicket());
        request.setClaimToken(accessTokenResponse.getToken());

        authzClient.authorization().authorize(request);

        String resourceId = resourceA.getId();
        String resourceName = resourceA.getName();

        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();

            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeA")));

            Map<String, Set<String>> claims = cacheManager.get(tokenID).get(0).getClaims();
            Set<String> value = claims.get("my.bank.account.withdraw.value");

            assertThat(value).containsExactly("50.5");
        });

        permissionRequest.setClaim("my.bank.account.withdraw.value", "100");

        response = authzClient.protection("marta", "password").permission().create(permissionRequest);

        request = new AuthorizationRequest();

        request.setTicket(response.getTicket());
        request.setClaimToken(accessTokenResponse.getToken());

        authzClient.authorization().authorize(request);

        testingClient.server().run(session -> {
            PermissionCacheManager cacheManager = TestingCachedAuthorizationProviderFactory.permissionCacheManager;
            assertThat(cacheManager.containsKey(tokenID)).isTrue();

            assertThat(cacheManager.get(tokenID))
                    .extracting("resourceId", "resourceName", "scopes")
                    .containsExactly(tuple(resourceId, resourceName, Collections.singleton("ScopeA")));

            Map<String, Set<String>> claims = cacheManager.get(tokenID).get(0).getClaims();
            Set<String> value = claims.get("my.bank.account.withdraw.value");

            assertThat(value).containsExactly("100");
        });
    }

    @Override
    public void assertPermissions(Collection<Permission> permissions, String expectedResource, String... expectedScopes) {
        Iterator<Permission> iterator = permissions.iterator();

        while (iterator.hasNext()) {
            Permission permission = iterator.next();

            if (permission.getResourceName().equalsIgnoreCase(expectedResource)) {
                Set<String> scopes = permission.getScopes();

                assertEquals(expectedScopes.length, scopes.size());

                if (scopes.containsAll(Arrays.asList(expectedScopes))) {
                    iterator.remove();
                }
            }
        }
    }

    @Test
    public void testInvalidationsWithAccessToken() throws Exception {
        AccessTokenResponse accessTokenResponse = getAuthzClient().obtainAccessToken("marta", "password");
        authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));

        setPolicy(DENY_ALL);

        try {
            authorize(null, null, null, null, accessTokenResponse.getToken(), null, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));

            fail(); // should fail as cache should be invalidated
        } catch (AuthorizationDeniedException ignore) {
        }
    }

    @Test
    public void testInvalidationsWithIdToken() throws Exception {
        String idToken = getIdToken("marta", "password");
        authorize(null, null, null, null, null, idToken, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));

        setPolicy(DENY_ALL);

        try {
            authorize(null, null, null, null,null, idToken, null, new PermissionRequest("Resource A", "ScopeA", "ScopeB"));
            fail(); // should fail as cache should be invalidated
        } catch (AuthorizationDeniedException ignore) {
        }
    }




}
