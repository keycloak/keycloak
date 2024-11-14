package org.keycloak.test.admin.userprofile;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserProfileAttributeGroupMetadata;
import org.keycloak.representations.idm.UserProfileMetadata;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.utils.JsonTestUtils;
import org.keycloak.userprofile.config.UPConfigUtils;

import java.util.List;
import java.util.Map;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserProfileAdminTest {

    @InjectRealm(lifecycle = LifeCycle.CLASS)
    private ManagedRealm realm;

    @Test
    @Order(1)
    public void testDefaultConfigIfNoneSet() {
        JsonTestUtils.assertJsonEquals(UPConfigUtils.readSystemDefaultConfig(), realm.admin().users().userProfile().getConfiguration());
    }

    @Test
    public void testSetDefaultConfig() {
        UPConfig config = UPConfigUtils.parseSystemDefaultConfig().addOrReplaceAttribute(new UPAttribute("test"));
        UserProfileResource userProfile = realm.admin().users().userProfile();
        userProfile.update(config);
        // TODO
        /*getCleanup().addCleanup(() -> testRealm().users().userProfile().update(null));*/

        JsonTestUtils.assertJsonEquals(config, userProfile.getConfiguration());
    }

    @Test
    public void testEmailRequiredIfEmailAsUsernameEnabled() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setRegistrationEmailAsUsername(true);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });*/
        UserProfileResource userProfile = realm.admin().users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        Assertions.assertTrue(metadata.getAttributeMetadata(UserModel.EMAIL).isRequired());
    }

    @Test
    public void testEmailNotRequiredIfEmailAsUsernameDisabled() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });*/
        UserProfileResource userProfile = realm.admin().users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        Assertions.assertFalse(metadata.getAttributeMetadata(UserModel.EMAIL).isRequired());
    }

    @Test
    public void testUsernameRequiredAndWritableIfEmailAsUsernameDisabledAndEditUsernameAllowed() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });*/
        realmRep.setEditUsernameAllowed(true);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            realm.update(realmRep);
        });*/
        UserProfileResource userProfile = realm.admin().users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        Assertions.assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
        Assertions.assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
    }

    @Test
    public void testUsernameRequiredAndWritableIfEmailAsUsernameDisabledAndEditUsernameDisabled() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setRegistrationEmailAsUsername(false);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });*/
        realmRep.setEditUsernameAllowed(false);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setEditUsernameAllowed(editUsernameAllowed);
            realm.update(realmRep);
        });*/
        UserProfileResource userProfile = realm.admin().users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        Assertions.assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
        Assertions.assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
    }

    @Test
    public void testUsernameNotRequiredIfEmailAsUsernameEnabled() {
        RealmRepresentation realmRep = realm.admin().toRepresentation();
        realmRep.setRegistrationEmailAsUsername(true);
        realm.admin().update(realmRep);
        // TODO
        /*getCleanup().addCleanup(() -> {
            realmRep.setRegistrationEmailAsUsername(registrationEmailAsUsername);
            realm.update(realmRep);
        });*/
        UserProfileResource userProfile = realm.admin().users().userProfile();
        UserProfileMetadata metadata = userProfile.getMetadata();
        Assertions.assertFalse(metadata.getAttributeMetadata(UserModel.USERNAME).isRequired());
        Assertions.assertTrue(metadata.getAttributeMetadata(UserModel.USERNAME).isReadOnly());
    }

    @Test
    public void testGroupsMetadata() {
        UPConfig config = realm.admin().users().userProfile().getConfiguration();

        for (int i = 0; i < 3; i++) {
            UPGroup group = new UPGroup();
            group.setName("name-" + i);
            group.setDisplayHeader("displayHeader-" + i);
            group.setDisplayDescription("displayDescription-" + i);
            group.setAnnotations(Map.of("k1", "v1", "k2", "v2", "k3", "v3"));
            config.addGroup(group);
        }

        UPAttribute firstName = config.getAttribute(UserModel.FIRST_NAME);
        firstName.setGroup(config.getGroups().get(0).getName());
        UserProfileResource userProfile = realm.admin().users().userProfile();
        userProfile.update(config);
        // TODO
        /*getCleanup().addCleanup(() -> testRealm().users().userProfile().update(null));*/

        UserProfileMetadata metadata = realm.admin().users().userProfile().getMetadata();
        List<UserProfileAttributeGroupMetadata> groups = metadata.getGroups();
        Assertions.assertNotNull(groups);
        Assertions.assertFalse(groups.isEmpty());
        Assertions.assertEquals(config.getGroups().size(), groups.size());
        for (UPGroup group : config.getGroups()) {
            UserProfileAttributeGroupMetadata mGroup = metadata.getAttributeGroupMetadata(group.getName());
            Assertions.assertNotNull(mGroup);
            Assertions.assertEquals(group.getName(), mGroup.getName());
            Assertions.assertEquals(group.getDisplayHeader(), mGroup.getDisplayHeader());
            Assertions.assertEquals(group.getDisplayDescription(), mGroup.getDisplayDescription());
            if (group.getAnnotations() == null) {
                Assertions.assertEquals(group.getAnnotations(), mGroup.getAnnotations());
            } else {
                Assertions.assertEquals(group.getAnnotations().size(), mGroup.getAnnotations().size());
            }
        }
        Assertions.assertEquals(config.getGroups().get(0).getName(), metadata.getAttributeMetadata(UserModel.FIRST_NAME).getGroup());
    }
}
