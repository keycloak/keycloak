import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import UserEventsTab, {
  UserEventSearchData,
} from "../support/pages/admin_console/manage/events/tabs/UserEventsTab";
import AdminEventsTab from "../support/pages/admin_console/manage/events/tabs/AdminEventsTab";
import RealmSettingsPage from "../support/pages/admin_console/manage/realm_settings/RealmSettingsPage";
import Masthead from "../support/pages/admin_console/Masthead";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import EventsPage, {
  EventsTab,
} from "../support/pages/admin_console/manage/events/EventsPage";
import ListingPage from "../support/pages/admin_console/ListingPage";
import adminClient from "../support/util/AdminClient";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const userEventsTab = new UserEventsTab();
const eventsPage = new EventsPage();
const adminEventsTab = new AdminEventsTab();
const realmSettingsPage = new RealmSettingsPage();
const masthead = new Masthead();
const listingPage = new ListingPage();

const dateFrom = new Date();
dateFrom.setDate(dateFrom.getDate() - 100);
const dateFromFormatted = `${dateFrom.getFullYear()}-${dateFrom.getMonth()}-${dateFrom.getDay()}`;
const dateTo = new Date();
dateTo.setDate(dateTo.getDate() + 100);
const dateToFormatted = `${dateTo.getFullYear()}-${dateTo.getMonth()}-${dateTo.getDay()}`;

describe("Events tests", () => {
  const eventsTestUser = {
    eventsTestUserId: "",
    userRepresentation: {
      username: "events-test",
      enabled: true,
      credentials: [{ value: "events-test" }],
    },
  };
  const eventsTestUserClientId = "admin-cli";

  before(async () => {
    const result = await adminClient.createUser(
      eventsTestUser.userRepresentation
    );
    eventsTestUser.eventsTestUserId = result.id;
  });

  after(() =>
    adminClient.deleteUser(eventsTestUser.userRepresentation.username)
  );

  describe("User events list", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
    });

    it("Show empty when no save events", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToUserEventsSettingsSubTab()
        .enableSaveEventsSwitch()
        .save()
        .clearUserEvents();

      cy.wait(5000);

      sidebarPage.goToEvents();
      eventsPage.goToUserEventsTab();
      userEventsTab.assertNoSearchResultsExist(true);
    });

    it("Expand item to see more information", () => {
      listingPage.expandRow(0).assertExpandedRowContainText(0, "code_id");
    });
  });

  describe("Search user events list", () => {
    const eventTypes = [
      "LOGOUT",
      "CODE_TO_TOKEN",
      "CODE_TO_TOKEN_ERROR",
      "LOGIN_ERROR",
      "LOGIN",
    ];

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
    });

    it("Check search dropdown display", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToUserEventsSettingsSubTab()
        .disableSaveEventsSwitch()
        .save();

      cy.wait(5000);

      masthead.signOut();
      loginPage.logIn();

      sidebarPage.goToEvents();
      eventsPage.tabUtils().checkIsCurrentTab(EventsTab.UserEvents);
      userEventsTab.assertSearchUserEventDropdownMenuExist(true);
    });

    it("Check user events search form fields display", () => {
      userEventsTab
        .openSearchUserEventDropdownMenu()
        .assertUserSearchDropdownMenuHasLabels();
    });

    it("Check event type dropdown options exist", () => {
      userEventsTab
        .openSearchUserEventDropdownMenu()
        .openEventTypeSelectMenu()
        .clickEventTypeSelectItem(eventTypes[0])
        .clickEventTypeSelectItem(eventTypes[1])
        .clickEventTypeSelectItem(eventTypes[2])
        .clickEventTypeSelectItem(eventTypes[3])
        .closeEventTypeSelectMenu();
    });

    it("Check `search events` button disabled by default", () => {
      userEventsTab
        .openSearchUserEventDropdownMenu()
        .assertSearchEventBtnIsEnabled(false);
    });

    it("Check user events search and removal work", () => {
      userEventsTab
        .searchUserEventByEventType([eventTypes[0]])
        .assertEventTypeChipGroupItemExist(eventTypes[0], true)
        .assertEventTypeChipGroupItemExist(eventTypes[1], false)
        .assertEventTypeChipGroupItemExist(eventTypes[2], false)
        .assertEventTypeChipGroupItemExist(eventTypes[3], false)
        .assertNoSearchResultsExist(true)
        .removeEventTypeChipGroupItem(eventTypes[0])
        .assertEventTypeChipGroupExist(false);
    });

    it("Check for no events logged", () => {
      userEventsTab
        .searchUserEventByUserId("test")
        .assertNoSearchResultsExist(true);
    });

    it("Check `search events` button enabled", () => {
      userEventsTab
        .openSearchUserEventDropdownMenu()
        .assertSearchEventBtnIsEnabled(false)
        .typeUserId("11111")
        .assertSearchEventBtnIsEnabled(true);
    });

    it("Search by user ID", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToUserEventsSettingsSubTab()
        .enableSaveEventsSwitch()
        .save();
      sidebarPage.goToEvents();
      cy.wrap(null).then(() =>
        adminClient.loginUser(
          eventsTestUser.userRepresentation.username,
          eventsTestUser.userRepresentation.credentials[0].value,
          eventsTestUserClientId
        )
      );
      userEventsTab
        .searchUserEventByUserId(eventsTestUser.eventsTestUserId)
        .assertUserIdChipGroupExist(true)
        .assertEventTypeChipGroupExist(false)
        .assertClientChipGroupExist(false)
        .assertDateFromChipGroupExist(false)
        .assertDateToChipGroupExist(false);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by event type", () => {
      userEventsTab
        .searchUserEventByEventType([eventTypes[4]])
        .assertUserIdChipGroupExist(false)
        .assertEventTypeChipGroupExist(true)
        .assertClientChipGroupExist(false)
        .assertDateFromChipGroupExist(false)
        .assertDateToChipGroupExist(false);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by client", () => {
      userEventsTab
        .searchUserEventByClient(eventsTestUserClientId)
        .assertUserIdChipGroupExist(false)
        .assertEventTypeChipGroupExist(false)
        .assertClientChipGroupExist(true)
        .assertDateFromChipGroupExist(false)
        .assertDateToChipGroupExist(false);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by date from", () => {
      userEventsTab
        .searchUserEventByDateFrom(dateFromFormatted)
        .assertUserIdChipGroupExist(false)
        .assertEventTypeChipGroupExist(false)
        .assertClientChipGroupExist(false)
        .assertDateFromChipGroupExist(true)
        .assertDateToChipGroupExist(false);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by dato to", () => {
      userEventsTab
        .searchUserEventByDateTo(dateToFormatted)
        .assertUserIdChipGroupExist(false)
        .assertEventTypeChipGroupExist(false)
        .assertClientChipGroupExist(false)
        .assertDateFromChipGroupExist(false)
        .assertDateToChipGroupExist(true);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by all elements", () => {
      const searchData = new UserEventSearchData();
      searchData.client = eventsTestUserClientId;
      searchData.userId = eventsTestUser.eventsTestUserId;
      searchData.eventType = [eventTypes[4]];
      searchData.dateFrom = dateFromFormatted;
      searchData.dateTo = dateToFormatted;
      userEventsTab
        .searchUserEvent(searchData)
        .assertUserIdChipGroupExist(true)
        .assertEventTypeChipGroupExist(true)
        .assertClientChipGroupExist(true)
        .assertDateFromChipGroupExist(true)
        .assertDateToChipGroupExist(true);
      listingPage.itemsGreaterThan(0);
    });
  });

  describe("Admin events list", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
    });

    it("Show events", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToAdminEventsSettingsSubTab()
        .enableSaveEvents()
        .save();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
      listingPage.itemsGreaterThan(0);
    });

    it("Show empty when no save events", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToAdminEventsSettingsSubTab()
        .disableSaveEvents()
        .save()
        .clearAdminEvents();

      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
      adminEventsTab.assertNoSearchResultsExist(true);
    });
  });

  describe("Search admin events list", () => {
    const resourceTypes = ["REALM"];
    const operationTypes = ["UPDATE"];

    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
    });

    it("Search by resource types", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToAdminEventsSettingsSubTab()
        .enableSaveEvents()
        .save();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
      adminEventsTab
        .searchAdminEventByResourceTypes([resourceTypes[0]])
        .assertResourceTypesChipGroupExist(true);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by operation types", () => {
      adminEventsTab
        .searchAdminEventByOperationTypes([operationTypes[0]])
        .assertOperationTypesChipGroupExist(true);
      listingPage.itemsGreaterThan(0);
    });

    it("Search by resource path", () => {
      adminEventsTab
        .searchAdminEventByResourcePath("test")
        .assertResourcePathChipGroupExist(true);
    });

    it("Search by realm", () => {
      adminEventsTab
        .searchAdminEventByRealm("master")
        .assertRealmChipGroupExist(true);
    });

    it("Search by client", () => {
      adminEventsTab
        .searchAdminEventByClient("admin-cli")
        .assertClientChipGroupExist(true);
    });

    it("Search by user ID", () => {
      adminEventsTab
        .searchAdminEventByUser("test")
        .assertUserChipGroupExist(true);
    });

    it("Search by IP address", () => {
      adminEventsTab
        .searchAdminEventByIpAddress("test")
        .assertIpAddressChipGroupExist(true);
    });

    it("Search by date from", () => {
      adminEventsTab
        .searchAdminEventByDateTo(dateToFormatted)
        .assertDateToChipGroupExist(true);
    });

    it("Search by date to", () => {
      adminEventsTab
        .searchAdminEventByDateFrom(dateFromFormatted)
        .assertDateFromChipGroupExist(true);
    });
  });

  describe("Search admin events", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
    });

    it("Check admin events search form fields display", () => {
      sidebarPage.goToRealmSettings();
      realmSettingsPage
        .goToEventsTab()
        .goToAdminEventsSettingsSubTab()
        .disableSaveEvents()
        .save();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
      adminEventsTab
        .openSearchAdminEventDropdownMenu()
        .assertAdminSearchDropdownMenuHasLabels();
    });

    it("Check `search admin events` button disabled by default", () => {
      adminEventsTab
        .openSearchAdminEventDropdownMenu()
        .assertSearchAdminBtnEnabled(false);
    });

    it("Check admin events search and removal work", () => {
      sidebarPage.goToEvents();
      eventsPage
        .goToAdminEventsTab()
        .searchAdminEventByResourcePath("events/config")
        .assertResourcePathChipGroupItemExist("events/config", true)
        .removeResourcePathChipGroup();
      listingPage.itemContainValue("UPDATE", 3, "UPDATE");
    });

    it("Check for no events logged", () => {
      adminEventsTab
        .searchAdminEventByResourcePath("events/test")
        .assertNoSearchResultsExist(true);
    });

    it("Check `search admin events` button enabled", () => {
      adminEventsTab
        .openSearchAdminEventDropdownMenu()
        .typeIpAddress("11111")
        .assertSearchAdminBtnEnabled(true);
    });
  });

  describe("Check more button opens auth and representation dialogs", () => {
    beforeEach(() => {
      keycloakBefore();
      loginPage.logIn();
      sidebarPage.goToEvents();
      eventsPage.goToAdminEventsTab();
    });

    it("Check auth dialog opens and is not empty", () => {
      listingPage.clickRowDetails("UPDATE").clickDetailMenu("Auth");
      adminEventsTab.assertAuthDialogIsNotEmpty();
    });

    it("Check representation dialog opens and is not empty", () => {
      listingPage.clickRowDetails("UPDATE").clickDetailMenu("Representation");
      adminEventsTab.assertRepresentationDialogIsNotEmpty();
    });
  });
});
