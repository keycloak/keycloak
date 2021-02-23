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
  it("Create Kerberos provider from empty state", () => {
    cy.visit("");
    loginPage.logIn();

    sidebarPage.goToUserFederation();
    cy.get("[data-testid=kerberos-card]").click();

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
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);
    providersPage.selectCacheType(newPolicy);

    cy.contains(defaultKerberosDay).click();
    cy.contains(newKerberosDay).click();

    cy.contains(defaultKerberosHour).click();
    cy.contains(newKerberosHour).click();

    cy.contains(defaultKerberosMinute).click();
    cy.contains(newKerberosMinute).click();

    providersPage.save();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    cy.wait(1000);

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);

    expect(cy.contains(newPolicy).should("exist"));
    expect(cy.contains(defaultPolicy).should("not.exist"));
  });

  it("Change existing Kerberos provider and click button to cancel", () => {
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);
    providersPage.selectCacheType(newPolicy);

    cy.contains(newKerberosDay).click();
    cy.contains(defaultKerberosDay).click();

    cy.contains(newKerberosHour).click();
    cy.contains(defaultKerberosHour).click();

    cy.contains(newKerberosMinute).click();
    cy.contains(defaultKerberosMinute).click();

    providersPage.cancel();

    cy.wait(1000);
    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);
    providersPage.selectCacheType(newPolicy);

    expect(cy.contains(newKerberosDay).should("exist"));
    expect(cy.contains(newKerberosHour).should("exist"));
    expect(cy.contains(newKerberosMinute).should("exist"));

    expect(cy.contains(defaultKerberosMinute).should("not.exist"));

    sidebarPage.goToUserFederation();
  });

  it("Disable an existing Kerberos provider", () => {
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);

    providersPage.disableEnabledSwitch();

    modalUtils.checkModalTitle(disableModalTitle).confirmModal();

    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();

    cy.wait(1000);
    expect(cy.contains("Disabled").should("exist"));
  });

  it("Enable an existing previously-disabled Kerberos provider", () => {
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);

    providersPage.enableEnabledSwitch();
    masthead.checkNotificationMessage(savedSuccessMessage);

    sidebarPage.goToUserFederation();

    cy.wait(1000);
    expect(cy.contains("Enabled").should("exist"));
  });

  it("Create new Kerberos provider using the New Provider dropdown", () => {
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.contains("Add new provider").click();
    cy.contains("Kerberos").click();
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
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="my-kerberos-2-dropdown"]').click();
    cy.get('[data-testid="card-delete"]').click();

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();

    masthead.checkNotificationMessage(deletedSuccessMessage);
  });

  it("Delete a Kerberos provider using the Settings view's Action menu", () => {
    cy.visit("");
    loginPage.logIn();
    sidebarPage.goToUserFederation();

    cy.get('[data-testid="keycloak-card-title"]')
      .contains(firstKerberosName)
      .click();
    cy.wait(1000);

    cy.get('[data-testid="action-dropdown"]').click();
    cy.get('[data-testid="delete-kerberos-cmd"]').click();

    modalUtils.checkModalTitle(deleteModalTitle).confirmModal();

    masthead.checkNotificationMessage(deletedSuccessMessage);
  });
});
