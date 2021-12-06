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

const provider = "kerberos";
const initCapProvider = provider.charAt(0).toUpperCase() + provider.slice(1);

const firstKerberosName = "my-kerberos";
const firstKerberosRealm = "my-realm";
const firstKerberosPrincipal = "my-principal";
const firstKerberosKeytab = "my-keytab";

const secondKerberosName = `${firstKerberosName}-2`;
const secondKerberosRealm = `${firstKerberosRealm}-2`;
const secondKerberosPrincipal = `${firstKerberosPrincipal}-2`;
const secondKerberosKeytab = `${firstKerberosKeytab}-2`;

const defaultPolicy = "DEFAULT";
const newPolicy = "EVICT_WEEKLY";
const defaultKerberosDay = "Sunday";
const defaultKerberosHour = "00";
const defaultKerberosMinute = "00";
const newKerberosDay = "Wednesday";
const newKerberosHour = "15";
const newKerberosMinute = "55";

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";

describe("User Fed Kerberos tests", () => {
  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToUserFederation();
  });

  it("Create Kerberos provider from empty state", () => {
    // if tests don't start at empty state, e.g. user has providers configured locally,
    // create a new card from the card view instead
    cy.get("body").then(($body) => {
      if ($body.find(`[data-testid=kerberos-card]`).length > 0) {
        providersPage.clickNewCard(provider);
      } else {
        providersPage.clickMenuCommand(addProviderMenu, initCapProvider);
      }
    });
    providersPage.fillKerberosRequiredData(
      firstKerberosName,
      firstKerberosRealm,
      firstKerberosPrincipal,
      firstKerberosKeytab
    );
    providersPage.save(provider);

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Update an existing Kerberos provider and save", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);
    providersPage.changeCacheTime("day", newKerberosDay);
    providersPage.changeCacheTime("hour", newKerberosHour);
    providersPage.changeCacheTime("minute", newKerberosMinute);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(newPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Change existing Kerberos provider and click button to cancel", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeCacheTime("day", defaultKerberosDay);
    providersPage.changeCacheTime("hour", defaultKerberosHour);
    providersPage.changeCacheTime("minute", defaultKerberosMinute);

    providersPage.cancel(provider);
    cy.wait(1000);

    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);

    providersPage.verifyChangedHourInput(newKerberosHour, defaultKerberosHour);
    sidebarPage.goToUserFederation();
  });

  it("Disable an existing Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.disableEnabledSwitch(initCapProvider);

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Enable an existing previously-disabled Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.enableEnabledSwitch(initCapProvider);

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new Kerberos provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand(addProviderMenu, initCapProvider);

    providersPage.fillKerberosRequiredData(
      secondKerberosName,
      secondKerberosRealm,
      secondKerberosPrincipal,
      secondKerberosKeytab
    );
    providersPage.save(provider);

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Delete a Kerberos provider from card view using the card's menu", () => {
    providersPage.deleteCardFromCard(secondKerberosName);

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Delete a Kerberos provider using the Settings view's Action menu", () => {
    providersPage.deleteCardFromMenu(provider, firstKerberosName);

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
