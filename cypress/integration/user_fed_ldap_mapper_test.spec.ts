import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import ListingPage from "../support/pages/admin_console/ListingPage";
import ProviderPage from "../support/pages/admin_console/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_before";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();

const providersPage = new ProviderPage();
const modalUtils = new ModalUtils();

const provider = "ldap";
const allCapProvider = provider.toUpperCase();

const ldapName = "ldap-mappers-testing";
const ldapVendor = "Active Directory";

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

const addProviderMenu = "Add new provider";
const providerCreatedSuccess = "User federation provider successfully created";
const mapperCreatedSuccess = "Mapping successfully created";
const mapperUpdatedSuccess = "Mapping successfully updated";
const providerDeleteSuccess = "The user federation provider has been deleted.";
const providerDeleteTitle = "Delete user federation provider?";
const mapperDeletedSuccess = "Mapping successfully deleted";
const mapperDeleteTitle = "Delete mapping?";

// mapperType variables
const msadUserAcctMapper = "msad-user-account-control-mapper";
const msadLdsUserAcctMapper = "msad-lds-user-account-control-mapper";
const userAttLdapMapper = "user-attribute-ldap-mapper";
const certLdapMapper = "certificate-ldap-mapper";
const fullNameLdapMapper = "full-name-ldap-mapper";
const groupLdapMapper = "group-ldap-mapper";

// Used by "Delete default mappers" test
const creationDateMapper = "creation date";
const emailMapper = "email";
const lastNameMapper = "last name";
const modifyDateMapper = "modify date";
const usernameMapper = "username";
const firstNameMapper = "first name";
const MsadAccountControlsMapper = "MSAD account controls";

describe("User Fed LDAP mapper tests", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToUserFederation();
  });

  it("Create LDAP provider from empty state", () => {
    // if tests don't start at empty state, e.g. user has providers configured locally,
    // create a new card from the card view instead
    cy.get("body").then(($body) => {
      if ($body.find(`[data-testid=ldap-card]`).length > 0) {
        providersPage.clickNewCard(provider);
      } else {
        providersPage.clickMenuCommand(addProviderMenu, allCapProvider);
      }
    });
    providersPage.fillLdapRequiredGeneralData(ldapName, ldapVendor);
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
    masthead.checkNotificationMessage(providerCreatedSuccess);
    sidebarPage.goToUserFederation();
  });

  // delete default mappers
  it("Delete default mappers", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();

    listingPage.itemExist(creationDateMapper).deleteItem(creationDateMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(creationDateMapper, false);

    listingPage.itemExist(emailMapper).deleteItem(emailMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(emailMapper, false);

    listingPage.itemExist(lastNameMapper).deleteItem(lastNameMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(lastNameMapper, false);

    listingPage.itemExist(modifyDateMapper).deleteItem(modifyDateMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(modifyDateMapper, false);

    listingPage.itemExist(usernameMapper).deleteItem(usernameMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(usernameMapper, false);

    listingPage.itemExist(firstNameMapper).deleteItem(firstNameMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
    listingPage.itemExist(firstNameMapper, false);

    listingPage
      .itemExist(MsadAccountControlsMapper)
      .deleteItem(MsadAccountControlsMapper);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess, true);
  });

  // mapper CRUD tests
  // create mapper
  it("Create certificate ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(certLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(certLdapMapper, true);
  });

  // update mapper
  it("Update certificate ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();

    listingPage.goToItemDetails(`${certLdapMapper}-test`);
    providersPage.updateMapper(certLdapMapper);

    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperUpdatedSuccess);
  });

  // delete mapper
  it("Delete certificate ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();

    listingPage.deleteItem(`${certLdapMapper}-test`);
    modalUtils.checkModalTitle(mapperDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(mapperDeletedSuccess);
  });

  // create one of each mapper type
  it("Create user account control mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(msadUserAcctMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(msadUserAcctMapper, true);
  });

  it("Create msad lds user account control mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(msadLdsUserAcctMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(msadLdsUserAcctMapper, true);
  });

  it("Create certificate ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(certLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(certLdapMapper, true);
  });

  it("Create user attribute ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(userAttLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(userAttLdapMapper, true);
  });

  it("Create full name ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(fullNameLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(fullNameLdapMapper, true);
  });

  it("Create group ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(groupLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(groupLdapMapper, true);
  });

  // *** test cleanup ***
  it("Cleanup - delete LDAP provider", () => {
    providersPage.deleteCardFromMenu(ldapName);
    modalUtils.checkModalTitle(providerDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(providerDeleteSuccess);
  });
});
