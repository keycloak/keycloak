import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateKerberosProviderPage from "../support/pages/admin_console/manage/providers/CreateKerberosProviderPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";

const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();
const providersPage = new CreateKerberosProviderPage();
const modalUtils = new ModalUtils();

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

const createdSuccessMessage = "User federation provider successfully created";
const savedSuccessMessage = "User federation provider successfully saved";
const deletedSuccessMessage = "The user federation provider has been deleted.";
const deleteModalTitle = "Delete user federation provider?";
const disableModalTitle = "Disable user federation provider?";

describe("User Fed Kerberos tests", () => {
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

  it("Create Kerberos provider from empty state", () => {
    providersPage.clickNewCard("kerberos");
    providersPage.fillKerberosRequiredData(
      firstKerberosName,
      firstKerberosRealm,
      firstKerberosPrincipal,
      firstKerberosKeytab
    );
    providersPage.save();

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Update an existing Kerberos provider and save", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);
    providersPage.changeTime(defaultKerberosDay, newKerberosDay);
    providersPage.changeTime(defaultKerberosHour, newKerberosHour);
    providersPage.changeTime(defaultKerberosMinute, newKerberosMinute);
    providersPage.save();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    providersPage.clickExistingCard(firstKerberosName);

    expect(cy.contains(newPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Change existing Kerberos provider and click button to cancel", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);

    providersPage.changeTime(newKerberosDay, defaultKerberosDay);
    providersPage.changeTime(newKerberosHour, defaultKerberosHour);
    providersPage.changeTime(newKerberosMinute, defaultKerberosMinute);

    providersPage.cancel();
    cy.wait(1000);

    providersPage.clickExistingCard(firstKerberosName);
    providersPage.selectCacheType(newPolicy);

    expect(cy.contains(newKerberosDay).should("exist"));
    expect(cy.contains(newKerberosHour).should("exist"));
    expect(cy.contains(newKerberosMinute).should("exist"));
    expect(cy.contains(defaultKerberosMinute).should("not.exist"));

    sidebarPage.goToUserFederation();
  });

  it("Disable an existing Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.disableEnabledSwitch();

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);
    sidebarPage.goToUserFederation();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Enable an existing previously-disabled Kerberos provider", () => {
    providersPage.clickExistingCard(firstKerberosName);
    providersPage.enableEnabledSwitch();

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new Kerberos provider using the New Provider dropdown", () => {
    providersPage.clickMenuCommand("Add new provider", "Kerberos");

    providersPage.fillKerberosRequiredData(
      secondKerberosName,
      secondKerberosRealm,
      secondKerberosPrincipal,
      secondKerberosKeytab
    );
    providersPage.save();

    masthead.checkNotificationMessage(createdSuccessMessage);
    sidebarPage.goToUserFederation();
  });

  it("Delete a Kerberos provider from card view using the card's menu", () => {
    providersPage.deleteCardFromCard(secondKerberosName);

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Delete a Kerberos provider using the Settings view's Action menu", () => {
    providersPage.deleteCardFromMenu("kerberos", firstKerberosName);

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();
    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
