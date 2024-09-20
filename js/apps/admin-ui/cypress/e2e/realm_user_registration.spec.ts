import ListingPage from "../support/pages/admin-ui/ListingPage";
import ClientRolesTab from "../support/pages/admin-ui/manage/clients/ClientRolesTab";
import UserRegistration, {
  GroupPickerDialog,
} from "../support/pages/admin-ui/manage/realm_settings/UserRegistration";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ModalUtils from "../support/util/ModalUtils";

describe("Realm settings - User registration tab", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const modalUtils = new ModalUtils();
  const masthead = new Masthead();

  const listingPage = new ListingPage();
  const groupPicker = new GroupPickerDialog();
  const userRegistration = new UserRegistration();
  const rolesTab = new ClientRolesTab();

  const groupName = "The default group";

  before(() => adminClient.createGroup(groupName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealmSettings();
    userRegistration.goToTab();
  });

  after(() => adminClient.deleteGroups());

  it("Add admin role", () => {
    const role = "admin";
    const roleType = "roles";
    userRegistration.addRole();
    sidebarPage.waitForPageLoad();
    userRegistration.changeRoleTypeFilter(roleType).selectRow(role).assign();
    masthead.checkNotificationMessage("Associated roles have been added");
    listingPage.searchItem(role, false).itemExist(role);

    sidebarPage.goToRealmRoles();
    listingPage.goToItemDetails("admin");
    rolesTab.goToUsersInRoleTab();
    cy.findByTestId("users-in-role-table").contains("admin");
  });

  it("Remove admin role", () => {
    const role = "admin";
    listingPage.markItemRow(role).removeMarkedItems("Unassign");
    sidebarPage.waitForPageLoad();
    modalUtils
      .checkModalTitle("Remove role?")
      .checkModalMessage("Are you sure you want to remove this role?")
      .checkConfirmButtonText("Remove")
      .confirmModal();
    masthead.checkNotificationMessage("Scope mapping successfully removed");
  });

  it("Add default group", () => {
    userRegistration.goToDefaultGroupTab().addDefaultGroup();
    groupPicker.checkTitle("Add default groups").clickRow(groupName).clickAdd();
    masthead.checkNotificationMessage("New group added to the default groups");
    listingPage.itemExist(groupName);
  });
});
