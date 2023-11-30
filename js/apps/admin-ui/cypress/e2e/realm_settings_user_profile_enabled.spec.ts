import { v4 as uuid } from "uuid";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import UserProfile from "../support/pages/admin-ui/manage/realm_settings/UserProfile";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import ModalUtils from "../support/util/ModalUtils";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import CreateUserPage from "../support/pages/admin-ui/manage/users/CreateUserPage";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const userProfileTab = new UserProfile();
const listingPage = new ListingPage();
const modalUtils = new ModalUtils();
const masthead = new Masthead();
const realmSettingsPage = new RealmSettingsPage();
const createUserPage = new CreateUserPage();

// Selectors
const getUserProfileTab = () => userProfileTab.goToTab();
const getAttributesTab = () => userProfileTab.goToAttributesTab();
const getAttributesGroupTab = () => userProfileTab.goToAttributesGroupTab();
const getJsonEditorTab = () => userProfileTab.goToJsonEditorTab();
const clickCreateAttributeButton = () =>
  userProfileTab.createAttributeButtonClick();

describe("User profile tabs", () => {
  const realmName = "Realm_" + uuid();
  const attributeName = "Test";

  before(() =>
    adminClient.createRealm(realmName, {
      attributes: { userProfileEnabled: "true" },
    }),
  );

  after(() => adminClient.deleteRealm(realmName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
    sidebarPage.goToRealmSettings();
  });

  afterEach(() => {
    sidebarPage.goToRealmSettings();
    sidebarPage.waitForPageLoad();
    realmSettingsPage.goToLoginTab();
    cy.wait(1000);
    cy.findByTestId("email-as-username-switch").uncheck({ force: true });
    cy.findByTestId("edit-username-switch").uncheck({ force: true });
  });

  describe("Attributes sub tab tests", () => {
    it("Goes to create attribute page", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
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
        "Success! User Profile configuration has been saved.",
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
        "Success! User Profile configuration has been saved.",
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
        }),
      );

      getUserProfileTab();
      getAttributesGroupTab();
      listingPage.deleteItem("Test");
      modalUtils.confirmModal();
      listingPage.checkEmptyList();
    });
  });

  describe("Json Editor sub tab tests", () => {
    const removedThree = `
      {ctrl+a}{backspace}
  {
    "attributes": [
      {
  "name": "username",
  "validations": {
    "length": {
    "min": 3,
  "max": 255 {downArrow},
  "username-prohibited-characters": {
  `;

    it("Removes three validators with the editor", () => {
      getUserProfileTab();
      getJsonEditorTab();
      userProfileTab.typeJSON(removedThree).saveJSON();
      masthead.checkNotificationMessage(
        "User profile settings successfully updated.",
      );
    });
  });

  describe("Check attribute presence when creating a user based on email as username and edit username configs enabled/disabled", () => {
    it("Checks that not required attribute is not present when user is created with email as username and edit username set to disabled", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeNotRequiredWithoutPermissions(
          "newAttribute1",
          "newAttribute1",
        )
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      cy.findByTestId("email-as-username-switch").should("have.value", "off");
      cy.findByTestId("edit-username-switch").should("have.value", "off");
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser7");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute1")
        .should("not.exist");
      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem("newAttribute1");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });
    it("Checks that not required attribute is not present when user is created/edited with email as username enabled", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeNotRequiredWithoutPermissions(
          "newAttribute2",
          "newAttribute2",
        )
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      cy.findByTestId("email-as-username-switch").check({ force: true });
      cy.findByTestId("email-as-username-switch").should("have.value", "on");
      cy.findByTestId("edit-username-switch").should("have.value", "off");
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("email").type("testuser8@gmail.com");
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute2")
        .should("not.exist");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");
      // Edit user
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute2")
        .should("not.exist");
      cy.findByTestId("email").clear();
      cy.findByTestId("email").type("testuser9@gmail.com");
      cy.findByTestId("save-user").click();
      masthead.checkNotificationMessage("The user has been saved");
      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem("newAttribute2");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });
    it("Checks that not required attribute with permissions to view/edit is present when user is created", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeNotRequiredWithPermissions(
          "newAttribute3",
          "newAttribute3",
        )
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      cy.findByTestId("email-as-username-switch").should("have.value", "off");
      cy.findByTestId("edit-username-switch").should("have.value", "off");
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser10");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute3")
        .should("exist");
      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem("newAttribute3");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });
    //TODO this test doesn't seem to pass on CI
    it.skip("Checks that required attribute with permissions to view/edit is present and required when user is created", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeRequiredWithPermissions(
          "newAttribute4",
          "newAttribute4",
        )
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser11");
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute4")
        .should("exist");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage(
        "Could not create user: Please specify attribute newAttribute4.",
      );
      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem("newAttribute4");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });
    it("Checks that required attribute with permissions to view/edit is accepted when user is created", () => {
      getUserProfileTab();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeRequiredWithPermissions(
          "newAttribute5",
          "newAttribute5",
        )
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser12");
      cy.get(".pf-c-form__label-text")
        .contains("newAttribute5")
        .should("exist");
      cy.findByTestId("newAttribute5").type("MyAttribute");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");
      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem("newAttribute5");
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });
    it("Checks that attribute group is visible when user with existing attribute is created", () => {
      getUserProfileTab();
      getAttributesGroupTab();
      cy.findAllByTestId("no-attributes-groups-empty-action").click();
      userProfileTab.createAttributeGroup("personalInfo", "personalInfo");
      userProfileTab.saveAttributesGroupCreation();

      getAttributesTab();
      userProfileTab.selectElementInList("username");
      cy.get("#kc-attributeGroup").click();
      cy.get("button.pf-c-select__menu-item").contains("personalInfo").click();
      userProfileTab.saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser14");
      cy.get("h1#personalinfo").should("have.text", "personalInfo");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");

      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      userProfileTab.selectElementInList("username");
      cy.get("#kc-attributeGroup").click();
      cy.get("button.pf-c-select__menu-item").contains("None").click();
      userProfileTab.saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );

      getAttributesGroupTab();
      listingPage.deleteItem("personalInfo");
      modalUtils.confirmModal();
      listingPage.checkEmptyList();
    });
    it("Checks that attribute group is visible when user with a new attribute is created", () => {
      getUserProfileTab();
      getAttributesGroupTab();
      cy.findAllByTestId("no-attributes-groups-empty-action").click();
      userProfileTab.createAttributeGroup("contact", "contact");
      userProfileTab.saveAttributesGroupCreation();
      getAttributesTab();
      clickCreateAttributeButton();
      userProfileTab
        .createAttributeNotRequiredWithPermissions("address", "address")
        .saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      userProfileTab.selectElementInList("address");
      cy.get("#kc-attributeGroup").click();
      cy.get("button.pf-c-select__menu-item").contains("contact").click();
      userProfileTab.saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );
      // Create user
      sidebarPage.goToUsers();
      createUserPage.goToCreateUser();
      cy.findByTestId("username").type("testuser13");
      cy.get("h1#contact").should("have.text", "contact");
      cy.get(".pf-c-form__label-text").contains("address").should("exist");
      cy.findByTestId("address").type("MyNewAddress1");
      cy.findByTestId("create-user").click();
      masthead.checkNotificationMessage("The user has been created");
      cy.findByTestId("address").should("have.value", "MyNewAddress1");

      // Edit attribute
      cy.findByTestId("address").clear();
      cy.findByTestId("address").type("MyNewAddress2");
      cy.findByTestId("save-user").click();
      masthead.checkNotificationMessage("The user has been saved");
      cy.findByTestId("address").should("have.value", "MyNewAddress2");

      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      userProfileTab.selectElementInList("address");
      cy.get("#kc-attributeGroup").click();
      cy.get("button.pf-c-select__menu-item").contains("None").click();
      userProfileTab.saveAttributeCreation();
      masthead.checkNotificationMessage(
        "Success! User Profile configuration has been saved.",
      );

      getAttributesGroupTab();
      listingPage.deleteItem("contact");
      modalUtils.confirmModal();
      listingPage.checkEmptyList();
    });
  });
});
