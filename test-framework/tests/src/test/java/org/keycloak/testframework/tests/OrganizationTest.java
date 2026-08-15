package org.keycloak.testframework.tests;

import java.util.List;
import java.util.Map;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationInvitationRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.testframework.annotations.InjectOrganization;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedOrganization;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.OrganizationBuilder;
import org.keycloak.testframework.realm.OrganizationConfig;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@KeycloakIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrganizationTest {

    private static final String CUSTOM_ORG_REF = "custom";
    private static final String REALM_B_REF = "realmB";
    private static final String ORG_B_REF = "orgB";
    private static final String INVITEE_EMAIL = "invitee@example.org";

    @InjectRealm(config = OrganizationRealmConfig.class)
    ManagedRealm realm;

    @InjectRealm(ref = REALM_B_REF, config = OrganizationRealmConfig.class)
    ManagedRealm realmB;

    @InjectOrganization
    ManagedOrganization organization;

    @InjectOrganization(ref = CUSTOM_ORG_REF, config = CustomOrganizationConfig.class)
    ManagedOrganization customOrganization;

    @InjectOrganization(ref = ORG_B_REF, realmRef = REALM_B_REF)
    ManagedOrganization organizationB;

    @InjectUser(config = UserWithEmail.class)
    ManagedUser user;

    /**
     * The server sends the invitation email itself, so the realms require a working SMTP configuration
     */
    @InjectMailServer
    MailServer mail;

    @Test
    @Order(1)
    public void testDefaultOrganization() {
        Assertions.assertNotNull(organization.getId());
        Assertions.assertEquals("default", organization.getName());
        Assertions.assertNull(organization.getAlias());

        OrganizationRepresentation rep = organization.admin().toRepresentation();
        Assertions.assertEquals(organization.getId(), rep.getId());
        Assertions.assertEquals("default", rep.getName());
        // the server defaults the alias to the name when none is configured
        Assertions.assertEquals("default", rep.getAlias());
        Assertions.assertTrue(rep.isEnabled());
    }

    @Test
    @Order(2)
    public void testCustomOrganizationConfig() {
        Assertions.assertEquals(CUSTOM_ORG_REF, customOrganization.getName());
        Assertions.assertEquals("custom-alias", customOrganization.getAlias());

        OrganizationRepresentation rep = customOrganization.admin().toRepresentation();
        Assertions.assertEquals(CUSTOM_ORG_REF, rep.getName());
        Assertions.assertEquals("custom-alias", rep.getAlias());
        Assertions.assertEquals("A custom organization", rep.getDescription());
        Assertions.assertEquals("http://localhost:8080/custom", rep.getRedirectUrl());
        Assertions.assertEquals(List.of("value1", "value2"), rep.getAttributes().get("key"));

        List<String> domains = rep.getDomains().stream().map(OrganizationDomainRepresentation::getName).sorted().toList();
        Assertions.assertEquals(List.of("custom.org", "custom.test"), domains);
    }

    @Test
    @Order(3)
    public void testOrganizationAttachedToReferencedRealm() {
        Assertions.assertEquals(ORG_B_REF, organizationB.getName());

        List<String> defaultRealmOrgs = organizationIds(realm);
        Assertions.assertTrue(defaultRealmOrgs.contains(organization.getId()));
        Assertions.assertTrue(defaultRealmOrgs.contains(customOrganization.getId()));
        Assertions.assertFalse(defaultRealmOrgs.contains(organizationB.getId()));

        List<String> realmBOrgs = organizationIds(realmB);
        Assertions.assertEquals(List.of(organizationB.getId()), realmBOrgs);
    }

    @Test
    @Order(4)
    public void updateWithRollback() {
        organization.updateWithCleanup(o -> o.description("updated description").redirectUrl("http://localhost:8080/updated"));

        OrganizationRepresentation rep = organization.admin().toRepresentation();
        Assertions.assertEquals("updated description", rep.getDescription());
        Assertions.assertEquals("http://localhost:8080/updated", rep.getRedirectUrl());
    }

    @Test
    @Order(5)
    public void verifyUpdateWithRollback() {
        OrganizationRepresentation rep = organization.admin().toRepresentation();
        Assertions.assertNull(rep.getDescription());
        Assertions.assertNull(rep.getRedirectUrl());
    }

    @Test
    @Order(6)
    public void markOrganizationDirty() {
        organization.updateWithCleanup(o -> o.description("dirty description"));
        organization.dirty();

        Assertions.assertEquals("dirty description", organization.admin().toRepresentation().getDescription());
    }

    @Test
    @Order(7)
    public void verifyOrganizationRecreatedAfterDirty() {
        OrganizationRepresentation rep = organization.admin().toRepresentation();
        Assertions.assertEquals("default", rep.getName());
        Assertions.assertNull(rep.getDescription());

        // the organization was re-created, so the previous one must be gone and only the two
        // organizations of the default realm remain
        List<String> defaultRealmOrgs = organizationIds(realm);
        Assertions.assertEquals(2, defaultRealmOrgs.size());
        Assertions.assertTrue(defaultRealmOrgs.contains(organization.getId()));
        Assertions.assertTrue(defaultRealmOrgs.contains(customOrganization.getId()));
    }

    @Test
    @Order(9)
    public void inviteUserWithCleanup() throws MessagingException {
        organization.inviteUser(INVITEE_EMAIL, "In", "Vitee");

        assertInvitationEmailSentTo(INVITEE_EMAIL);

        OrganizationInvitationRepresentation invitation = organization.getInvitation(INVITEE_EMAIL);
        Assertions.assertNotNull(invitation);
        Assertions.assertEquals(organization.getId(), invitation.getOrganizationId());
        Assertions.assertEquals(INVITEE_EMAIL, invitation.getEmail());
        Assertions.assertEquals("In", invitation.getFirstName());
        Assertions.assertEquals("Vitee", invitation.getLastName());
        Assertions.assertEquals(OrganizationInvitationRepresentation.Status.PENDING, invitation.getStatus());
    }

    @Test
    @Order(10)
    public void inviteExistingUserWithCleanup() throws MessagingException {
        organization.inviteExistingUser(user);

        assertInvitationEmailSentTo(user.getEmail());

        OrganizationInvitationRepresentation invitation = organization.getInvitation(user.getEmail());
        Assertions.assertNotNull(invitation);
        Assertions.assertEquals(organization.getId(), invitation.getOrganizationId());
        Assertions.assertEquals(user.getEmail(), invitation.getEmail());
        Assertions.assertEquals(OrganizationInvitationRepresentation.Status.PENDING, invitation.getStatus());
    }

    @Test
    @Order(11)
    public void verifyInvitationsRemovedAfterTests() {
        Assertions.assertNull(organization.getInvitation(INVITEE_EMAIL));
        Assertions.assertNull(organization.getInvitation(user.getEmail()));
        Assertions.assertTrue(organization.admin().invitations().list().isEmpty());
    }

    /**
     * The mail server is purged after each test, so a single invitation must result in exactly one received email
     */
    private void assertInvitationEmailSentTo(String expectedRecipient) throws MessagingException {
        Assertions.assertTrue(mail.waitForIncomingEmail(1), "No invitation email received");

        MimeMessage[] messages = mail.getReceivedMessages();
        Assertions.assertEquals(1, messages.length);

        Address[] recipients = messages[0].getAllRecipients();
        Assertions.assertEquals(1, recipients.length);
        Assertions.assertEquals(expectedRecipient, ((InternetAddress) recipients[0]).getAddress());
    }

    private static List<String> organizationIds(ManagedRealm managedRealm) {
        return managedRealm.admin().organizations().list(null, null).stream()
                .map(OrganizationRepresentation::getId)
                .toList();
    }

    public static class OrganizationRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.organizationsEnabled(true);
        }
    }

    public static class CustomOrganizationConfig implements OrganizationConfig {

        @Override
        public OrganizationBuilder configure(OrganizationBuilder organization) {
            return organization.name(CUSTOM_ORG_REF)
                    .alias("custom-alias")
                    .description("A custom organization")
                    .redirectUrl("http://localhost:8080/custom")
                    .domains("custom.org", "custom.test")
                    .attributes(Map.of("key", List.of("value1", "value2")));
        }
    }

    public static class UserWithEmail implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("member").email("member@example.org").emailVerified(true);
        }
    }
}
