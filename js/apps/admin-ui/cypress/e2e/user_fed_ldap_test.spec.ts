import { v4 as uuid } from "uuid";

import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ProviderPage from "../support/pages/admin-ui/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new ProviderPage();
const modalUtils = new ModalUtils();

const provider = "ldap";
const allCapProvider = provider.toUpperCase();

const firstLdapName = "my-ldap";
const firstLdapVendor = "Active Directory";
const secondLdapName = `${firstLdapName}-2`;
const secondLdapVendor = "Other";
const updatedLdapName = `${firstLdapName}-updated`;

// connection and authentication settings
const connectionUrlValid = "ldap://localhost:3004";
const bindTypeSimple = "simple";
const truststoreSpiAlways = "Always";
const connectionTimeoutTwoSecs = "2000";
const bindDnCnDc = "cn=user,dc=test";
const bindCredsValid = "user";

const connectionUrlInvalid = "ldap://nowhere.com";
const bindTypeNone = "none";
const truststoreSpiNever = "Never";
const bindDnCnOnly = "cn=read-only-admin";
const bindCredsInvalid = "not-my-password";

// kerberos integration settings
const kerberosRealm = "FOO.ORG";
const serverPrincipal = "HTTP/host.foo.org@FOO.ORG";
const keyTab = "/etc/krb5.keytab";

// ldap synchronization settings
const batchSize = "100";
const fullSyncPeriod = "604800";
const userSyncPeriod = "86400";

// ldap searching and updating
const editModeReadOnly = "READ_ONLY";
const editModeWritable = "WRITABLE";
const editModeUnsynced = "UNSYNCED";

const firstUsersDn = "user-dn-1";
const firstUserLdapAtt = "uid";
const firstRdnLdapAtt = "uid";
const firstUuidLdapAtt = "entryUUID";
const firstUserObjClasses = "inetOrgPerson, organizationalPerson";
const firstUserLdapFilter = "(first-filter)";
const firstReadTimeout = "5000";

const searchScopeOneLevel = "One Level";
const searchScopeSubtree = "Subtree";

const secondUsersDn = "user-dn-2";
const secondUserLdapAtt = "cn";
const secondRdnLdapAtt = "cn";
const secondUuidLdapAtt = "objectGUID";
const secondUserObjClasses = "person, organizationalPerson, user";
const secondUserLdapFilter = "(second-filter)";
const secondReadTimeout = "5000";

const defaultPolicy = "DEFAULT";
const weeklyPolicy = "EVICT_WEEKLY";
const dailyPolicy = "EVICT_DAILY";
const lifespanPolicy = "MAX_LIFESPAN";
const noCachePolicy = "NO_CACHE";
const defaultLdapDay = "Sunday";
const defaultLdapHour = "00";
const defaultLdapMinute = "00";
const newLdapDay = "Wednesday";
const newLdapHour = "15";
const newLdapMinute = "55";
const maxLifespan = 5;

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";
const validatePasswordPolicyFailMessage =
  "User federation provider could not be saved: Validate Password Policy is applicable only with WRITABLE edit mode";
const userImportingDisabledFailMessage =
  "User federation provider could not be saved: Can not disable Importing users when LDAP provider mode is UNSYNCED";

const ldapTestSuccessMsg = "Successfully connected to LDAP";
const ldapTestFailMsg =
  "Error when trying to connect to LDAP: 'CommunicationError'";

describe("User Federation LDAP tests", () => {
  const realmName = `ldap-realm-${uuid()}`;

  before(() => adminClient.createRealm(realmName));

  after(() => adminClient.deleteRealm(realmName));

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealm(realmName);
    sidebarPage.goToUserFederation();
  });

  it("Should create LDAP provider from empty state", () => {
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
      bindCredsInvalid,
    );
    providersPage.fillLdapSearchingData(
      editModeReadOnly,
      firstUsersDn,
      firstUserLdapAtt,
      firstRdnLdapAtt,
      firstUuidLdapAtt,
      firstUserObjClasses,
      firstUserLdapFilter,
      searchScopeOneLevel,
      firstReadTimeout,
    );
    providersPage.save(provider);
    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Should fail updating advanced settings", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.toggleSwitch(providersPage.ldapv3PwSwitch);
    providersPage.toggleSwitch(providersPage.validatePwPolicySwitch);
    providersPage.toggleSwitch(providersPage.trustEmailSwitch);
    providersPage.save(provider);
    masthead.checkNotificationMessage(validatePasswordPolicyFailMessage);
    sidebarPage.goToUserFederation();
  });

  it("Should update advanced settings", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.toggleSwitch(providersPage.ldapv3PwSwitch);
    providersPage.toggleSwitch(providersPage.validatePwPolicySwitch);
    providersPage.toggleSwitch(providersPage.trustEmailSwitch);
    providersPage.fillLdapSearchingData(
      editModeWritable,
      secondUsersDn,
      secondUserLdapAtt,
      secondRdnLdapAtt,
      secondUuidLdapAtt,
      secondUserObjClasses,
    );
    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);
    providersPage.verifyToggle(providersPage.ldapv3PwSwitch, "on");
    providersPage.verifyToggle(providersPage.validatePwPolicySwitch, "on");
    providersPage.verifyToggle(providersPage.trustEmailSwitch, "on");
  });

  it("Should set cache policy to evict_daily", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(dailyPolicy);
    providersPage.changeCacheTime("hour", newLdapHour);
    providersPage.changeCacheTime("minute", newLdapMinute);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(dailyPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Should set cache policy to default", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(defaultPolicy);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(defaultPolicy).should("exist"));
    expect(cy.contains(dailyPolicy).should("not.exist"));
  });

  it("Should set cache policy to evict_weekly", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(weeklyPolicy);
    providersPage.changeCacheTime("day", newLdapDay);
    providersPage.changeCacheTime("hour", newLdapHour);
    providersPage.changeCacheTime("minute", newLdapMinute);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(weeklyPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Update connection and authentication settings and save", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.fillLdapConnectionData(
      connectionUrlInvalid,
      bindTypeNone,
      truststoreSpiNever,
      connectionTimeoutTwoSecs,
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
      connectionUrlInvalid,
    );
    providersPage.verifyTextField(
      providersPage.connectionTimeoutInput,
      connectionTimeoutTwoSecs,
    );
    providersPage.verifySelect(
      providersPage.truststoreSpiInput,
      truststoreSpiNever,
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
      truststoreSpiAlways,
      connectionTimeoutTwoSecs,
      bindDnCnDc,
      bindCredsValid,
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

  it("Should update Kerberos integration settings and save", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.toggleSwitch(providersPage.allowKerberosAuth);
    providersPage.toggleSwitch(providersPage.debug);
    providersPage.toggleSwitch(providersPage.useKerberosForPwAuth);

    providersPage.fillTextField(
      providersPage.ldapKerberosRealmInput,
      kerberosRealm,
    );
    providersPage.fillTextField(
      providersPage.ldapServerPrincipalInput,
      serverPrincipal,
    );
    providersPage.fillTextField(providersPage.ldapKeyTabInput, keyTab);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    // now verify
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);
    providersPage.verifyTextField(
      providersPage.ldapKerberosRealmInput,
      kerberosRealm,
    );
    providersPage.verifyTextField(
      providersPage.ldapServerPrincipalInput,
      serverPrincipal,
    );
    providersPage.verifyTextField(providersPage.ldapKeyTabInput, keyTab);
    providersPage.verifyToggle(providersPage.allowKerberosAuth, "on");
    providersPage.verifyToggle(providersPage.debug, "on");
    providersPage.verifyToggle(providersPage.useKerberosForPwAuth, "on");

    sidebarPage.goToUserFederation();
  });

  it("Should update Synchronization settings and save", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.toggleSwitch(providersPage.importUsers);
    providersPage.toggleSwitch(providersPage.periodicFullSync);
    providersPage.toggleSwitch(providersPage.periodicUsersSync);

    providersPage.fillTextField(providersPage.ldapBatchSizeInput, batchSize);
    providersPage.fillTextField(
      providersPage.ldapFullSyncPeriodInput,
      fullSyncPeriod,
    );
    providersPage.fillTextField(
      providersPage.ldapUsersSyncPeriodInput,
      userSyncPeriod,
    );

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    // now verify
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);
    providersPage.verifyTextField(providersPage.ldapBatchSizeInput, batchSize);
    providersPage.verifyTextField(
      providersPage.ldapFullSyncPeriodInput,
      fullSyncPeriod,
    );
    providersPage.verifyTextField(
      providersPage.ldapUsersSyncPeriodInput,
      userSyncPeriod,
    );
    providersPage.verifyToggle(providersPage.periodicFullSync, "on");
    providersPage.verifyToggle(providersPage.periodicUsersSync, "on");
    providersPage.verifyToggle(providersPage.importUsers, "on");
    sidebarPage.goToUserFederation();
  });

  it("Should update LDAP searching and updating settings and save", () => {
    providersPage.clickExistingCard(firstLdapName);

    providersPage.fillLdapSearchingData(
      editModeWritable,
      secondUsersDn,
      secondUserLdapAtt,
      secondRdnLdapAtt,
      secondUuidLdapAtt,
      secondUserObjClasses,
      secondUserLdapFilter,
      searchScopeSubtree,
      secondReadTimeout,
    );
    providersPage.toggleSwitch(providersPage.ldapPagination);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    // now verify
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    providersPage.verifySelect(
      providersPage.ldapEditModeInput,
      editModeWritable,
    );
    providersPage.verifyTextField(
      providersPage.ldapUsersDnInput,
      secondUsersDn,
    );
    providersPage.verifyTextField(
      providersPage.ldapUserLdapAttInput,
      secondUserLdapAtt,
    );
    providersPage.verifyTextField(
      providersPage.ldapRdnLdapAttInput,
      secondRdnLdapAtt,
    );
    providersPage.verifyTextField(
      providersPage.ldapUuidLdapAttInput,
      secondUuidLdapAtt,
    );
    providersPage.verifyTextField(
      providersPage.ldapUserObjClassesInput,
      secondUserObjClasses,
    );
    providersPage.verifyTextField(
      providersPage.ldapUserLdapFilter,
      secondUserLdapFilter,
    );
    providersPage.verifySelect(
      providersPage.ldapSearchScopeInput,
      searchScopeSubtree,
    );
    providersPage.verifyTextField(
      providersPage.ldapReadTimeout,
      secondReadTimeout,
    );
    providersPage.verifyToggle(providersPage.ldapPagination, "on");

    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    providersPage.fillSelect(providersPage.ldapEditModeInput, editModeUnsynced);

    providersPage.toggleSwitch(providersPage.importUsers);

    providersPage.save(provider);
    masthead.checkNotificationMessage(validatePasswordPolicyFailMessage);

    providersPage.toggleSwitch(providersPage.importUsers);
    providersPage.toggleSwitch(providersPage.validatePwPolicySwitch);
    providersPage.save(provider);

    masthead.checkNotificationMessage(userImportingDisabledFailMessage);

    providersPage.toggleSwitch(providersPage.importUsers);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    // now verify
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    providersPage.verifySelect(
      providersPage.ldapEditModeInput,
      editModeUnsynced,
    );
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

  it("Should update existing LDAP provider and cancel", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(weeklyPolicy);

    providersPage.changeCacheTime("day", defaultLdapDay);
    providersPage.changeCacheTime("hour", defaultLdapHour);
    providersPage.changeCacheTime("minute", defaultLdapMinute);

    providersPage.cancel(provider);

    providersPage.clickExistingCard(updatedLdapName);
    providersPage.selectCacheType(weeklyPolicy);

    providersPage.verifyChangedHourInput(newLdapHour, defaultLdapHour);

    sidebarPage.goToUserFederation();
  });

  it("Should set cache policy to max_lifespan", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(lifespanPolicy);
    providersPage.fillMaxLifespanData(maxLifespan);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(lifespanPolicy).should("exist"));
    expect(cy.contains(weeklyPolicy).should("not.exist"));
  });

  it("Should set cache policy to no_cache", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.selectCacheType(noCachePolicy);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstLdapName);

    expect(cy.contains(noCachePolicy).should("exist"));
    expect(cy.contains(lifespanPolicy).should("not.exist"));
  });

  it("Should disable an existing LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.disableEnabledSwitch(allCapProvider);
    modalUtils.checkModalTitle(disableModalTitle).confirmModal();
    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Should enable a previously-disabled LDAP provider", () => {
    providersPage.clickExistingCard(firstLdapName);
    providersPage.enableEnabledSwitch(allCapProvider);
    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Should create new LDAP provider using New Provider dropdown", () => {
    providersPage.clickMenuCommand(addProviderMenu, allCapProvider);
    providersPage.fillLdapGeneralData(secondLdapName, secondLdapVendor);
    providersPage.fillLdapConnectionData(
      connectionUrlValid,
      bindTypeSimple,
      truststoreSpiNever,
      connectionTimeoutTwoSecs,
      bindDnCnOnly,
      bindCredsInvalid,
    );
    providersPage.fillLdapSearchingData(
      editModeWritable,
      secondUsersDn,
      secondUserLdapAtt,
      secondRdnLdapAtt,
      secondUuidLdapAtt,
      secondUserObjClasses,
    );
    providersPage.save(provider);
    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Should delete LDAP provider from card view using card menu", () => {
    providersPage.deleteCardFromCard(secondLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Should delete LDAP provider using Settings view Action menu", () => {
    providersPage.deleteCardFromMenu(firstLdapName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
