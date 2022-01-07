import ListingPage from "../support/pages/admin_console/ListingPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import AdminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_before";
import ModalUtils from "../support/util/ModalUtils";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const realmSettingsPage = new RealmSettingsPage();
const adminClient = new AdminClient();
const listingPage = new ListingPage();
const modalUtils = new ModalUtils();

// Selectors
const getUserProfileTab = () =>
  cy.findByTestId(realmSettingsPage.userProfileTab);
const getAttributesGroupTab = () => cy.findByTestId("attributesGroupTab");

describe("User profile tabs", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

  before(() =>
    adminClient.createRealm(realmName, {
      attributes: { userProfileEnabled: "true" },
    })
  );

  after(() => adminClient.deleteRealm(realmName));

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealm(realmName);
    sidebarPage.goToRealmSettings();
  });

  describe("Attribute groups", () => {
    it("deletes an attributes group", () => {
      cy.wrap(null).then(() =>
        adminClient.patchUserProfile(realmName, {
          groups: [{ name: "Test" }],
        })
      );

      getUserProfileTab().click();
      getAttributesGroupTab().click();
      listingPage.deleteItem("Test");
      modalUtils.confirmModal();
      listingPage.itemExist("Test", false);
    });
  });
});
