import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import GroupModal from "../support/pages/admin-ui/manage/groups/GroupModal";
import ProviderPage from "../support/pages/admin-ui/manage/providers/ProviderPage";
import CreateClientPage from "../support/pages/admin-ui/manage/clients/CreateClientPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import GroupPage from "../support/pages/admin-ui/manage/groups/GroupPage";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const groupModal = new GroupModal();
const createClientPage = new CreateClientPage();
const groupPage = new GroupPage();

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
const providerDeleteSuccess = "The user federation provider has been deleted.";
const providerDeleteTitle = "Delete user federation provider?";
const mapperDeletedSuccess = "Mapping successfully deleted";
const mapperDeleteTitle = "Delete mapping?";
const groupDeleteTitle = "Delete group?";
const groupCreatedSuccess = "Group created";
const groupDeletedSuccess = "Group deleted";
const clientCreatedSuccess = "Client created successfully";
const clientDeletedSuccess = "The client has been deleted";
const roleCreatedSuccess = "Role created";
const groupName = "aa-uf-mappers-group";
const clientName = "aa-uf-mappers-client";
const roleName = "aa-uf-mappers-role";

// mapperType variables
const hcAttMapper = "hardcoded-attribute-mapper";
const hcLdapGroupMapper = "hardcoded-ldap-group-mapper";
const hcLdapAttMapper = "hardcoded-ldap-attribute-mapper";
const roleLdapMapper = "role-ldap-mapper";
const hcLdapRoleMapper = "hardcoded-ldap-role-mapper";

// Used by "Delete default mappers" test
const creationDateMapper = "creation date";
const emailMapper = "email";
const lastNameMapper = "last name";
const modifyDateMapper = "modify date";

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

  // create a new group
  it("Create group", () => {
    sidebarPage.goToGroups();
    groupPage.openCreateGroupModal(true);
    groupModal.setGroupNameInput(groupName).create();
    masthead.checkNotificationMessage(groupCreatedSuccess);
  });

  // create a new client and then new role for that client
  it("Create client and role", () => {
    sidebarPage.goToClients();
    listingPage.goToCreateItem();
    createClientPage
      .selectClientType("OpenID Connect")
      .fillClientData(clientName)
      .continue()
      .continue()
      .save();

    masthead.checkNotificationMessage(clientCreatedSuccess);

    providersPage.createRole(roleName);
    masthead.checkNotificationMessage(roleCreatedSuccess);

    sidebarPage.goToClients();
    listingPage.searchItem(clientName).itemExist(clientName);
  });

  // delete four default mappers
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
  });

  // create one of each hardcoded mapper type
  it("Create hardcoded attribute mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(hcAttMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(hcAttMapper, true);
  });

  it("Create hardcoded ldap group mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(hcLdapGroupMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(hcLdapGroupMapper, true);
  });

  it("Create hardcoded ldap attribute mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(hcLdapAttMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(hcLdapAttMapper, true);
  });

  it("Create hardcoded ldap role mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(hcLdapRoleMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(hcLdapRoleMapper, true);
  });

  it("Create role ldap mapper", () => {
    providersPage.clickExistingCard(ldapName);
    providersPage.goToMappers();
    providersPage.createNewMapper(roleLdapMapper);
    providersPage.save("ldap-mapper");
    masthead.checkNotificationMessage(mapperCreatedSuccess);
    listingPage.itemExist(roleLdapMapper, true);
  });

  // *** test cleanup ***
  it("Cleanup - delete LDAP provider", () => {
    providersPage.deleteCardFromMenu(ldapName);
    modalUtils.checkModalTitle(providerDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(providerDeleteSuccess);
  });

  it("Cleanup - delete group", () => {
    sidebarPage.goToGroups();
    listingPage.deleteItem(groupName);
    modalUtils.checkModalTitle(groupDeleteTitle).confirmModal();
    masthead.checkNotificationMessage(groupDeletedSuccess);
  });

  it("Cleanup - delete client", () => {
    sidebarPage.goToClients();
    listingPage.deleteItem(clientName);
    modalUtils.checkModalTitle(`Delete ${clientName} ?`).confirmModal();
    masthead.checkNotificationMessage(clientDeletedSuccess);
  });
});
