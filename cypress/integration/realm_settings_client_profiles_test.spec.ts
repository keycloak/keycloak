import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import { keycloakBefore } from "../support/util/keycloak_before";
import AdminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const realmSettingsPage = new RealmSettingsPage();

describe("Realm settings client profiles tab tests", () => {
  const realmName = "Realm_" + (Math.random() + 1).toString(36).substring(7);

  beforeEach(() => {
    keycloakBefore();
    loginPage.logIn();
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientProfiles-tab").click();
  });

  before(async () => {
    await new AdminClient().createRealm(realmName);
  });

  after(async () => {
    await new AdminClient().deleteRealm(realmName);
  });

  it("Go to client policies profiles tab", () => {
    realmSettingsPage.shouldDisplayProfilesTab();
  });

  it("Check new client form is displaying", () => {
    realmSettingsPage.shouldDisplayNewClientProfileForm();
  });

  it("Complete new client form and cancel", () => {
    realmSettingsPage.shouldCompleteAndCancelCreateNewClientProfile();
  });

  it("Complete new client form and submit", () => {
    realmSettingsPage.shouldCompleteAndCreateNewClientProfile();
  });

  it("Should perform client profile search by profile name", () => {
    realmSettingsPage.shouldSearchClientProfile();
  });

  it("Check cancelling the client profile deletion", () => {
    realmSettingsPage.shouldDisplayDeleteClientProfileDialog();
  });

  it("Check deleting the client profile", () => {
    realmSettingsPage.shouldDeleteClientProfileDialog();
  });

  it("Check navigating between Form View and JSON editor", () => {
    realmSettingsPage.shouldNavigateBetweenFormAndJSONView();
  });

  it("Check saving changed JSON profiles", () => {
    realmSettingsPage.shouldSaveChangedJSONProfiles();
    realmSettingsPage.shouldDeleteClientProfileDialog();
  });

  it("Should not create duplicate client profile", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientProfiles-tab").click();
    realmSettingsPage.shouldCompleteAndCreateNewClientProfile();
    realmSettingsPage.shouldNotCreateDuplicateClientProfile();
  });

  it("Should edit client profile", () => {
    realmSettingsPage.shouldEditClientProfile();
  });

  it("Should check that edited client profile is now listed", () => {
    sidebarPage.goToRealmSettings();
    cy.findByTestId("rs-clientPolicies-tab").click();
    cy.findByTestId("rs-policies-clientProfiles-tab").click();
    realmSettingsPage.shouldCheckEditedClientProfileListed();
  });

  it("Should show error when client profile left blank", () => {
    realmSettingsPage.shouldShowErrorWhenNameBlank();
  });

  it("Should revert back to the previous profile name", () => {
    realmSettingsPage.shouldReloadClientProfileEdits();
  });

  it("Should not have executors configured by default", () => {
    realmSettingsPage.shouldNotHaveExecutorsConfigured();
  });

  it("Should cancel adding a new executor to a client profile", () => {
    realmSettingsPage.shouldCancelAddingExecutor();
  });

  it("Should add a new executor to a client profile", () => {
    realmSettingsPage.shouldAddExecutor();
  });

  it("Should cancel deleting executor from a client profile", () => {
    realmSettingsPage.shouldCancelDeletingExecutor();
  });

  it("Should cancel editing executor", () => {
    realmSettingsPage.shouldCancelEditingExecutor();
  });

  it("Should edit executor", () => {
    realmSettingsPage.shouldEditExecutor();
  });

  it("Should delete executor from a client profile", () => {
    realmSettingsPage.shouldDeleteExecutor();
  });

  it("Should delete edited client profile", () => {
    realmSettingsPage.shouldDeleteEditedProfile();
  });
});
