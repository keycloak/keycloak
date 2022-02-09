import ListingPage from "../support/pages/admin_console/ListingPage";
import UserRegistration, {
  GroupPickerDialog,
} from "../support/pages/admin_console/manage/realm_settings/UserRegistration";
import Masthead from "../support/pages/admin_console/Masthead";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import AdminClient from "../support/util/AdminClient";
import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";

describe("Realm settings - User registration tab", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const masthead = new Masthead();
  const adminClient = new AdminClient();

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
    keycloakBeforeEach();
    sidebarPage.goToRealmSettings();
    userRegistration.goToTab();
  });

  after(() => adminClient.deleteGroups());

  it("add default role", () => {
    const role = "admin";
    userRegistration.addRoleButtonClick();
    userRegistration.selectRow(role).assign();
    masthead.checkNotificationMessage("Associated roles have been added");
    listingPage.searchItem(role, false).itemExist(role);
  });

  it("add default role", () => {
    userRegistration.goToDefaultGroupTab().addDefaultGroupClick();
    groupPicker.checkTitle("Add default groups").clickRow(groupName).clickAdd();
    masthead.checkNotificationMessage("New group added to the default groups");
    listingPage.itemExist(groupName);
  });
});
