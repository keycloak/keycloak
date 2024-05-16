import CommonPage from "../../../../CommonPage";
import DedicatedScopesMappersTab from "./DedicatedScopesMappersTab";
import DedicatedScopesScopeTab from "./DedicatedScopesScopeTab";

export enum DedicatedScopesTab {
  Mappers = "Mappers",
  Scope = "Scope",
}

export default class DedicatedScopesPage extends CommonPage {
  #mappersTab = new DedicatedScopesMappersTab();
  #scopeTab = new DedicatedScopesScopeTab();

  goToMappersTab() {
    this.tabUtils().clickTab(DedicatedScopesTab.Mappers);
    return this.#mappersTab;
  }

  goToScopeTab() {
    this.tabUtils().clickTab(DedicatedScopesTab.Scope);
    return this.#scopeTab;
  }
}
