package org.keycloak.social.steam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Unit test for {@link SteamGameRoleMapper}
 * validating dynamic role assignment based on Valve API game ownership.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamGameRoleMapperTest {

    private TestableSteamGameRoleMapper mapper;
    private IdentityProviderMapperModel mapperModel;
    private BrokeredIdentityContext context;
    private KeycloakSession session;

    @Before
    public void setup() {
        mapper = new TestableSteamGameRoleMapper();

        mapperModel = new IdentityProviderMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put("appId", "730");
        config.put("role", "premium_gamer");
        mapperModel.setConfig(config);

        SteamIdentityProviderConfig idpConfig = new SteamIdentityProviderConfig();
        idpConfig.setEnabled(true);
        idpConfig.setSteamApiKey("TEST_API_KEY");

        context = new BrokeredIdentityContext("123456789", idpConfig);

        session = createDummySession("123456789", "TEST_API_KEY");
    }

    private KeycloakSession createDummySession(String steamId64, String apiKey) {
        // FederatedIdentityModel for the steam user
        FederatedIdentityModel federatedIdentity = new FederatedIdentityModel(
                SteamIdentityProviderFactory.PROVIDER_ID,
                steamId64,
                "someUsername"
        );

        // UserProvider mock: returns the federated identity stream
        UserProvider userProvider = (UserProvider) Proxy.newProxyInstance(
                UserProvider.class.getClassLoader(),
                new Class[]{UserProvider.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getFederatedIdentitiesStream")) {
                        return Stream.of(federatedIdentity);
                    }
                    return null;
                }
        );

        // IdentityProviderModel mock: returns the api key from config
        Map<String, String> idpConfigMap = new HashMap<>();
        idpConfigMap.put("steamApiKey", apiKey);
        IdentityProviderModel idpModel = new IdentityProviderModel();
        idpModel.setConfig(idpConfigMap);

        // IdentityProviderStorageProvider mock: returns the idpModel by alias
        IdentityProviderStorageProvider idpStorageProvider = (IdentityProviderStorageProvider) Proxy.newProxyInstance(
                IdentityProviderStorageProvider.class.getClassLoader(),
                new Class[]{IdentityProviderStorageProvider.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getByAlias")) return idpModel;
                    return null;
                }
        );

        // KeycloakSession mock: routes users() and getProvider() to the mocks above
        return (KeycloakSession) Proxy.newProxyInstance(
                KeycloakSession.class.getClassLoader(),
                new Class[]{KeycloakSession.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("users")) return userProvider;
                    if (method.getName().equals("getProvider")) return idpStorageProvider;
                    return null;
                }
        );
    }

    private RoleModel createDummyRole(String roleName) {
        return (RoleModel) Proxy.newProxyInstance(
                RoleModel.class.getClassLoader(),
                new Class[]{RoleModel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return roleName;
                    return null;
                }
        );
    }

    private RealmModel createDummyRealm() {
        return (RealmModel) Proxy.newProxyInstance(
                RealmModel.class.getClassLoader(),
                new Class[]{RealmModel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRole")) return createDummyRole((String) args[0]);
                    return null;
                }
        );
    }

    private UserModel createDummyUser(List<String> grantedRoles) {
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("grantRole")) {
                        RoleModel role = (RoleModel) args[0];
                        grantedRoles.add(role.getName());
                    }
                    return null;
                }
        );
    }

    @Test
    public void testUserOwnsGame_RoleIsGranted() {
        mapper.mockJsonResponse = "{\"response\":{\"game_count\":1,\"games\":[{\"appid\":730,\"playtime_forever\":1000}]}}";

        List<String> grantedRoles = new ArrayList<>();
        UserModel dummyUser = createDummyUser(grantedRoles);
        RealmModel dummyRealm = createDummyRealm();

        mapper.importNewUser(session, dummyRealm, dummyUser, mapperModel, context);

        Assert.assertEquals(1, grantedRoles.size());
        Assert.assertEquals("premium_gamer", grantedRoles.get(0));
    }

    @Test
    public void testUserDoesNotOwnGame_RoleIsNotGranted() {
        mapper.mockJsonResponse = "{\"response\":{\"game_count\":1,\"games\":[{\"appid\":4000,\"playtime_forever\":20}]}}";

        List<String> grantedRoles = new ArrayList<>();
        UserModel dummyUser = createDummyUser(grantedRoles);
        RealmModel dummyRealm = createDummyRealm();

        mapper.importNewUser(session, dummyRealm, dummyUser, mapperModel, context);

        Assert.assertTrue("Role should not be granted if the AppID doesn't match.", grantedRoles.isEmpty());
    }

    /**
     * Concrete wrapper overriding the network boundary to inject JSON payloads natively.
     */
    private class TestableSteamGameRoleMapper extends SteamGameRoleMapper {
        public String mockJsonResponse;

        @Override
        protected boolean isGameOwned(KeycloakSession session, String url, int targetAppId) throws java.io.IOException {
            JsonNode tree = new ObjectMapper().readTree(mockJsonResponse);
            JsonNode games = tree.path("response").path("games");
            if (games.isArray()) {
                for (JsonNode game : games) {
                    if (game.path("appid").asInt() == targetAppId) return true;
                }
            }
            return false;
        }
    }
}
