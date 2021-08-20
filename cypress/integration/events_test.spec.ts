import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import EventsPage from "../support/pages/admin_console/manage/events/EventsPage";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import { keycloakBefore } from "../support/util/keycloak_before";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const eventsPage = new EventsPage();
const realmSettingsPage = new RealmSettingsPage();
const masthead = new Masthead();

describe("Search events test", function () {
  describe("Search events dropdown", function () {
    beforeEach(function () {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
    });

    it("Check search dropdown display", () => {
      eventsPage.shouldDisplay();
    });

    it("Check search form fields display", () => {
      eventsPage.shouldHaveFormFields();
    });

    it("Check event type dropdown options exist", () => {
      eventsPage.shouldHaveEventTypeOptions();
    });

    it("Check `search events` button disabled by default", () => {
      eventsPage.shouldHaveSearchBtnDisabled();
    });

    it.skip("Check search and removal works", () => {
      sidebarPage.goToRealmSettings();
      cy.getId("rs-realm-events-tab").click();

      cy.get("#eventsEnabled-switch-on")
        .should("exist")
        .then((exist) => {
          if (exist) {
            sidebarPage.goToEvents();
            eventsPage.shouldDoSearchAndRemoveChips();
          } else {
            realmSettingsPage
              .toggleSwitch(realmSettingsPage.enableEvents)
              .save(realmSettingsPage.eventsUserSave);

            masthead.checkNotificationMessage(
              "Successfully saved configuration"
            );
            sidebarPage.goToEvents();
            eventsPage.shouldDoSearchAndRemoveChips();
          }
        });
    });

    it("Check `search events` button enabled", () => {
      eventsPage.shouldHaveSearchBtnEnabled();
    });
  });
});
