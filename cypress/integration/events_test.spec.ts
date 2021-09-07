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

describe("Events tests", function () {
  describe("Search user events", function () {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
    });

    it("Check search dropdown display", () => {
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
      sidebarPage.goToRealmSettings();
      cy.getId("rs-realm-events-tab").click();

      realmSettingsPage
        .toggleSwitch(realmSettingsPage.enableEvents)
        .save(realmSettingsPage.eventsUserSave);

      masthead.signOut();
      loginPage.logIn();

      sidebarPage.goToEvents();
      userEventsTab.shouldDoSearchAndRemoveChips();
    });

    it("Check for no events logged", () => {
      userEventsTab.shouldDoNoResultsSearch();
    });

    it("Check `search events` button enabled", () => {
      userEventsTab.shouldHaveSearchBtnEnabled();
    });
  });

  describe("Search admin events", function () {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      cy.getId("admin-events-tab").click();
    });

    it("Check admin events search form fields display", () => {
      adminEventsTab.shouldHaveFormFields();
    });

    it("Check `search admin events` button disabled by default", () => {
      adminEventsTab.shouldHaveSearchBtnDisabled();
    });

    it("Check admin events search and removal work", () => {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-realm-events-tab").click();
      cy.getId("rs-admin-events-tab").click();

      realmSettingsPage
        .toggleSwitch(realmSettingsPage.enableAdminEvents)
        .save(realmSettingsPage.eventsAdminSave);

      sidebarPage.goToEvents();
      cy.getId("admin-events-tab").click();
      adminEventsTab.shouldDoAdminEventsSearchAndRemoveChips();
    });

    it("Check for no events logged", () => {
      adminEventsTab.shouldDoNoResultsSearch();
    });

    it("Check `search admin events` button enabled", () => {
      adminEventsTab.shouldHaveSearchBtnEnabled();
    });
  });

  describe("Check more button opens auth and representation dialogs", function () {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      cy.getId("admin-events-tab").click();
    });

    it("Check auth dialog opens and is not empty", () => {
      adminEventsTab.shouldCheckAuthDialogOpensAndIsNotEmpty();
    });

    it("Check representation dialog opens and is not empty", () => {
      adminEventsTab.shouldCheckRepDialogOpensAndIsNotEmpty();
    });
  });
});
