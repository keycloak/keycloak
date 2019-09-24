package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.UserBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static org.keycloak.testsuite.admin.ApiUtil.*;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractUserAttributeMapperTest extends AbstractBaseBrokerTest {

    protected static final String MAPPED_ATTRIBUTE_NAME = "mapped-user-attribute";
    protected static final String MAPPED_ATTRIBUTE_FRIENDLY_NAME = "mapped-user-attribute-friendly";
    protected static final String ATTRIBUTE_TO_MAP_FRIENDLY_NAME = "user-attribute-friendly";

    private static final Set<String> PROTECTED_NAMES = ImmutableSet.<String>builder().add("email").add("lastName").add("firstName").build();
    private static final Map<String, String> ATTRIBUTE_NAME_TRANSLATION = ImmutableMap.<String, String>builder()
      .put("dotted.email", "dotted.email")
      .put("nested.email", "nested.email")
      .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, MAPPED_ATTRIBUTE_FRIENDLY_NAME)
      .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, MAPPED_ATTRIBUTE_NAME)
      .build();

    protected abstract Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers();

    @Before
    public void addIdentityProviderToConsumerRealm() {
        log.debug("adding identity provider to realm " + bc.consumerRealmName());

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        final IdentityProviderRepresentation idp = bc.setUpIdentityProvider(suiteContext);
        Response resp = realm.identityProviders().create(idp);
        resp.close();

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        for (IdentityProviderMapperRepresentation mapper : createIdentityProviderMappers()) {
            mapper.setIdentityProviderAlias(bc.getIDPAlias());
            resp = idpResource.addMapper(mapper);
            resp.close();
        }
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

    protected void createUserInProviderRealm(Map<String, List<String>> attributes) {
        log.debug("creating user in realm " + bc.providerRealmName());

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

        int userCount = consumerUsers.count();
        assertThat("There must be at least one user", userCount, greaterThan(0));

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        for (UserRepresentation user : users) {
            if (user.getUsername().equals(userName) && user.getEmail().equals(email)) {
                return user;
            }
        }

        fail("User " + userName + " not found in " + realm + " realm");
        return null;
    }

    private void assertUserAttributes(Map<String, List<String>> attrs, UserRepresentation userRep) {
        Set<String> mappedAttrNames = attrs.entrySet().stream()
          .filter(me -> me.getValue() != null && ! me.getValue().isEmpty())
          .map(me -> me.getKey())
          .filter(a -> ! PROTECTED_NAMES.contains(a))
          .map(ATTRIBUTE_NAME_TRANSLATION::get)
          .collect(Collectors.toSet());

        if (mappedAttrNames.isEmpty()) {
            assertThat("No attributes are expected to be present", userRep.getAttributes(), nullValue());
        } else {
            assertThat(userRep.getAttributes(), notNullValue());
            assertThat(userRep.getAttributes().keySet(), equalTo(mappedAttrNames));
            for (Map.Entry<String, List<String>> me : attrs.entrySet()) {
                String mappedAttrName = ATTRIBUTE_NAME_TRANSLATION.get(me.getKey());
                if (mappedAttrNames.contains(mappedAttrName)) {
                    assertThat(userRep.getAttributes().get(mappedAttrName), containsInAnyOrder(me.getValue().toArray()));
                }
            }
        }

        if (attrs.containsKey("email")) {
            assertThat(userRep.getEmail(), equalTo(attrs.get("email").get(0)));
        }
        if (attrs.containsKey("firstName")) {
            assertThat(userRep.getFirstName(), equalTo(attrs.get("firstName").get(0)));
        }
        if (attrs.containsKey("lastName")) {
            assertThat(userRep.getLastName(), equalTo(attrs.get("lastName").get(0)));
        }
    }

    protected void testValueMapping(Map<String, List<String>> initialUserAttributes, Map<String, List<String>> modifiedUserAttributes) {
        String email = bc.getUserEmail();
        createUserInProviderRealm(initialUserAttributes);

        logInAsUserInIDPForFirstTime();
        UserRepresentation userRep = findUser(bc.consumerRealmName(), bc.getUserLogin(), email);

        assertUserAttributes(initialUserAttributes, userRep);

        logoutFromRealm(bc.consumerRealmName());

        // update user in provider realm
        UserRepresentation userRepProvider = findUser(bc.providerRealmName(), bc.getUserLogin(), email);
        Map<String, List<String>> modifiedWithoutSpecialKeys = modifiedUserAttributes.entrySet().stream()
          .filter(a -> ! PROTECTED_NAMES.contains(a.getKey()))
          .filter(a -> a.getValue() != null)  // Remove empty attributes
          .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
        userRepProvider.setAttributes(modifiedWithoutSpecialKeys);
        if (modifiedUserAttributes.containsKey("email")) {
            userRepProvider.setEmail(modifiedUserAttributes.get("email").get(0));
            email = modifiedUserAttributes.get("email").get(0);
        }
        if (modifiedUserAttributes.containsKey("firstName")) {
            userRepProvider.setFirstName(modifiedUserAttributes.get("firstName").get(0));
        }
        if (modifiedUserAttributes.containsKey("lastName")) {
            userRepProvider.setLastName(modifiedUserAttributes.get("lastName").get(0));
        }
        adminClient.realm(bc.providerRealmName()).users().get(userRepProvider.getId()).update(userRepProvider);

        logInAsUserInIDP();
        userRep = findUser(bc.consumerRealmName(), bc.getUserLogin(), email);

        assertUserAttributes(modifiedUserAttributes, userRep);
    }

    @Test
    public void testBasicMappingSingleValue() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingEmail() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put("email", ImmutableList.<String>builder().add(bc.getUserEmail()).build())
          .put("nested.email", ImmutableList.<String>builder().add(bc.getUserEmail()).build())
          .put("dotted.email", ImmutableList.<String>builder().add(bc.getUserEmail()).build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put("email", ImmutableList.<String>builder().add("other_email@redhat.com").build())
          .put("nested.email", ImmutableList.<String>builder().add("other_email@redhat.com").build())
          .put("dotted.email", ImmutableList.<String>builder().add("other_email@redhat.com").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingClearValue() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().build())
          .build()
        );
    }

    @Test
    public void testBasicMappingRemoveValue() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .build()
        );
    }

    @Test
    public void testBasicMappingMultipleValues() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testAddBasicMappingMultipleValues() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testDeleteBasicMappingMultipleValues() {
        testValueMapping(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .build()
        );
    }
}
