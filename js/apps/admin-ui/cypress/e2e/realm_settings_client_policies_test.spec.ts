import { v4 as uuid } from "uuid";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";
import ModalUtils from "../support/util/ModalUtils";
import Masthead from "../support/pages/admin-ui/Masthead";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const modalUtils = new ModalUtils();
const masthead = new Masthead();

describe("Realm settings client policies tab tests", () => {
  const realmName = "Realm_" + uuid();
  const realmSettingsPage = new RealmSettingsPage(realmName);

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage
      .waitForPageLoad()
      .goToRealm(realmName)
      .goToRealmSettings()
      .waitForPageLoad();
    realmSettingsPage.goToClientPoliciesTab().goToClientPoliciesList();
  });

  before(() => adminClient.createRealm(realmName));

  after(() => {
    adminClient.deleteRealm(realmName);
  });

  it("Complete new client form and cancel", () => {
    realmSettingsPage
      .checkDisplayPoliciesTab()
      .createNewClientPolicyFromEmptyState("Test", "Test Description", true)
      .checkNewClientPolicyForm()
      .cancelNewClientPolicyCreation()
      .checkEmptyPolicyList();
  });

  it("Complete new client form and submit", () => {
    const url = `/admin/realms/${realmName}/client-policies/policies`;
    cy.intercept("PUT", url).as("save");

    realmSettingsPage.createNewClientPolicyFromEmptyState(
      "Test",
      "Test Description",
    );
    masthead.checkNotificationMessage("New policy created");
    cy.wait("@save");
  });

  it("Should perform client profile search by profile name", () => {
    realmSettingsPage.searchClientPolicy("Test");
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
    realmSettingsPage.deleteClientRolesCondition();
    sidebarPage.waitForPageLoad();
    modalUtils
      .checkModalTitle("Delete condition?")
      .checkModalMessage(
        "This action will permanently delete client-roles. This cannot be undone.",
      )
      .checkConfirmButtonText("Delete")
      .cancelButtonContains("Cancel")
      .cancelModal();
    realmSettingsPage.checkConditionsListContains("client-roles");
  });

  it("Should delete client-roles condition from a client profile", () => {
    realmSettingsPage.deleteClientRolesCondition();
    sidebarPage.waitForPageLoad();
    modalUtils.confirmModal();
    realmSettingsPage.checkConditionsListContains("client-scopes");
  });

  it("Should delete client-scopes condition from a client profile", () => {
    realmSettingsPage.shouldDeleteClientScopesCondition();
  });

  it("Check cancelling the client policy deletion", () => {
    realmSettingsPage.deleteClientPolicyItemFromTable("Test");
    modalUtils
      .checkModalMessage(
        "This action will permanently delete the policy Test. This cannot be undone.",
      )
      .cancelModal();
    realmSettingsPage.checkElementInList("Test");
  });

  it("Check deleting the client policy", () => {
    realmSettingsPage.deleteClientPolicyItemFromTable("Test");

    modalUtils.confirmModal();
    masthead.checkNotificationMessage("Client policy deleted");
    realmSettingsPage.checkEmptyPolicyList();
  });

  it("Check navigating between Form View and JSON editor", () => {
    realmSettingsPage.shouldNavigateBetweenFormAndJSONViewPolicies();
  });

  it("Should not create duplicate client profile", () => {
    const url = `admin/realms/${realmName}/client-policies/policies`;
    cy.intercept("PUT", url).as("save");

    realmSettingsPage.createNewClientPolicyFromEmptyState(
      "Test",
      "Test Description",
    );
    masthead.checkNotificationMessage("New policy created");
    cy.wait("@save");

    sidebarPage.goToRealmSettings();

    realmSettingsPage.goToClientPoliciesTab().goToClientPoliciesList();

    realmSettingsPage.createNewClientPolicyFromList(
      "Test",
      "Test Again Description",
      true,
    );

    realmSettingsPage.shouldShowErrorWhenDuplicate();
    sidebarPage.goToRealmSettings();

    realmSettingsPage
      .goToClientPoliciesTab()
      .goToClientPoliciesList()
      .deleteClientPolicyItemFromTable("Test");

    modalUtils.confirmModal();
    cy.wait("@save");
    masthead.checkNotificationMessage("Client policy deleted");
    realmSettingsPage.checkEmptyPolicyList();
  });

  it("Check deleting newly created client policy from create view via dropdown", () => {
    const url = `admin/realms/${realmName}/client-policies/policies`;
    cy.intercept("PUT", url).as("save");
    realmSettingsPage.createNewClientPolicyFromEmptyState(
      "Test again",
      "Test Again Description",
    );
    masthead.checkNotificationMessage("New policy created");
    sidebarPage.waitForPageLoad();
    cy.wait("@save");
    realmSettingsPage.deleteClientPolicyFromDetails();
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("Client policy deleted");
    sidebarPage.waitForPageLoad();
    realmSettingsPage.checkEmptyPolicyList();
  });

  it("Check reloading JSON policies", () => {
    realmSettingsPage.shouldReloadJSONPolicies();
  });
});
