package org.keycloak.tests.admin.partialimport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationGroupResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.partialimport.PartialImportResult;
import org.keycloak.partialimport.PartialImportResults;
import org.keycloak.partialimport.ResourceType;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationIdentityProviderLinkRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = AbstractPartialImportTest.PartialImportServerConfig.class)
public class PartialImportOrganizationTest extends AbstractPartialImportTest {

    @InjectRealm(ref = "source", lifecycle = LifeCycle.METHOD, config = OrganizationsRealmConfig.class)
    ManagedRealm sourceRealm;

    @InjectRunOnServer(realmRef = "master")
    RunOnServerClient runOnServer;

    @BeforeEach
    public void enableOrganizations() {
        managedRealm.updateWithCleanup(realm -> realm.organizationsEnabled(true));
    }

    @Test
    @DatabaseTest
    public void testImportOrganizationExportedFromAnotherRealm() throws IOException {
        createOrganizationWithBrokersAndGroups(sourceRealm.admin());
        RealmRepresentation exported = partialExport(sourceRealm);
        piRep.setIdentityProviders(exported.getIdentityProviders());
        piRep.setOrganizations(exported.getOrganizations());
        adminEvents.clear();

        PartialImportResults results = doImport();

        assertEquals(3, results.getAdded(), results.getErrorMessage());
        assertEquals(organizationDefinitions(sourceRealm), organizationDefinitions(managedRealm));
        List<String> createdPaths = Stream.generate(adminEvents::poll).limit(results.getAdded()).map(AdminEventRepresentation::getResourcePath).toList();
        assertThat(createdPaths, hasItem("organizations/" + organizationId(results)));
    }

    @Test
    public void testAddOrganizationFail() {
        setFail();
        piRep.setOrganizations(List.of(organization("Acme Corp", "acme")));

        assertNull(doImport().getErrorMessage());
        assertEquals("Organization 'acme' already exists.", assertImportFails(Response.Status.CONFLICT).getErrorMessage());
    }

    @Test
    public void testAddOrganizationSkip() {
        setSkip();
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        piRep.setOrganizations(List.of(organization));
        String organizationId = organizationId(doImport());
        organization.setDescription("Changed in the file");
        piRep.setOrganizations(List.of(organization, organization("Globex", "globex")));

        PartialImportResults results = doImport();

        assertEquals(1, results.getSkipped(), results.getErrorMessage());
        assertEquals(1, results.getAdded());
        assertNull(managedRealm.admin().organizations().get(organizationId).toRepresentation().getDescription());
    }

    @Test
    public void testOverwriteIsRefusedForAnExistingOrganization() {
        setOverwrite();
        addClients(false);
        piRep.setOrganizations(List.of(organization("Acme Corp", "acme")));
        PartialImportResults added = doImport();
        assertEquals(NUM_ENTITIES + 1, added.getAdded(), added.getErrorMessage());
        String organizationId = organizationId(added);
        String clientUuid = clientUuid(CLIENT_PREFIX + 0);
        String memberId = addManagedMember(organizationId);

        ErrorRepresentation refused = assertImportFails(Response.Status.CONFLICT);

        assertEquals("Organization 'acme' already exists. The partial import does not overwrite organizations.", refused.getErrorMessage());
        assertEquals(clientUuid, clientUuid(CLIENT_PREFIX + 0));
        List<MemberRepresentation> members = managedRealm.admin().organizations().get(organizationId).members().getAll();
        assertEquals(List.of(memberId), members.stream().map(MemberRepresentation::getId).toList());
        assertEquals(MembershipType.MANAGED, members.get(0).getMembershipType());
    }

    @Test
    public void testIdentityProviderOfAnOrganizationMustExist() {
        setFail();
        addClients(false);
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.addIdentityProvider(broker("missing-broker"));
        piRep.setOrganizations(List.of(organization));

        ErrorRepresentation error = assertImportFails(Response.Status.BAD_REQUEST);

        assertEquals("Identity provider 'missing-broker' linked to organization 'acme' does not exist.", error.getErrorMessage());
        assertTrue(managedRealm.admin().clients().findByClientId(CLIENT_PREFIX + 0).isEmpty());
    }

    @Test
    public void testIdentityProviderLinkWithoutSettingsGetsTheDefaults() {
        setFail();
        for (String alias : List.of("github", "google", "facebook")) {
            assertStatus(Response.Status.CREATED, managedRealm.admin().identityProviders().create(broker(alias)));
        }
        IdentityProviderRepresentation withoutLinks = broker("github");
        IdentityProviderRepresentation withNoLink = broker("google");
        withNoLink.setOrganizationLinks(List.of());
        IdentityProviderRepresentation withEmptyLink = broker("facebook");
        withEmptyLink.setOrganizationLinks(List.of(new OrganizationIdentityProviderLinkRepresentation()));
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.setIdentityProviders(List.of(withoutLinks, withNoLink, withEmptyLink));
        piRep.setOrganizations(List.of(organization));

        String organizationId = organizationId(doImport());

        List<OrganizationIdentityProviderLinkRepresentation> links = managedRealm.admin().organizations().get(organizationId).identityProviders().getIdentityProviders().stream()
                .map(linked -> linked.getOrganizationLinks().get(0))
                .toList();
        assertEquals(List.of(true, true, true), links.stream().map(OrganizationIdentityProviderLinkRepresentation::getAutoMembership).toList());
        assertEquals(List.of("UNMANAGED", "UNMANAGED", "UNMANAGED"), links.stream().map(OrganizationIdentityProviderLinkRepresentation::getMembershipType).toList());
    }

    @Test
    public void testGroupWithOnlyANameIsImported() {
        setFail();
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.addGroup(group("engineering"));
        piRep.setOrganizations(List.of(organization));

        doImport();

        List<GroupRepresentation> groups = partialExport(managedRealm).getOrganizations().get(0).getGroups();
        assertEquals(List.of("engineering"), groups.stream().map(GroupRepresentation::getName).toList());
    }

    @Test
    public void testInvalidRedirectUrlIsABadRequest() {
        setFail();
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.setRedirectUrl("not a url");
        piRep.setOrganizations(List.of(organization));

        assertImportFails(Response.Status.BAD_REQUEST);
    }

    @Test
    public void testInvalidMembershipTypeIsABadRequest() {
        setFail();
        assertStatus(Response.Status.CREATED, managedRealm.admin().identityProviders().create(broker("github")));
        OrganizationIdentityProviderLinkRepresentation link = new OrganizationIdentityProviderLinkRepresentation();
        link.setMembershipType("INVALID");
        IdentityProviderRepresentation linkedBroker = broker("github");
        linkedBroker.setOrganizationLinks(List.of(link));
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.addIdentityProvider(linkedBroker);
        piRep.setOrganizations(List.of(organization));

        ErrorRepresentation error = assertImportFails(Response.Status.BAD_REQUEST);

        assertEquals("Invalid configuration for organization 'acme': No enum constant org.keycloak.representations.idm.MembershipType.INVALID", error.getErrorMessage());
        assertTrue(managedRealm.admin().organizations().getAll().isEmpty());
    }

    @Test
    public void testRoleOfAMissingClientIsABadRequest() {
        setFail();
        GroupRepresentation engineering = group("engineering");
        engineering.setClientRoles(Map.of("missing-client", List.of("viewer")));
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        organization.addGroup(engineering);
        piRep.setOrganizations(List.of(organization));

        OAuth2ErrorRepresentation error = assertImportFails(Response.Status.BAD_REQUEST, OAuth2ErrorRepresentation.class);

        assertEquals("Unable to find client role mappings for client: missing-client", error.getErrorDescription());
        assertTrue(managedRealm.admin().organizations().getAll().isEmpty());
    }

    @Test
    public void testMembersOfAnOrganizationAreNotImported() {
        setFail();
        managedRealm.addUser(UserBuilder.create().username("alice").email("alice@acme.example"));
        OrganizationRepresentation organization = organization("Acme Corp", "acme");
        MemberRepresentation member = new MemberRepresentation();
        member.setUsername("alice");
        member.setMembershipType(MembershipType.UNMANAGED);
        organization.addMember(member);
        piRep.setOrganizations(List.of(organization));

        PartialImportResults results = doImport();

        assertEquals(1, results.getAdded(), results.getErrorMessage());
        assertTrue(managedRealm.admin().organizations().get(organizationId(results)).members().getAll().isEmpty());
    }

    @Test
    public void testNameOfAnotherOrganizationIsRefused() {
        setSkip();
        piRep.setOrganizations(List.of(organization("Acme Corp", "acme")));
        doImport();
        piRep.setOrganizations(List.of(organization("Acme Corp", "acme-france")));

        assertImportFails(Response.Status.CONFLICT);

        assertEquals(1, managedRealm.admin().organizations().getAll().size());
    }

    @Test
    public void testOrganizationWithoutAliasUsesItsNameAsAlias() {
        setSkip();
        piRep.setOrganizations(List.of(organization("Acme France", "acme")));
        doImport();
        piRep.setOrganizations(List.of(organization("acme")));

        PartialImportResults results = doImport();

        assertEquals(1, results.getSkipped(), results.getErrorMessage());
    }

    @Test
    public void testOrganizationsAreIgnoredWhenDisabledForTheRealm() {
        managedRealm.updateWithCleanup(realm -> realm.organizationsEnabled(false));
        setFail();
        addGroups(1);
        piRep.setOrganizations(List.of(organization("Acme Corp", "acme")));

        PartialImportResults results = doImport();

        assertEquals(1, results.getAdded(), results.getErrorMessage());
    }

    private ErrorRepresentation assertImportFails(Response.Status status) {
        return assertImportFails(status, ErrorRepresentation.class);
    }

    private <T> T assertImportFails(Response.Status status, Class<T> entity) {
        try (Response response = managedRealm.admin().partialImport(piRep)) {
            assertEquals(status.getStatusCode(), response.getStatus());
            return response.readEntity(entity);
        }
    }

    private static void assertStatus(Response.Status expected, Response response) {
        try (response) {
            assertEquals(expected.getStatusCode(), response.getStatus());
        }
    }

    private static OrganizationRepresentation organization(String name) {
        OrganizationRepresentation organization = new OrganizationRepresentation();
        organization.setName(name);
        return organization;
    }

    private static OrganizationRepresentation organization(String name, String alias) {
        OrganizationRepresentation organization = organization(name);
        organization.setAlias(alias);
        return organization;
    }

    private static IdentityProviderRepresentation broker(String alias) {
        IdentityProviderRepresentation broker = new IdentityProviderRepresentation();
        broker.setAlias(alias);
        broker.setProviderId(alias);
        broker.setEnabled(true);
        broker.setConfig(Map.of("clientId", alias, "clientSecret", "secret"));
        return broker;
    }

    private static GroupRepresentation group(String name) {
        GroupRepresentation group = new GroupRepresentation();
        group.setName(name);
        return group;
    }

    private static void createOrganizationWithBrokersAndGroups(RealmResource realm) {
        OrganizationRepresentation definition = organization("Acme Corp", "acme");
        definition.setDescription("Acme Corporation");
        definition.setRedirectUrl("https://acme.example/welcome");
        definition.setAttributes(Map.of("tier", List.of("gold")));
        definition.addDomain(new OrganizationDomainRepresentation("acme.example"));
        OrganizationResource organization = realm.organizations().get(ApiUtil.getCreatedId(realm.organizations().create(definition)));

        OrganizationIdentityProviderLinkRepresentation manualMembership = new OrganizationIdentityProviderLinkRepresentation();
        manualMembership.setAutoMembership(false);
        manualMembership.setMembershipType(MembershipType.UNMANAGED.name());
        linkBroker(realm, organization, "github", manualMembership);
        OrganizationIdentityProviderLinkRepresentation managedMembership = new OrganizationIdentityProviderLinkRepresentation();
        managedMembership.setAutoMembership(true);
        managedMembership.setMembershipType(MembershipType.MANAGED.name());
        linkBroker(realm, organization, "google", managedMembership);

        addGroupWithRoles(realm, organization);
        disableAndVerifyDomain(organization, "google");
    }

    private static void linkBroker(RealmResource realm, OrganizationResource organization, String alias, OrganizationIdentityProviderLinkRepresentation link) {
        assertStatus(Response.Status.CREATED, realm.identityProviders().create(broker(alias)));
        assertStatus(Response.Status.NO_CONTENT, organization.identityProviders().addIdentityProvider(alias));
        assertStatus(Response.Status.NO_CONTENT, organization.identityProviders().get(alias).update(link));
    }

    private static void addGroupWithRoles(RealmResource realm, OrganizationResource organization) {
        RoleRepresentation auditor = new RoleRepresentation();
        auditor.setName("acme-auditor");
        realm.roles().create(auditor);
        String accountUuid = realm.clients().findByClientId("account").get(0).getId();

        GroupRepresentation engineering = group("engineering");
        engineering.setAttributes(Map.of("team", List.of("platform")));
        OrganizationGroupResource created = organization.groups().group(ApiUtil.getCreatedId(organization.groups().addTopLevelGroup(engineering)));
        assertStatus(Response.Status.CREATED, created.addSubGroup(group("backend")));
        created.roles().realmLevel().add(List.of(realm.roles().get("acme-auditor").toRepresentation()));
        created.roles().clientLevel(accountUuid).add(List.of(realm.clients().get(accountUuid).roles().get("view-profile").toRepresentation()));
    }

    private static void disableAndVerifyDomain(OrganizationResource organization, String brokerAlias) {
        OrganizationRepresentation definition = organization.toRepresentation();
        definition.setEnabled(false);
        OrganizationDomainRepresentation domain = definition.getDomains().iterator().next();
        domain.setVerified(true);
        domain.setIdentityProviderAlias(brokerAlias);
        domain.setAutoRedirect(true);
        assertStatus(Response.Status.NO_CONTENT, organization.update(definition));
    }

    private static RealmRepresentation partialExport(ManagedRealm realm) {
        boolean groupsAndRoles = false;
        boolean clients = false;
        return realm.admin().partialExport(groupsAndRoles, clients);
    }

    private static List<String> organizationDefinitions(ManagedRealm realm) throws IOException {
        List<String> definitions = new ArrayList<>();
        for (OrganizationRepresentation organization : partialExport(realm).getOrganizations()) {
            definitions.add(JsonSerialization.writeValueAsString(withoutIds(organization)));
        }
        return definitions;
    }

    private static OrganizationRepresentation withoutIds(OrganizationRepresentation organization) {
        organization.setId(null);
        organization.getIdentityProviders().sort(Comparator.comparing(IdentityProviderRepresentation::getAlias));
        organization.getIdentityProviders().stream()
                .flatMap(broker -> broker.getOrganizationLinks().stream())
                .forEach(link -> link.setOrganizationId(null));
        clearIds(organization.getGroups());
        return organization;
    }

    private static void clearIds(List<GroupRepresentation> groups) {
        for (GroupRepresentation group : groups) {
            group.setId(null);
            group.setParentId(null);
            clearIds(group.getSubGroups());
        }
    }

    private static String organizationId(PartialImportResults results) {
        return results.getResults().stream()
                .filter(result -> result.getResourceType() == ResourceType.ORGANIZATION)
                .map(PartialImportResult::getId)
                .findFirst()
                .orElseThrow();
    }

    private String clientUuid(String clientId) {
        return managedRealm.admin().clients().findByClientId(clientId).get(0).getId();
    }

    private String addManagedMember(String organizationId) {
        String userId = ApiUtil.getCreatedId(managedRealm.admin().users().create(UserBuilder.create().username("alice").email("alice@acme.example").build()));
        String realmName = managedRealm.getName();
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            provider.addManagedMember(provider.getById(organizationId), session.users().getUserById(realm, userId));
        });
        return userId;
    }

    public static class OrganizationsRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder builder) {
            return builder.organizationsEnabled(true);
        }
    }
}
