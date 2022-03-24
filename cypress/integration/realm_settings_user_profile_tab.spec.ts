import ListingPage from "../support/pages/admin_console/ListingPage";
import UserProfile from "../support/pages/admin_console/manage/realm_settings/UserProfile";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ModalUtils from "../support/util/ModalUtils";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const userProfileTab = new UserProfile();
const listingPage = new ListingPage();
const modalUtils = new ModalUtils();

// Selectors
const getUserProfileTab = () => userProfileTab.goToTab();
const getAttributesTab = () => userProfileTab.goToAttributesTab();
const getAttributesGroupTab = () => userProfileTab.goToAttributesGroupTab();
const getJsonEditorTab = () => userProfileTab.goToJsonEditorTab();
const clickCreateAttributeButton = () =>
  userProfileTab.createAttributeButtonClick();

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

  describe("Attributes sub tab tests", () => {
    it("Goes to create attribute page", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      cy.get("p").should("have.text", "Create a new attribute");
    });
  });

  describe("Attribute groups sub tab tests", () => {
    it("Deletes an attributes group", () => {
      cy.wrap(null).then(() =>
        adminClient.patchUserProfile(realmName, {
          groups: [{ name: "Test" }],
        })
      );

      getUserProfileTab();
      getAttributesGroupTab();
      listingPage.deleteItem("Test");
      modalUtils.confirmModal();
      listingPage.itemExist("Test", false);
    });
  });

  describe("Json Editor sub tab tests", () => {
    it("Goes to Json Editor tab", () => {
      getUserProfileTab();
      getJsonEditorTab();
    });
  });
});
