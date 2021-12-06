import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings client policies tab tests", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientPolicies-tab").click();
  });

  before(async () => {
    await new AdminClient().createRealm(realmName);
  });

  after(async () => {
    await new AdminClient().deleteRealm(realmName);
  });

  it("Go to client policies tab", () => {
    realmSettingsPage.shouldDisplayPoliciesTab();
  });

  it("Check new client form is displaying", () => {
    realmSettingsPage.shouldDisplayNewClientPolicyForm();
  });

  it("Complete new client form and cancel", () => {
    realmSettingsPage.shouldCompleteAndCancelCreateNewClientPolicy();
  });

  it("Complete new client form and submit", () => {
    realmSettingsPage.shouldCompleteAndCreateNewClientPolicyFromEmptyState();
  });

  it("Should perform client profile search by profile name", () => {
    realmSettingsPage.shouldSearchClientPolicy();
  });

  it("Should not have conditions configured by default", () => {
    realmSettingsPage.shouldNotHaveConditionsConfigured();
  });

  it("Should cancel adding a new condition to a client profile", () => {
    realmSettingsPage.shouldCancelAddingCondition();
  });

  it("Should add a new client-roles condition to a client profile", () => {
    realmSettingsPage.shouldAddClientRolesCondition();
  });

  it("Should add a new client-scopes condition to a client profile", () => {
    realmSettingsPage.shouldAddClientScopesCondition();
  });

  it("Should edit the client-roles condition of a client profile", () => {
    realmSettingsPage.shouldEditClientRolesCondition();
  });

  it("Should edit the client-scopes condition of a client profile", () => {
    realmSettingsPage.shouldEditClientScopesCondition();
  });

  it("Should cancel deleting condition from a client profile", () => {
    realmSettingsPage.shouldCancelDeletingCondition();
  });

  it("Should delete client-roles condition from a client profile", () => {
    realmSettingsPage.shouldDeleteClientRolesCondition();
  });

  it("Should delete client-scopes condition from a client profile", () => {
    realmSettingsPage.shouldDeleteClientScopesCondition();
  });

  it("Check cancelling the client policy deletion", () => {
    realmSettingsPage.shouldDisplayDeleteClientPolicyDialog();
  });

  it("Check deleting the client policy", () => {
    realmSettingsPage.shouldDeleteClientPolicyDialog();
  });

  it("Check navigating between Form View and JSON editor", () => {
    realmSettingsPage.shouldNavigateBetweenFormAndJSONViewPolicies();
  });

  it("Should not create duplicate client profile", () => {
    realmSettingsPage.shouldCompleteAndCreateNewClientPolicyFromEmptyState();

    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientPolicies-tab").click();
    realmSettingsPage.shouldCompleteAndCreateNewClientPolicy();
    realmSettingsPage.shouldNotCreateDuplicateClientPolicy();
    cy.wait(2000);
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientPolicies-tab").click();
    realmSettingsPage.shouldDeleteClientProfileDialog();
  });

  it("Check deleting newly created client policy from create view via dropdown", () => {
    realmSettingsPage.shouldRemoveClientPolicyFromCreateView();
  });

  it("Check reloading JSON policies", () => {
    realmSettingsPage.shouldReloadJSONPolicies();
  });
});
