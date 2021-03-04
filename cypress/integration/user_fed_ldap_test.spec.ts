import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateLdapProviderPage from "../support/pages/admin_console/manage/providers/CreateLdapProviderPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new CreateLdapProviderPage();
const modalUtils = new ModalUtils();

const firstLdapName = "my-ldap";
const firstLdapVendor = "Active Directory";

const connectionUrl = "ldap://";
const firstBindType = "simple";
const firstBindDn = "user-1";
const firstBindCreds = "password1";

const firstUsersDn = "user-dn-1";
const firstUserLdapAtt = "uid";
const firstRdnLdapAtt = "uid";
const firstUuidLdapAtt = "entryUUID";
const firstUserObjClasses = "inetOrgPerson, organizationalPerson";

const secondLdapName = `${firstLdapName}-2`;
const secondLdapVendor = "Other";

const secondBindType = "none";
const secondBindDn = "user-2";
const secondBindCreds = "password2";

const secondUsersDn = "user-dn-2";
const secondUserLdapAtt = "cn";
const secondRdnLdapAtt = "cn";
const secondUuidLdapAtt = "objectGUID";
const secondUserObjClasses = "person, organizationalPerson, user";

const defaultPolicy = "DEFAULT";
const newPolicy = "EVICT_WEEKLY";
const defaultLdapDay = "Sunday";
const defaultLdapHour = "00";
const defaultLdapMinute = "00";
const newLdapDay = "Wednesday";
const newLdapHour = "15";
const newLdapMinute = "55";

const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";

describe("User Fed LDAP tests", () => {
  beforeEach(() => {
  /* 
    Prevent unpredictable 401 errors from failing individual tests.
    These are most often occurring during the login process:
       GET /admin/serverinfo/
       GET /admin/master/console/whoami
  */
    cy.on("uncaught:exception", (err, runnable) => {
      return false;
    });
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();
  });

  it("Create Ldap provider from empty state", () => {
    providersPage.clickNewCard("ldap");
    providersPage.fillLdapRequiredGeneralData(firstLdapName, firstLdapVendor);
    providersPage.fillLdapRequiredConnectionData(
      connectionUrl,
      firstBindType,
      firstBindDn,
      firstBindCreds
    );
    providersPage.fillLdapRequiredSearchingData(
      firstUsersDn,
      firstUserLdapAtt,
      firstRdnLdapAtt,
      firstUuidLdapAtt,
      firstUserObjClasses
    );

    providersPage.save();

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Update an existing LDAP provider and save", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeTime(defaultLdapDay, newLdapDay);
    providersPage.changeTime(defaultLdapHour, newLdapHour);
    providersPage.changeTime(defaultLdapMinute, newLdapMinute);

    providersPage.save();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    cy.wait(1000);

    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(newPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Change existing LDAP provider and click button to cancel", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeTime(newLdapDay, defaultLdapDay);
    providersPage.changeTime(newLdapHour, defaultLdapHour);
    providersPage.changeTime(newLdapMinute, defaultLdapMinute);

    providersPage.cancel();
    cy.wait(1000);

    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(newPolicy);

    expect(cy.contains(newLdapDay).should("exist"));
    expect(cy.contains(newLdapHour).should("exist"));
    expect(cy.contains(newLdapMinute).should("exist"));
    expect(cy.contains(defaultLdapMinute).should("not.exist"));

    sidebarPage.goToUserFederation();
  });

  it("Disable an existing LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.disableEnabledSwitch();

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    cy.wait(1000);
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Enable an existing previously-disabled LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.enableEnabledSwitch();

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    cy.wait(1000);
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new LDAP provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand("Add new provider", "LDAP");
    providersPage.fillLdapRequiredGeneralData(secondLdapName, secondLdapVendor);
    providersPage.fillLdapRequiredConnectionData(
      connectionUrl,
      secondBindType,
      secondBindDn,
      secondBindCreds
    );
    providersPage.fillLdapRequiredSearchingData(
      secondUsersDn,
      secondUserLdapAtt,
      secondRdnLdapAtt,
      secondUuidLdapAtt,
      secondUserObjClasses
    );
    providersPage.save();
    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Delete an LDAP provider from card view using the card's menu", () => {
    providersPage.deleteCardFromCard(secondLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Delete an LDAP provider using the Settings view's Action menu", () => {
    providersPage.deleteCardFromMenu(firstLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
