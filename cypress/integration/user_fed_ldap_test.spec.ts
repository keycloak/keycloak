import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import ProviderPage from "../support/pages/admin_console/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new ProviderPage();
const modalUtils = new ModalUtils();

const provider = "ldap";
const allCapProvider = provider.toUpperCase();

const firstLdapName = "my-ldap";
const firstLdapVendor = "Active Directory";

// connection and authentication settings
const connectionUrlValid = "ldap://ldap.forumsys.com:389";
const bindTypeSimple = "simple";
const truststoreSpiOnlyLdaps = "Only for ldaps";
const connectionTimeoutTwoSecs = "2000";
const bindDnCnDc = "cn=read-only-admin,dc=example,dc=com";
const bindCredsValid = "password";

const connectionUrlInvalid = "ldap://nowhere.com";
const bindTypeNone = "none";
const truststoreSpiNever = "Never";
const bindDnCnOnly = "cn=read-only-admin";
const bindCredsInvalid = "not-my-password";

// ldap searching and updating
const editModeReadOnly = "READ_ONLY";
const firstUsersDn = "user-dn-1";
const firstUserLdapAtt = "uid";
const firstRdnLdapAtt = "uid";
const firstUuidLdapAtt = "entryUUID";
const firstUserObjClasses = "inetOrgPerson, organizationalPerson";

const secondLdapName = `${firstLdapName}-2`;
const secondLdapVendor = "Other";

const editModeWritable = "WRITABLE";

const secondUsersDn = "user-dn-2";
const secondUserLdapAtt = "cn";
const secondRdnLdapAtt = "cn";
const secondUuidLdapAtt = "objectGUID";
const secondUserObjClasses = "person, organizationalPerson, user";

const updatedLdapName = `${firstLdapName}-updated`;

const defaultPolicy = "DEFAULT";
const newPolicy = "EVICT_WEEKLY";
const defaultLdapDay = "Sunday";
const defaultLdapHour = "00";
const defaultLdapMinute = "00";
const newLdapDay = "Wednesday";
const newLdapHour = "15";
const newLdapMinute = "55";

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";

const ldapTestSuccessMsg = "Successfully connected to LDAP";
const ldapTestFailMsg =
  "Error when trying to connect to LDAP. See server.log for details. LDAP test error";

describe("User Fed LDAP tests", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToUserFederation();
    cy.intercept("GET", "/admin/realms/master").as("getProvider");
  });

  it("Create Ldap provider from empty state", () => {
    // if tests don't start at empty state, e.g. user has providers configured locally,
    // create a new card from the card view instead
    cy.get("body").then(($body) => {
      if ($body.find(`[data-testid=ldap-card]`).length > 0) {
        providersPage.clickNewCard(provider);
      } else {
        providersPage.clickMenuCommand(addProviderMenu, allCapProvider);
      }
    });
    providersPage.fillLdapGeneralData(firstLdapName, firstLdapVendor);
    providersPage.fillLdapConnectionData(
      connectionUrlInvalid,
      bindTypeSimple,
      truststoreSpiNever,
      connectionTimeoutTwoSecs,
      bindDnCnOnly,
      bindCredsInvalid
    );
    providersPage.fillLdapSearchingData(
      editModeReadOnly,
      firstUsersDn,
      firstUserLdapAtt,
      firstRdnLdapAtt,
      firstUuidLdapAtt,
      firstUserObjClasses
    );
    providersPage.save(provider);
    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Update an existing LDAP provider and save", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeCacheTime("day", newLdapDay);
    providersPage.changeCacheTime("hour", newLdapHour);
    providersPage.changeCacheTime("minute", newLdapMinute);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(newPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Update connection and authentication settings and save", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.fillLdapConnectionData(
      connectionUrlInvalid,
      bindTypeNone,
      truststoreSpiNever,
      connectionTimeoutTwoSecs
    );
    providersPage.toggleSwitch(providersPage.enableStartTls);
    providersPage.toggleSwitch(providersPage.connectionPooling);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    // now verify
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);
    providersPage.verifyTextField(
      providersPage.connectionUrlInput,
      connectionUrlInvalid
    );
    providersPage.verifyTextField(
      providersPage.connectionTimeoutInput,
      connectionTimeoutTwoSecs
    );
    providersPage.verifySelect(
      providersPage.truststoreSpiInput,
      truststoreSpiNever
    );
    providersPage.verifySelect(providersPage.bindTypeInput, bindTypeNone);
    providersPage.verifyToggle(providersPage.enableStartTls, "on");
    providersPage.verifyToggle(providersPage.connectionPooling, "on");
    sidebarPage.goToUserFederation();
  });

  it("Should fail connection and authentication tests", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.testConnection();
    masthead.checkNotificationMessage(ldapTestFailMsg);

    providersPage.testAuthorization();
    masthead.checkNotificationMessage(ldapTestFailMsg);

    sidebarPage.goToUserFederation();
  });

  it("Should make changes and pass connection and authentication tests", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.fillLdapConnectionData(
      connectionUrlValid,
      bindTypeSimple,
      truststoreSpiOnlyLdaps,
      connectionTimeoutTwoSecs,
      bindDnCnDc,
      bindCredsValid
    );
    providersPage.toggleSwitch(providersPage.enableStartTls);
    providersPage.toggleSwitch(providersPage.connectionPooling);

    providersPage.save(provider);

    providersPage.testConnection();
    masthead.checkNotificationMessage(ldapTestSuccessMsg);

    providersPage.testAuthorization();
    masthead.checkNotificationMessage(ldapTestSuccessMsg);

    sidebarPage.goToUserFederation();
  });

  it("Should update display name", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.fillLdapGeneralData(updatedLdapName);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(updatedLdapName);

    sidebarPage.goToUserFederation();
  });

  it("Change existing LDAP provider and click button to cancel", () => {
    providersPage.clickExistingCard(updatedLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeCacheTime("day", defaultLdapDay);
    providersPage.changeCacheTime("hour", defaultLdapHour);
    providersPage.changeCacheTime("minute", defaultLdapMinute);

    providersPage.cancel(provider);

    providersPage.clickExistingCard(updatedLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.verifyChangedHourInput(newLdapHour, defaultLdapHour);

    sidebarPage.goToUserFederation();
  });

  it("Should disable an existing LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    cy.wait("@getProvider");
    providersPage.disableEnabledSwitch(allCapProvider);
    modalUtils.checkModalTitle(disableModalTitle).confirmModal();
    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Should enable a previously-disabled LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    cy.wait("@getProvider");
    providersPage.enableEnabledSwitch(allCapProvider);
    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new LDAP provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand(addProviderMenu, allCapProvider);
    providersPage.fillLdapGeneralData(secondLdapName, secondLdapVendor);
    providersPage.fillLdapConnectionData(
      connectionUrlValid,
      bindTypeSimple,
      truststoreSpiNever,
      connectionTimeoutTwoSecs,
      bindDnCnOnly,
      bindCredsInvalid
    );
    providersPage.fillLdapSearchingData(
      editModeWritable,
      secondUsersDn,
      secondUserLdapAtt,
      secondRdnLdapAtt,
      secondUuidLdapAtt,
      secondUserObjClasses
    );
    providersPage.save(provider);
    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Delete an LDAP provider from card view using the card's menu", () => {
    providersPage.deleteCardFromCard(secondLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Delete an LDAP provider using the Settings view's Action menu", () => {
    providersPage.deleteCardFromMenu(updatedLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
