package org.keycloak.tests.organization.admin;

import java.util.List;
import java.util.Set;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class OrganizationMemberTest extends AbstractOrganizationTest {

    @Test
    public void testUserProfileAttributePermissions() {
        OrganizationRepresentation org = createOrganization();
        OrganizationResource orgResource = realm.admin().organizations().get(org.getId());
        MemberRepresentation member = addMember(orgResource, memberEmail, "Test", "User");

        UserProfileResource upResource = realm.admin().users().userProfile();
        UPConfig originalCfg = upResource.getConfiguration();

        try {
            // Restrict email and firstName to user-role only: admins (USER_API context) cannot view them
            UPConfig cfg = upResource.getConfiguration();

            UPAttribute emailAttr = cfg.getAttribute(UserModel.EMAIL);
            if (emailAttr == null) {
                emailAttr = new UPAttribute(UserModel.EMAIL);
            }
            emailAttr.setPermissions(new UPAttributePermissions(Set.of("user"), Set.of("user")));
            cfg.addOrReplaceAttribute(emailAttr);

            UPAttribute firstNameAttr = cfg.getAttribute(UserModel.FIRST_NAME);
            if (firstNameAttr == null) {
                firstNameAttr = new UPAttribute(UserModel.FIRST_NAME);
            }
            firstNameAttr.setPermissions(new UPAttributePermissions(Set.of("user"), Set.of("user")));
            cfg.addOrReplaceAttribute(firstNameAttr);

            upResource.update(cfg);

            // List endpoint: email and firstName must be filtered by user profile permissions
            List<MemberRepresentation> members = orgResource.members().search(memberEmail, true, 0, 10);
            assertEquals(1, members.size());
            assertNull(members.get(0).getEmail());
            assertNull(members.get(0).getFirstName());
            assertNull(members.get(0).getUserProfileMetadata());

            // Single member endpoint: same filtering must apply
            MemberRepresentation fetched = orgResource.members().member(member.getId()).toRepresentation();
            assertNull(fetched.getEmail());
            assertNull(fetched.getFirstName());
            assertNull(fetched.getUserProfileMetadata());

        } finally {
            upResource.update(originalCfg);
        }
    }
}
