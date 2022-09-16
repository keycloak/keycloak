import ListingPage from "../support/pages/admin_console/ListingPage";
import UserRegistration, {
  GroupPickerDialog,
} from "../support/pages/admin_console/manage/realm_settings/UserRegistration";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
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

  const groupName = "The default group";

  before(() => {
    adminClient.createGroup(groupName);
    keycloakBefore();
    loginPage.logIn();
  });

  beforeEach(() => {
    sidebarPage.goToRealmSettings();
    userRegistration.goToTab();
  });

  after(() => adminClient.deleteGroups());

  it("Add admin role", () => {
    const role = "admin";
    userRegistration.addRole();
    sidebarPage.waitForPageLoad();
    userRegistration.selectRow(role).assign();
    masthead.checkNotificationMessage("Associated roles have been added");
    listingPage.searchItem(role, false).itemExist(role);
  });

  it("Remove admin role", () => {
    const role = "admin";
    listingPage.markItemRow(role).removeMarkedItems("Unassign");
    sidebarPage.waitForPageLoad();
    modalUtils
      .checkModalTitle("Remove mapping?")
      .checkModalMessage("Are you sure you want to remove this mapping?")
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
