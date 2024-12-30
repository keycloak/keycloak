import AttributesTab from "../support/pages/admin-ui/manage/AttributesTab";
import ClientRolesTab from "../support/pages/admin-ui/manage/clients/ClientRolesTab";
import AssociatedRolesPage from "../support/pages/admin-ui/manage/realm_roles/AssociatedRolesPage";
import createRealmRolePage from "../support/pages/admin-ui/manage/realm_roles/CreateRealmRolePage";
import CommonPage from "../support/pages/CommonPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const associatedRolesPage = new AssociatedRolesPage();
const attributesTab = new AttributesTab();
const commonPage = new CommonPage();
const loginPage = new LoginPage();

describe("Roles tab test", () => {
  const realmName = `clients-realm-${crypto.randomUUID()}`;
  const itemId = `client_crud${crypto.randomUUID()}`;

  const rolesTab = new ClientRolesTab();
  const client = "client_" + crypto.randomUUID();
  const createRealmRoleName = `create-realm-${crypto.randomUUID()}`;

  before(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.inRealm(realmName, () =>
      adminClient.createClient({
        clientId: client,
        protocol: "openid-connect",
        publicClient: false,
      }),
    );
    await adminClient.inRealm(realmName, () =>
      adminClient.createRealmRole({
        name: createRealmRoleName,
      }),
    );
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    commonPage.sidebar().goToRealm(realmName);
    commonPage.sidebar().goToClients();
    commonPage.tableToolbarUtils().searchItem(client);
    commonPage.tableUtils().clickRowItemLink(client);
    rolesTab.goToRolesTab();
  });

  after(() => adminClient.deleteRealm(realmName));

  it("Should fail to create client role with empty name", () => {
    rolesTab.goToCreateRoleFromEmptyState();
    createRealmRolePage.fillRealmRoleData("").save();
    createRealmRolePage.checkRealmRoleNameRequiredMessage();
  });

  it("Should create client role", () => {
    rolesTab.goToCreateRoleFromEmptyState();
    createRealmRolePage.fillRealmRoleData(itemId).save();
    commonPage.masthead().checkNotificationMessage("Role created", true);
  });

  it("Should update client role description", () => {
    const updateDescription = "updated description";
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().clickRowItemLink(itemId);
    createRealmRolePage.updateDescription(updateDescription).save();
    commonPage
      .masthead()
      .checkNotificationMessage("The role has been saved", true);
    createRealmRolePage.checkDescription(updateDescription);
  });

  it("Should add attribute to client role", () => {
    commonPage.tableUtils().clickRowItemLink(itemId);
    rolesTab.goToAttributesTab();
    attributesTab
      .addAttribute("crud_attribute_key", "crud_attribute_value")
      .save();
    attributesTab.assertRowItemsEqualTo(1);
    commonPage
      .masthead()
      .checkNotificationMessage("The role has been saved", true);
  });

  it("Should delete attribute from client role", () => {
    commonPage.tableUtils().clickRowItemLink(itemId);
    rolesTab.goToAttributesTab();
    attributesTab.deleteAttribute(0);
    attributesTab.assertEmpty();
    commonPage
      .masthead()
      .checkNotificationMessage("The role has been saved", true);
  });

  it("Should create client role to be deleted", () => {
    rolesTab.goToCreateRoleFromToolbar();
    createRealmRolePage.fillRealmRoleData("client_role_to_be_deleted").save();
    commonPage.masthead().checkNotificationMessage("Role created", true);
  });

  it("Should fail to create duplicate client role", () => {
    rolesTab.goToCreateRoleFromToolbar();
    createRealmRolePage.fillRealmRoleData(itemId).save();
    commonPage
      .masthead()
      .checkNotificationMessage(
        `Could not create role: Role with name ${itemId} already exists`,
        true,
      );
  });

  it("Should search existing client role", () => {
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().checkRowItemExists(itemId);
  });

  it("Should search non-existing role test", () => {
    commonPage.tableToolbarUtils().searchItem("role_DNE", false);
    commonPage.emptyState().checkIfExists(true);
  });

  it("roles empty search test", () => {
    commonPage.tableToolbarUtils().searchItem("", false);
    commonPage.tableUtils().checkIfExists(true);
  });

  it("Add associated roles test", () => {
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().clickRowItemLink(itemId);

    // Add associated realm role
    associatedRolesPage.addAssociatedRealmRole(createRealmRoleName);
    commonPage
      .masthead()
      .checkNotificationMessage("Associated roles have been added", true);

    // Add associated client role
    associatedRolesPage.addAssociatedRoleFromSearchBar("manage-account", true);
    commonPage
      .masthead()
      .checkNotificationMessage("Associated roles have been added", true);

    rolesTab.goToAssociatedRolesTab();

    // Add associated client role
    associatedRolesPage.addAssociatedRoleFromSearchBar("manage-consent", true);
    commonPage
      .masthead()
      .checkNotificationMessage("Associated roles have been added", true);
  });

  it("Should hide inherited roles test", () => {
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().clickRowItemLink(itemId);
    rolesTab.goToAssociatedRolesTab().hideInheritedRoles();
  });

  it("Should delete associated roles test", () => {
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().clickRowItemLink(itemId);
    rolesTab.goToAssociatedRolesTab();
    commonPage
      .tableUtils()
      .selectRowItemAction(createRealmRoleName, "Unassign");
    commonPage.sidebar().waitForPageLoad();
    commonPage.modalUtils().checkModalTitle("Remove role?").confirmModal();
    commonPage.sidebar().waitForPageLoad();

    commonPage
      .masthead()
      .checkNotificationMessage("Role mapping updated", true);

    commonPage.tableUtils().selectRowItemAction("manage-consent", "Unassign");
    commonPage.sidebar().waitForPageLoad();
    commonPage.modalUtils().checkModalTitle("Remove role?").confirmModal();
  });

  it("Should delete associated role from search bar test", () => {
    commonPage.tableToolbarUtils().searchItem(itemId, false);
    commonPage.tableUtils().clickRowItemLink(itemId);
    commonPage.sidebar().waitForPageLoad();
    rolesTab.goToAssociatedRolesTab();

    cy.get("td")
      .contains("manage-account")
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    associatedRolesPage.removeAssociatedRoles();

    commonPage.sidebar().waitForPageLoad();
    commonPage.modalUtils().checkModalTitle("Remove role?").confirmModal();
    commonPage.sidebar().waitForPageLoad();

    commonPage
      .masthead()
      .checkNotificationMessage("Role mapping updated", true);
  });

  it("Should delete client role test", () => {
    commonPage.tableUtils().selectRowItemAction(itemId, "Delete");
    commonPage.sidebar().waitForPageLoad();
    commonPage.modalUtils().checkModalTitle("Delete role?").confirmModal();
  });

  it("Should delete client role from role details test", () => {
    commonPage
      .tableToolbarUtils()
      .searchItem("client_role_to_be_deleted", false);
    commonPage.tableUtils().clickRowItemLink("client_role_to_be_deleted");
    createRealmRolePage.clickActionMenu("Delete this role");
    commonPage.modalUtils().confirmModal();
    commonPage
      .masthead()
      .checkNotificationMessage("The role has been deleted", true);
  });
});
