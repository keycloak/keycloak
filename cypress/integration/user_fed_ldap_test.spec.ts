import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import ProviderPage from "../support/pages/admin_console/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new ProviderPage();
const modalUtils = new ModalUtils();

const provider = "ldap";
const allCapProvider = provider.toUpperCase();

const firstLdapName = "my-ldap";
const firstLdapVendor = "Active Directory";

const connectionUrl = "ldap://";
const firstBindType = "simple";
const firstBindDn = "user-1";
const firstBindCreds = "password1";

const firstEditMode = "READ_ONLY";
const firstUsersDn = "user-dn-1";
const firstUserLdapAtt = "uid";
const firstRdnLdapAtt = "uid";
const firstUuidLdapAtt = "entryUUID";
const firstUserObjClasses = "inetOrgPerson, organizationalPerson";

const secondLdapName = `${firstLdapName}-2`;
const secondLdapVendor = "Other";

const secondBindType = "none";

const secondEditMode = "WRITABLE";
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

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";

describe("User Fed LDAP tests", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToUserFederation();
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
    providersPage.fillLdapRequiredGeneralData(firstLdapName, firstLdapVendor);
    providersPage.fillLdapRequiredConnectionData(
      connectionUrl,
      firstBindType,
      firstBindDn,
      firstBindCreds
    );
    providersPage.fillLdapRequiredSearchingData(
      firstEditMode,
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

  it("Change existing LDAP provider and click button to cancel", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeCacheTime("day", defaultLdapDay);
    providersPage.changeCacheTime("hour", defaultLdapHour);
    providersPage.changeCacheTime("minute", defaultLdapMinute);

    providersPage.cancel(provider);
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
    providersPage.disableEnabledSwitch(allCapProvider);

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Enable an existing previously-disabled LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.enableEnabledSwitch(allCapProvider);

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new LDAP provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand(addProviderMenu, allCapProvider);
    providersPage.fillLdapRequiredGeneralData(secondLdapName, secondLdapVendor);
    providersPage.fillLdapRequiredConnectionData(connectionUrl, secondBindType);
    providersPage.fillLdapRequiredSearchingData(
      secondEditMode,
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
    providersPage.deleteCardFromMenu(provider, firstLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
