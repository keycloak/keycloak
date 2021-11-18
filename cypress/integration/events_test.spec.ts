import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import UserEventsTab from "../support/pages/admin_console/manage/events/UserEventsTab";
import AdminEventsTab from "../support/pages/admin_console/manage/events/AdminEventsTab";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import { keycloakBefore } from "../support/util/keycloak_before";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const userEventsTab = new UserEventsTab();
const adminEventsTab = new AdminEventsTab();
const realmSettingsPage = new RealmSettingsPage();
const masthead = new Masthead();

describe("Events tests", () => {
  describe("Search user events", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
    });

    it("Check search dropdown display", () => {
      sidebarPage.goToRealmSettings();
      cy.findByTestId("rs-realm-events-tab").click();
      cy.findByTestId("rs-events-tab").click();

      realmSettingsPage
        .toggleSwitch(realmSettingsPage.enableEvents)
        .save(realmSettingsPage.eventsUserSave);

      masthead.signOut();
      loginPage.logIn();

      sidebarPage.goToEvents();

      userEventsTab.shouldDisplay();
    });

    it("Check user events search form fields display", () => {
      userEventsTab.shouldHaveFormFields();
    });

    it("Check event type dropdown options exist", () => {
      userEventsTab.shouldHaveEventTypeOptions();
    });

    it("Check `search events` button disabled by default", () => {
      userEventsTab.shouldHaveSearchBtnDisabled();
    });

    it("Check user events search and removal work", () => {
      userEventsTab.shouldDoSearchAndRemoveChips();
    });

    it("Check for no events logged", () => {
      userEventsTab.shouldDoNoResultsSearch();
    });

    it("Check `search events` button enabled", () => {
      userEventsTab.shouldHaveSearchBtnEnabled();
    });
  });

  describe("Search admin events", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      cy.findByTestId("admin-events-tab").click();
    });

    it("Check admin events search form fields display", () => {
      sidebarPage.goToRealmSettings();
      cy.findByTestId("rs-realm-events-tab").click();
      cy.findByTestId("rs-admin-events-tab").click();

      realmSettingsPage
        .toggleSwitch(realmSettingsPage.enableAdminEvents)
        .save(realmSettingsPage.eventsAdminSave);

      sidebarPage.goToEvents();
      cy.findByTestId("admin-events-tab").click();
      sidebarPage.waitForPageLoad();
      adminEventsTab.shouldHaveFormFields();
    });

    it("Check `search admin events` button disabled by default", () => {
      adminEventsTab.shouldHaveSearchBtnDisabled();
    });

    it("Check admin events search and removal work", () => {
      sidebarPage.goToEvents();
      cy.findByTestId("admin-events-tab").click();
      adminEventsTab.shouldDoAdminEventsSearchAndRemoveChips();
    });

    it("Check for no events logged", () => {
      adminEventsTab.shouldDoNoResultsSearch();
    });

    it("Check `search admin events` button enabled", () => {
      adminEventsTab.shouldHaveSearchBtnEnabled();
    });
  });

  describe("Check more button opens auth and representation dialogs", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      cy.findByTestId("admin-events-tab").click();
    });

    it("Check auth dialog opens and is not empty", () => {
      adminEventsTab.shouldCheckAuthDialogOpensAndIsNotEmpty();
    });

    it("Check representation dialog opens and is not empty", () => {
      sidebarPage.waitForPageLoad();
      adminEventsTab.shouldCheckRepDialogOpensAndIsNotEmpty();
    });
  });
});
