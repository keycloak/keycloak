import CommonPage from "../../../CommonPage";
import UserEventsTab from "./tabs/UserEventsTab";
import AdminEventsTab from "./tabs/AdminEventsTab";

export enum EventsTab {
  UserEvents = "User events",
  AdminEvents = "Admin events",
}

export default class EventsPage extends CommonPage {
  #userEventsTab = new UserEventsTab();
  #adminEventsTab = new AdminEventsTab();

  goToUserEventsTab() {
    this.tabUtils().clickTab(EventsTab.UserEvents);
    return this.#userEventsTab;
  }

  goToAdminEventsTab() {
    this.tabUtils().clickTab(EventsTab.AdminEvents);
    return this.#adminEventsTab;
  }
}
