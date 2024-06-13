import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import ProviderPage from "../support/pages/admin-ui/manage/providers/ProviderPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import PriorityDialog from "../support/pages/admin-ui/manage/providers/PriorityDialog";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new ProviderPage();
const modalUtils = new ModalUtils();

const provider = "kerberos";
const initCapProvider = provider.charAt(0).toUpperCase() + provider.slice(1);

const kerberosName = "my-kerberos";
const kerberosRealm = "my-realm";
const kerberosPrincipal = "my-principal";
const kerberosKeytab = "my-keytab";

const firstKerberosName = `${kerberosName}-1`;
const firstKerberosRealm = `${kerberosRealm}-1`;
const firstKerberosPrincipal = `${kerberosPrincipal}-1`;
const firstKerberosKeytab = `${kerberosKeytab}-1`;

const secondKerberosName = `${kerberosName}-2`;
const secondKerberosRealm = `${kerberosRealm}-2`;
const secondKerberosPrincipal = `${kerberosPrincipal}-2`;
const secondKerberosKeytab = `${kerberosKeytab}-2`;

const defaultPolicy = "DEFAULT";
const weeklyPolicy = "EVICT_WEEKLY";
const dailyPolicy = "EVICT_DAILY";
const lifespanPolicy = "MAX_LIFESPAN";
const noCachePolicy = "NO_CACHE";

const defaultKerberosDay = "Sunday";
const defaultKerberosHour = "00";
const defaultKerberosMinute = "00";
const newKerberosDay = "Wednesday";
const newKerberosHour = "15";
const newKerberosMinute = "55";
const maxLifespan = 5;

const addProviderMenu = "Add new provider";
const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";
const changeSuccessMsg =
  "Successfully changed the priority order of user federation providers";

describe("User Fed Kerberos tests", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToUserFederation();
  });

  it("Should create Kerberos provider from empty state", () => {
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
      firstKerberosKeytab,
    );
    providersPage.save(provider);

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Should enable debug, password authentication, and first login", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.toggleSwitch(providersPage.debugSwitch);
    providersPage.toggleSwitch(providersPage.passwordAuthSwitch);
    providersPage.toggleSwitch(providersPage.firstLoginSwitch);

    providersPage.save(provider);
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    providersPage.verifyToggle(providersPage.debugSwitch, "on");
    providersPage.verifyToggle(providersPage.passwordAuthSwitch, "on");
    providersPage.verifyToggle(providersPage.firstLoginSwitch, "on");
  });

  it("Should set cache policy to evict_daily", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(dailyPolicy);
    providersPage.changeCacheTime("hour", newKerberosHour);
    providersPage.changeCacheTime("minute", newKerberosMinute);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(dailyPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Should set cache policy to default", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(defaultPolicy);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(defaultPolicy).should("exist"));
    expect(cy.contains(dailyPolicy).should("not.exist"));
  });

  it("Should set cache policy to evict_weekly", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(weeklyPolicy);
    providersPage.changeCacheTime("day", newKerberosDay);
    providersPage.changeCacheTime("hour", newKerberosHour);
    providersPage.changeCacheTime("minute", newKerberosMinute);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(weeklyPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Should set cache policy to max_lifespan", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(lifespanPolicy);
    providersPage.fillMaxLifespanData(maxLifespan);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(lifespanPolicy).should("exist"));
    expect(cy.contains(weeklyPolicy).should("not.exist"));
  });

  it("Should set cache policy to no_cache", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(noCachePolicy);
    providersPage.save(provider);

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(noCachePolicy).should("exist"));
    expect(cy.contains(lifespanPolicy).should("not.exist"));
  });

  it("Should edit existing Kerberos provider and cancel", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(weeklyPolicy);

    providersPage.changeCacheTime("day", defaultKerberosDay);
    providersPage.changeCacheTime("hour", defaultKerberosHour);
    providersPage.changeCacheTime("minute", defaultKerberosMinute);

    providersPage.cancel(provider);

    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(weeklyPolicy);

    providersPage.verifyChangedHourInput(newKerberosHour, defaultKerberosHour);
    sidebarPage.goToUserFederation();
  });

  it("Should disable an existing Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.disableEnabledSwitch(initCapProvider);

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();

    expect(cy.contains("Disabled").should("exist"));
  });

  it("Should enable an existing previously-disabled Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.enableEnabledSwitch(initCapProvider);

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Should create new Kerberos provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand(addProviderMenu, initCapProvider);

    providersPage.fillKerberosRequiredData(
      secondKerberosName,
      secondKerberosRealm,
      secondKerberosPrincipal,
      secondKerberosKeytab,
    );
    providersPage.save(provider);

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it.skip("Should change the priority order of Kerberos providers", () => {
    const priorityDialog = new PriorityDialog();
    const providers = [firstKerberosName, secondKerberosName];

    sidebarPage.goToUserFederation();
    providersPage.clickMenuCommand(addProviderMenu, initCapProvider);

    sidebarPage.goToUserFederation();
    priorityDialog.openDialog().checkOrder(providers);
    priorityDialog.clickSave();
    masthead.checkNotificationMessage(changeSuccessMsg, true);
  });

  it("Should delete a Kerberos provider from card view using the card's menu", () => {
    providersPage.deleteCardFromCard(secondKerberosName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Should delete a Kerberos provider using the Settings view's Action menu", () => {
    providersPage.deleteCardFromMenu(firstKerberosName);
    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
