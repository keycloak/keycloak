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

const usernameAttributeName = "username";
const emailAttributeName = "email";

describe("User profile tabs", () => {
  const realmName = "Realm_" + uuid();
  const attributeName = "Test";
  const attributeDisplayName = "Test display name";

  before(() => adminClient.createRealm(realmName));

  after(() => adminClient.deleteRealm(realmName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
    sidebarPage.goToRealmSettings();
  });

  describe("Attributes sub tab tests", () => {
    it("Goes to create attribute page", () => {
      getUserProfileTab();
      getAttributesTab();
      userProfileTab.clickOnCreateAttributeButton();
    });

    it("Completes new attribute form and performs cancel", () => {
      getUserProfileTab();
      getAttributesTab();
      userProfileTab
        .clickOnCreateAttributeButton()
        .setAttributeNames(attributeName, attributeDisplayName)
        .cancelAttributeCreation()
        .checkElementNotInList(attributeName);
    });

    it("Completes new attribute form and performs submit", () => {
      getUserProfileTab();
      getAttributesTab();
      userProfileTab
        .clickOnCreateAttributeButton()
        .setAttributeNames(attributeName, attributeDisplayName)
        .saveAttributeCreation()
        .assertNotificationSaved();
    });

    it("Modifies existing attribute and performs save", () => {
      const attrName = "ModifyTest";
      getUserProfileTab();
      createAttributeDefinition(attrName);

      userProfileTab
        .selectElementInList(attrName)
        .editAttribute("Edited display name")
        .saveAttributeCreation()
        .assertNotificationSaved();
    });

    it("Adds and removes validator to/from existing attribute and performs save", () => {
      getUserProfileTab();

      userProfileTab
        .selectElementInList(attributeName)
        .cancelAddingValidator(emailAttributeName);
      userProfileTab.addValidator(emailAttributeName);
      cy.get('tbody [data-label="Validator name"]').contains(
        emailAttributeName,
      );

      userProfileTab.cancelRemovingValidator();
      userProfileTab.removeValidator();
      cy.get(".kc-emptyValidators").contains("No validators.");
    });
  });

  describe("Attribute groups sub tab tests", () => {
    const group = "Test" + uuid();

    before(() => adminClient.addGroupToProfile(realmName, group));

    it("Deletes an attributes group", () => {
      getUserProfileTab();
      getAttributesGroupTab();
      listingPage.deleteItem(group);
      modalUtils.confirmModal();
      listingPage.itemExist(group, false);
    });
  });

  describe("Check attributes are displayed and editable on user create/edit", () => {
    it("Checks that not required attribute is not present when user is created with email as username and edit username set to disabled", () => {
      const attrName = "newAttribute1";

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setNoAttributePermissions(),
      );

      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      realmSettingsPage
        .assertSwitch(realmSettingsPage.emailAsUsernameSwitch, false)
        .assertSwitch(realmSettingsPage.editUsernameSwitch, false);

      // Create user
      sidebarPage.goToUsers();
      cy.wait(1000);
      createUserPage
        .goToCreateUser()
        .assertAttributeFieldExists(attrName, false)
        .setUsername(`testuser7-${uuid()}`)
        .create()
        .assertNotificationCreated()
        .assertAttributeFieldExists(attrName, false);

      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      listingPage.deleteItem(attrName);
      modalUtils.confirmModal();
      masthead.checkNotificationMessage("Attribute deleted");
    });

    it("Checks that not required attribute is not present when user is created/edited with email as username enabled", () => {
      const attrName = "newAttribute2";

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setNoAttributePermissions(),
      );

      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      realmSettingsPage
        .setSwitch(realmSettingsPage.emailAsUsernameSwitch, true)
        .assertSwitch(realmSettingsPage.emailAsUsernameSwitch, true)
        .assertSwitch(realmSettingsPage.editUsernameSwitch, false);

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .setAttributeValue(emailAttributeName, `testuser8-${uuid()}@gmail.com`)
        .assertAttributeFieldExists(attrName, false)
        .create()
        .assertNotificationCreated();

      // Edit user
      createUserPage
        .assertAttributeFieldExists(attrName, false)
        .setAttributeValue(emailAttributeName, `testuser9-${uuid()}@gmail.com`)
        .update()
        .assertNotificationUpdated();

      deleteAttributeDefinition(attrName);
    });

    it("Checks that not required attribute with permissions to view/edit is present when user is created", () => {
      const attrName = "newAttribute3";

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setAllAttributePermissions(),
      );

      sidebarPage.goToRealmSettings();
      realmSettingsPage.goToLoginTab();
      cy.wait(1000);
      realmSettingsPage
        .setSwitch(realmSettingsPage.emailAsUsernameSwitch, false)
        .assertSwitch(realmSettingsPage.emailAsUsernameSwitch, false)
        .setSwitch(realmSettingsPage.editUsernameSwitch, false)
        .assertSwitch(realmSettingsPage.editUsernameSwitch, false);

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .assertAttributeFieldExists(attrName, true)
        .setUsername(`testuser10-${uuid()}`)
        .create()
        .assertNotificationCreated()
        .assertAttributeFieldExists(attrName, true);

      deleteAttributeDefinition(attrName);
    });

    it("Checks that required attribute with permissions to view/edit is present and required when user is created", () => {
      const attrName = "newAttribute4";

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setAllAttributePermissions().setAttributeRequired(),
      );

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .assertAttributeLabel(attrName, attrName)
        .setUsername(`testuser11-${uuid()}`)
        .create()
        .assertValidationErrorRequired(attrName);

      createUserPage
        .setAttributeValue(attrName, "MyAttribute")
        .create()
        .assertNotificationCreated();

      deleteAttributeDefinition(attrName);
    });

    it("Checks that required attribute with permissions to view/edit is accepted when user is created", () => {
      const attrName = "newAttribute5";

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setAllAttributePermissions().setAttributeRequired(),
      );

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .assertAttributeLabel(attrName, attrName)
        .setUsername(`testuser12-${uuid()}`)
        .setAttributeValue(attrName, "MyAttribute")
        .create()
        .assertNotificationCreated();

      deleteAttributeDefinition(attrName);
    });

    it("Checks that attribute group is visible when user with existing attribute is created", () => {
      const group = "personalInfo";

      getUserProfileTab();
      getAttributesGroupTab()
        .clickOnCreatesAttributesGroupButton()
        .createAttributeGroup(group, group)
        .saveAttributesGroupCreation()
        .assertNotificationUpdated();

      getAttributesTab();
      userProfileTab
        .selectElementInList(usernameAttributeName)
        .setAttributeGroup(group)
        .saveAttributeCreation()
        .assertNotificationSaved();

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .assertGroupDisplayName(group, group)
        .setUsername(`testuser14-${uuid()}`)
        .create()
        .assertNotificationCreated();

      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      userProfileTab
        .selectElementInList(usernameAttributeName)
        .resetAttributeGroup()
        .saveAttributeCreation()
        .assertNotificationSaved();
    });

    it("Checks that attribute group is visible when user with a new attribute is created", () => {
      const group = "contact";
      const attrName = "address";

      getUserProfileTab();
      getAttributesGroupTab()
        .clickOnCreatesAttributesGroupButton()
        .createAttributeGroup(group, group)
        .saveAttributesGroupCreation()
        .assertNotificationUpdated();

      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer.setAllAttributePermissions(),
      );

      userProfileTab
        .selectElementInList(attrName)
        .setAttributeGroup(group)
        .saveAttributeCreation()
        .assertNotificationSaved();

      // Create user
      sidebarPage.goToUsers();
      const initialAttrValue = "MyNewAddress1";
      createUserPage
        .goToCreateUser()
        .assertGroupDisplayName(group, group)
        .assertAttributeLabel(attrName, attrName)
        .setUsername(`testuser13-${uuid()}`)
        .setAttributeValue(attrName, initialAttrValue)
        .create()
        .assertNotificationCreated()
        .assertAttributeValue(attrName, initialAttrValue);

      // Edit attribute
      const newAttrValue = "MyNewAddress2";
      createUserPage
        .setAttributeValue(attrName, newAttrValue)
        .update()
        .assertNotificationUpdated()
        .assertAttributeValue(attrName, newAttrValue);

      sidebarPage.goToRealmSettings();
      getUserProfileTab();
      getAttributesTab();
      userProfileTab
        .selectElementInList(attrName)
        .resetAttributeGroup()
        .saveAttributeCreation()
        .assertNotificationSaved();

      deleteAttributeDefinition(attrName);
    });

    it("Checks that attribute with select-annotation is displayed and editable when user is created/edited", () => {
      const userName = `select-test-user-${uuid()}`;
      const attrName = "select-test-attr";
      const opt1 = "opt1";
      const opt2 = "opt2";
      const supportedOptions = [opt1, opt2];

      getUserProfileTab();
      createAttributeDefinition(attrName, (attrConfigurer) =>
        attrConfigurer
          .setAllAttributePermissions()
          .clickAddValidator()
          .selectValidatorType("options")
          .setListFieldValues("options", supportedOptions)
          .clickSave(),
      );

      // Create user
      sidebarPage.goToUsers();
      createUserPage
        .goToCreateUser()
        .assertAttributeLabel(attrName, attrName)
        .assertAttributeSelect(attrName, supportedOptions, "Select an option")
        .setUsername(userName)
        .setAttributeValueOnSelect(attrName, opt1)
        .create()
        .assertNotificationCreated()
        .assertAttributeLabel(attrName, attrName)
        .assertAttributeSelect(attrName, supportedOptions, opt1);

      // Edit attribute
      createUserPage
        .setAttributeValueOnSelect(attrName, opt2)
        .update()
        .assertNotificationUpdated()
        .assertAttributeLabel(attrName, attrName)
        .assertAttributeSelect(attrName, supportedOptions, opt2);
    });
  });

  function deleteAttributeDefinition(attrName: string) {
    sidebarPage.goToRealmSettings();
    getUserProfileTab();
    getAttributesTab();
    listingPage.deleteItem(attrName);
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("Attribute deleted");
  }

  function createAttributeDefinition(
    attrName: string,
    attrConfigurer?: (attrConfigurer: UserProfile) => void,
  ) {
    userProfileTab
      .goToAttributesTab()
      .clickOnCreateAttributeButton()
      .setAttributeNames(attrName, attrName);

    if (attrConfigurer) {
      attrConfigurer(userProfileTab);
    }

    userProfileTab.saveAttributeCreation().assertNotificationSaved();
  }
});
