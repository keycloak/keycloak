import CommonPage from "../../../../CommonPage";
import SettingsTab from "./tabs/SettingsTab";
import MappersTab from "./tabs/MappersTab";
import ScopeTab from "./tabs/ScopeTab";

export enum ClientScopeDetailsTab {
  SettingsTab = "Settings",
  MappersTab = "Mappers",
  Scope = "Scope",
}

export default class ClientScopeDetailsPage extends CommonPage {
  #settingsTab = new SettingsTab();
  #scopesTab = new ScopeTab();
  #mappersTab = new MappersTab();

  goToSettingsTab() {
    this.tabUtils().clickTab(ClientScopeDetailsTab.SettingsTab);
    return this.#settingsTab;
  }

  goToMappersTab() {
    this.tabUtils().clickTab(ClientScopeDetailsTab.MappersTab);
    return this.#mappersTab;
  }

  goToScopesTab() {
    this.tabUtils().clickTab(ClientScopeDetailsTab.Scope);
    return this.#scopesTab;
  }
}
