package org.keycloak.testsuite.broker;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.mappers.AdvancedClaimToRoleMapper;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;

/**
 * @author hmlnarik, <a href="mailto:external.benjamin.weimer@bosch-si.com">Benjamin Weimer</a>
 */
public class OidcAdvancedClaimToRoleMapperTest extends AbstractBaseBrokerTest {

    private static final String CLIENT = "realm-management";
    private static final String CLIENT_ROLE = "view-realm";
    private static final String CLIENT_ROLE_MAPPER_REPRESENTATION = CLIENT + "." + CLIENT_ROLE;

    private static final String CLAIMS = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"value 1\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";

    private static final String CLAIMS_REGEX = "[\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME + "\",\n" +
            "    \"value\": \"va.*\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"key\": \"" + KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2 + "\",\n" +
            "    \"value\": \"value 2\"\n" +
            "  }\n" +
            "]";


    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration();
    }

    @Before
    public void addClients() {
        List<ClientRepresentation> clients = bc.createProviderClients(suiteContext);
        if (clients != null) {
            RealmResource providerRealm = adminClient.realm(bc.providerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.providerRealmName());

                Response resp = providerRealm.clients().create(client);
                resp.close();
            }
        }

        clients = bc.createConsumerClients(suiteContext);
        if (clients != null) {
            RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
            for (ClientRepresentation client : clients) {
                log.debug("adding client " + client.getName() + " to realm " + bc.consumerRealmName());

                Response resp = consumerRealm.clients().create(client);
                resp.close();
            }
        }
    }

    @Test
    public void valueMatchesRegexTest() {
        AdvancedClaimToRoleMapper advancedClaimToRoleMapper = new AdvancedClaimToRoleMapper();

        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", "AB_ADMIN"), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", "AA_ADMIN"), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("99.*", 999), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("98.*", 999), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("99\\..*", 99.9), is(true));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", null), is(false));
        assertThat(advancedClaimToRoleMapper.valueMatchesRegex("AB.*", Arrays.asList("AB_ADMIN", "AA_ADMIN")), is(true));
    }

    @Test
    public void allClaimValuesMatch() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());
    }

    @Test
    public void claimValuesMismatch() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());
    }

    @Test
    public void claimValuesMatchIfNoClaimsSpecified() {
        createAdvancedClaimToRoleMapper("[]", false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("some value").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("some value").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());
    }

    @Test
    public void allClaimValuesMatchRegex() {
        createAdvancedClaimToRoleMapper(CLAIMS_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());
    }


    @Test
    public void claimValuesMismatchRegex() {
        createAdvancedClaimToRoleMapper(CLAIMS_REGEX, true);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("mismatch").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasNotBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());
    }

    @Test
    public void updateBrokeredUserMismatchDeletesRole() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());

        // update
        user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> mismatchingAttributes = ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value mismatch").build())
                .build();
        user.setAttributes(mismatchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);

        logInAsUserInIDP();
        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());

        assertThatRoleHasNotBeenAssigned(user);
    }

    @Test
    public void updateBrokeredUserMatchDoesntDeleteRole() {
        createAdvancedClaimToRoleMapper(CLAIMS, false);
        createUserInProviderRealm(ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .build());

        logInAsUserInIDPForFirstTime();

        UserRepresentation user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        assertThatRoleHasBeenAssigned(user);

        logoutFromRealm(bc.consumerRealmName());

        // update
        user = findUser(bc.providerRealmName(), bc.getUserLogin(), bc.getUserEmail());
        ImmutableMap<String, List<String>> matchingAttributes = ImmutableMap.<String, List<String>>builder()
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
                .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME_2, ImmutableList.<String>builder().add("value 2").build())
                .put("some.other.attribute", ImmutableList.<String>builder().add("some value").build())
                .build();
        user.setAttributes(matchingAttributes);
        adminClient.realm(bc.providerRealmName()).users().get(user.getId()).update(user);

        logInAsUserInIDP();
        user = findUser(bc.consumerRealmName(), bc.getUserLogin(), bc.getUserEmail());

        assertThatRoleHasBeenAssigned(user);
    }

    private void createAdvancedClaimToRoleMapper(String claimsRepresentation, boolean areClaimValuesRegex) {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        final IdentityProviderRepresentation idp = bc.setUpIdentityProvider(suiteContext);
        Response resp = realm.identityProviders().create(idp);
        resp.close();

        IdentityProviderMapperRepresentation advancedClaimToRoleMapper = new IdentityProviderMapperRepresentation();
        advancedClaimToRoleMapper.setName("advanced-claim-to-role-mapper");
        advancedClaimToRoleMapper.setIdentityProviderMapper(AdvancedClaimToRoleMapper.PROVIDER_ID);
        advancedClaimToRoleMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(AdvancedClaimToRoleMapper.CLAIM_PROPERTY_NAME, claimsRepresentation)
                .put(AdvancedClaimToRoleMapper.ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME, areClaimValuesRegex ? "true" : "false")
                .put(ConfigConstants.ROLE, CLIENT_ROLE_MAPPER_REPRESENTATION)
                .build());

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        advancedClaimToRoleMapper.setIdentityProviderAlias(bc.getIDPAlias());
        resp = idpResource.addMapper(advancedClaimToRoleMapper);
        resp.close();
    }

    private void createUserInProviderRealm(Map<String, List<String>> attributes) {
        log.debug("Creating user in realm " + bc.providerRealmName());

        UserRepresentation user = UserBuilder.create()
                .username(bc.getUserLogin())
                .email(bc.getUserEmail())
                .build();
        user.setEmailVerified(true);
        user.setAttributes(attributes);
        this.userId = createUserAndResetPasswordWithAdminClient(adminClient.realm(bc.providerRealmName()), user, bc.getUserPassword());
    }

    private UserRepresentation findUser(String realm, String userName, String email) {
        UsersResource consumerUsers = adminClient.realm(realm).users();

        List<UserRepresentation> users = consumerUsers.list();
        assertThat("There must be exactly one user", users, hasSize(1));
        UserRepresentation user = users.get(0);
        assertThat("Username has to match", user.getUsername(), equalTo(userName));
        assertThat("Email has to match", user.getEmail(), equalTo(email));

        MappingsRepresentation roles = consumerUsers.get(user.getId()).roles().getAll();

        List<String> realmRoles = roles.getRealmMappings().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
        user.setRealmRoles(realmRoles);

        Map<String, List<String>> clientRoles = new HashMap<>();
        roles.getClientMappings().forEach((key, value) -> clientRoles.put(key, value.getMappings().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList())));
        user.setClientRoles(clientRoles);

        return user;
    }

    private void assertThatRoleHasBeenAssigned(UserRepresentation user) {
        assertThat(user.getClientRoles().get(CLIENT), contains(CLIENT_ROLE));
    }

    private void assertThatRoleHasNotBeenAssigned(UserRepresentation user) {
        assertThat(user.getClientRoles().get(CLIENT), not(contains(CLIENT_ROLE)));
    }
}
