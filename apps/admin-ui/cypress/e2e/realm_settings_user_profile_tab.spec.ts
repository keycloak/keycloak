import ListingPage from "../support/pages/admin_console/ListingPage";
import UserProfile from "../support/pages/admin_console/manage/realm_settings/UserProfile";
import Masthead from "../support/pages/admin_console/Masthead";
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
const masthead = new Masthead();

// Selectors
const getUserProfileTab = () => userProfileTab.goToTab();
const getAttributesTab = () => userProfileTab.goToAttributesTab();
const getAttributesGroupTab = () => userProfileTab.goToAttributesGroupTab();
const getJsonEditorTab = () => userProfileTab.goToJsonEditorTab();
const clickCreateAttributeButton = () =>
  userProfileTab.createAttributeButtonClick();

describe("User profile tabs", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);
  const attributeName = "Test";

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

    it("Completes new attribute form and performs cancel", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttribute(attributeName, "Test display name")
        .cancelAttributeCreation()
        .checkElementNotInList(attributeName);
    });

    it("Completes new attribute form and performs submit", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttribute(attributeName, "Display name")
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved."
      );
    });

    it("Modifies existing attribute and performs save", () => {
      getUserProfileTab();
      getAttributesTab();
      userProfileTab
        .selectElementInList(attributeName)
        .editAttribute("Edited display name")
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved."
      );
    });

    it("Adds and removes validator to/from existing attribute and performs save", () => {
      getUserProfileTab();
      getAttributesTab();
      userProfileTab.selectElementInList(attributeName).cancelAddingValidator();
      userProfileTab.addValidator();
      cy.get('tbody [data-label="Validator name"]').contains("email");

      userProfileTab.cancelRemovingValidator();
      userProfileTab.removeValidator();
      cy.get(".kc-emptyValidators").contains("No validators.");
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
