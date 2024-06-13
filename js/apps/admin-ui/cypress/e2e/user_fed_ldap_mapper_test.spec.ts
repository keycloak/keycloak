import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import ProviderPage from "../support/pages/admin-ui/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";

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

// connection and authentication settings
const connectionUrlValid = "ldap://localhost:3004";
const bindTypeSimple = "simple";
const truststoreSpiAlways = "Always";
const connectionTimeoutTwoSecs = "2000";
const bindDnCnDc = "cn=user,dc=test";
const bindCredsValid = "user";

// ldap searching and updating
const editModeReadOnly = "READ_ONLY";
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
const fullNameLdapMapper = "full-name-ldap-mapper";
const groupLdapMapper = "group-ldap-mapper";
const certLdapMapper = "certificate-ldap-mapper";

const mapperNames = [
  `${msadUserAcctMapper}-test`,
  `${msadLdsUserAcctMapper}-test`,
  `${userAttLdapMapper}-test`,
  `${fullNameLdapMapper}-test`,
  `${groupLdapMapper}-test`,
];
const multiMapperNames = mapperNames.slice(2);
const singleMapperName = mapperNames.slice(4);
const uniqueSearchTerm = "group";
const multipleSearchTerm = "ldap";
const nonexistingSearchTerm = "redhat";

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
    loginPage.logIn();
    keycloakBefore();
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
    providersPage.fillLdapGeneralData(ldapName, ldapVendor);
    providersPage.fillLdapConnectionData(
      connectionUrlValid,
      bindTypeSimple,
      truststoreSpiAlways,
      connectionTimeoutTwoSecs,
      bindDnCnDc,
      bindCredsValid,
    );
    providersPage.toggleSwitch(providersPage.enableStartTls);
    providersPage.toggleSwitch(providersPage.connectionPooling);

    providersPage.fillLdapSearchingData(
      editModeReadOnly,
      firstUsersDn,
      firstUserLdapAtt,
      firstRdnLdapAtt,
      firstUuidLdapAtt,
      firstUserObjClasses,
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

  // create one of each non-hardcoded mapper type except
  // certificate ldap mapper which was already tested in CRUD section
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

  it("Should return one search result for mapper with unique string", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    listingPage.searchItem(uniqueSearchTerm, false);
    singleMapperName.map((mapperName) => listingPage.itemExist(mapperName));
  });

  it("Should return multiple search results for mappers that share common string", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    listingPage.searchItem(multipleSearchTerm, false);
    multiMapperNames.map((mapperName) => listingPage.itemExist(mapperName));
  });

  it("Should return all mappers in search results when no string is specified", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    listingPage.searchItem("", false);
    mapperNames.map((mapperName) => listingPage.itemExist(mapperName));
  });

  it("Should return no search results for string that does not exist in any mappers", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    listingPage.searchItem(nonexistingSearchTerm, false);
    listingPage.assertNoResults();
  });

  // *** test cleanup ***
  it("Cleanup - delete LDAP provider", () => {
    providersPage.deleteCardFromMenu(ldapName);
    modalUtils.checkModalTitle(providerDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(providerDeleteSuccess);
  });
});
