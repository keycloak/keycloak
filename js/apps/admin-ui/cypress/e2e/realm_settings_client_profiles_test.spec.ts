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

describe("Realm settings client profiles tab tests", () => {
  const profileName = "Test";
  const editedProfileName = "Edit";
  const realmName = "Realm_" + uuid();
  const realmSettingsPage = new RealmSettingsPage(realmName);

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.waitForPageLoad().goToRealm(realmName).goToRealmSettings();
    realmSettingsPage.goToClientPoliciesTab().goToClientProfilesList();
  });

  before(() => adminClient.createRealm(realmName));

  after(() => adminClient.deleteRealm(realmName));

  it("Go to client policies profiles tab", () => {
    realmSettingsPage.shouldDisplayProfilesTab();
  });

  it("Check new client form is displaying", () => {
    realmSettingsPage.shouldDisplayNewClientProfileForm();
  });

  it("Complete new client form and cancel", () => {
    realmSettingsPage
      .createClientProfile(profileName, "Test Description")
      .cancelClientProfileCreation()
      .checkElementNotInList(profileName);
  });

  it("Complete new client form and submit", () => {
    const url = `admin/realms/${realmName}/client-policies/profiles`;
    cy.intercept("PUT", url).as("save");
    realmSettingsPage
      .createClientProfile(profileName, "Test Description")
      .saveClientProfileCreation();
    cy.wait("@save");
    masthead.checkNotificationMessage("New client profile created");
  });

  it("Should perform client profile search by profile name", () => {
    realmSettingsPage.searchClientProfile(profileName);
  });

  it("Should search non-existent client profile", () => {
    realmSettingsPage.searchNonExistingClientProfile("nonExistentProfile");
    cy.findByTestId("empty-state").should("be.visible");
  });

  it("Should navigate to client profile", () => {
    realmSettingsPage.searchClientProfile(profileName);
    realmSettingsPage.goToClientProfileByNameLink(profileName);
    cy.findByTestId("view-header").should("have.text", profileName);
  });

  it("Check navigating between Form View and JSON editor", () => {
    realmSettingsPage.shouldNavigateBetweenFormAndJSONView();
  });

  it("Check saving changed JSON profiles", () => {
    realmSettingsPage.shouldSaveChangedJSONProfiles();
    realmSettingsPage.deleteClientPolicyItemFromTable(profileName);
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("Client profile deleted");
    realmSettingsPage.checkElementNotInList(profileName);
  });

  it("Should not create duplicate client profile", () => {
    const url = `admin/realms/${realmName}/client-policies/profiles`;
    cy.intercept("PUT", url).as("save");
    realmSettingsPage
      .createClientProfile(profileName, "Test Description")
      .saveClientProfileCreation();
    cy.wait("@save");

    sidebarPage.goToRealmSettings();
    realmSettingsPage.goToClientPoliciesTab().goToClientProfilesList();

    sidebarPage.waitForPageLoad();
    realmSettingsPage
      .createClientProfile(profileName, "Test Description")
      .saveClientProfileCreation();
    cy.wait("@save");
    masthead.checkNotificationMessage(
      "Could not create client profile: 'proposed client profile name duplicated.'",
    );
  });

  it("Should edit client profile", () => {
    realmSettingsPage.shouldEditClientProfile();
  });

  it("Should check that edited client profile is now listed", () => {
    sidebarPage.goToRealmSettings();
    realmSettingsPage
      .goToClientPoliciesTab()
      .goToClientProfilesList()
      .shouldCheckEditedClientProfileListed();
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
    realmSettingsPage.openProfileDetails(editedProfileName).editExecutor(4000);
    sidebarPage.waitForPageLoad();
    realmSettingsPage
      .cancelEditingExecutor()
      .checkExecutorNotInList()
      .editExecutor()
      .checkAvailablePeriodExecutor(3600);
  });

  it("Should edit executor", () => {
    realmSettingsPage
      .openProfileDetails(editedProfileName)
      .editExecutor(4000)
      .saveExecutor();
    masthead.checkNotificationMessage("Executor updated successfully");
    realmSettingsPage.editExecutor();
    // TODO: UNCOMMENT LINE WHEN ISSUE 2037 IS FIXED
    //.checkAvailablePeriodExecutor(4000);
  });

  it("Should delete executor from a client profile", () => {
    realmSettingsPage.shouldDeleteExecutor();
  });

  it("Check cancelling the client profile deletion", () => {
    realmSettingsPage.deleteClientPolicyItemFromTable(editedProfileName);
    modalUtils
      .checkModalMessage(
        "This action will permanently delete the profile " +
          editedProfileName +
          ". This cannot be undone.",
      )
      .cancelModal();
    realmSettingsPage.checkElementInList(editedProfileName);
  });

  it("Check deleting the client profile", () => {
    realmSettingsPage.deleteClientPolicyItemFromTable(editedProfileName);
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("Client profile deleted");
    sidebarPage.waitForPageLoad();
    realmSettingsPage.checkElementNotInList(editedProfileName);
  });
});
