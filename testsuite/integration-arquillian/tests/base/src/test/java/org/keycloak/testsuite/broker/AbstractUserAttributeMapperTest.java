package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.AccountHelper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.keycloak.testsuite.broker.KcSamlBrokerConfiguration.ATTRIBUTE_TO_MAP_FRIENDLY_NAME;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractUserAttributeMapperTest extends AbstractIdentityProviderMapperTest {

    protected static final String MAPPED_ATTRIBUTE_NAME = "mapped-user-attribute";
    protected static final String MAPPED_ATTRIBUTE_FRIENDLY_NAME = "mapped-user-attribute-friendly";

    private static final Set<String> PROTECTED_NAMES = ImmutableSet.<String>builder().add("email").add("lastName").add("firstName").build();
    private static final Map<String, String> ATTRIBUTE_NAME_TRANSLATION = ImmutableMap.<String, String>builder()
      .put("dotted.email", "dotted.email")
      .put("nested.email", "nested.email")
      .put(ATTRIBUTE_TO_MAP_FRIENDLY_NAME, MAPPED_ATTRIBUTE_FRIENDLY_NAME)
      .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, MAPPED_ATTRIBUTE_NAME)
      .build();

    protected abstract Iterable<IdentityProviderMapperRepresentation> createIdentityProviderMappers(IdentityProviderMapperSyncMode syncMode);

    public void addIdentityProviderToConsumerRealm(IdentityProviderMapperSyncMode syncMode) {
        IdentityProviderRepresentation idp = setupIdentityProvider();

        IdentityProviderResource idpResource = realm.identityProviders().get(idp.getAlias());
        for (IdentityProviderMapperRepresentation mapper : createIdentityProviderMappers(syncMode)) {
            mapper.setIdentityProviderAlias(bc.getIDPAlias());
            idpResource.addMapper(mapper).close();
        }
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

    private void testValueMappingForImportSyncMode(Map<String, List<String>> initialUserAttributes, Map<String, List<String>> modifiedUserAttributes) {
        addIdentityProviderToConsumerRealm(IdentityProviderMapperSyncMode.IMPORT);
        testValueMapping(initialUserAttributes, modifiedUserAttributes, initialUserAttributes);
    }

    private void testValueMappingForForceSyncMode(Map<String, List<String>> initialUserAttributes, Map<String, List<String>> modifiedUserAttributes) {
        addIdentityProviderToConsumerRealm(IdentityProviderMapperSyncMode.FORCE);
        testValueMapping(initialUserAttributes, modifiedUserAttributes, modifiedUserAttributes);
    }

    private void testValueMappingForLegacySyncMode(Map<String, List<String>> initialUserAttributes, Map<String, List<String>> modifiedUserAttributes) {
        addIdentityProviderToConsumerRealm(IdentityProviderMapperSyncMode.LEGACY);
        testValueMapping(initialUserAttributes, modifiedUserAttributes, modifiedUserAttributes);
    }

    private void testValueMapping(Map<String, List<String>> initialUserAttributes, Map<String, List<String>> modifiedUserAttributes, Map<String, List<String>> assertedModifiedAttributes) {
        String email = bc.getUserEmail();
        createUserInProviderRealm(initialUserAttributes);

        logInAsUserInIDPForFirstTime();
        UserRepresentation userRep = findUser(bc.consumerRealmName(), bc.getUserLogin(), email);

        assertUserAttributes(initialUserAttributes, userRep);

        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

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

        assertUserAttributes(assertedModifiedAttributes, userRep);
    }

    @Test
    public void testBasicMappingSingleValueForce() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingSingleValueImport() {
        testValueMappingForImportSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingEmail() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
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
    public void testBasicMappingAttributeGetsModifiedInSyncModeForce() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().build())
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeGetsRemovedInSyncModeForce() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeWithMultipleValuesIsModifiedInSyncModeForce() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeWithMultipleValuesIsModifiedInSyncModeLegacy() {
        testValueMappingForLegacySyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeWithMultipleValuesDoesNotGetModifiedInSyncModeImport() {
        testValueMappingForImportSyncMode(ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("value 1").add("value 2").build())
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeWithMultipleValuesGetsAddedInSyncModeForce() {
        testValueMappingForForceSyncMode(ImmutableMap.<String, List<String>>builder()
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }

    @Test
    public void testBasicMappingAttributeWithMultipleValuesDoesNotGetAddedInSyncModeImport() {
        testValueMappingForImportSyncMode(ImmutableMap.<String, List<String>>builder()
          .build(),
          ImmutableMap.<String, List<String>>builder()
          .put(KcOidcBrokerConfiguration.ATTRIBUTE_TO_MAP_NAME, ImmutableList.<String>builder().add("second value").add("second value 2").build())
          .build()
        );
    }
}
