import CommonPage from "../../../../CommonPage";
import AdminEventsSettingsTab from "./realmsettings_events_subtabs/AdminEventsSettingsTab";
import EventListenersTab from "./realmsettings_events_subtabs/EventListenersTab";
import UserEventsSettingsTab from "./realmsettings_events_subtabs/UserEventsSettingsTab";

enum RealmSettingsEventsSubTab {
  EventListeners = "Event listeners",
  UserEventsSettings = "User events settings",
  AdminEventsSettings = "Admin events settings",
}

export default class RealmSettingsEventsTab extends CommonPage {
  #eventListenersTab = new EventListenersTab();
  #userEventsSettingsTab = new UserEventsSettingsTab();
  #adminEventsSettingsTab = new AdminEventsSettingsTab();

  goToEventListenersSubTab() {
    this.tabUtils().clickTab(RealmSettingsEventsSubTab.EventListeners, 1);
    return this.#eventListenersTab;
  }

  goToUserEventsSettingsSubTab() {
    this.tabUtils().clickTab(RealmSettingsEventsSubTab.UserEventsSettings, 1);
    return this.#userEventsSettingsTab;
  }

  goToAdminEventsSettingsSubTab() {
    this.tabUtils().clickTab(RealmSettingsEventsSubTab.AdminEventsSettings, 1);
    return this.#adminEventsSettingsTab;
  }
}
