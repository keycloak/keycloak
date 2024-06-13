import CommonPage from "../../../../../CommonPage";
import SettingsTab from "./authorization_subtabs/SettingsTab";
import ResourcesTab from "./authorization_subtabs/ResourcesTab";
import ScopesTab from "./authorization_subtabs/ScopesTab";
import PoliciesTab from "./authorization_subtabs/PoliciesTab";
import PermissionsTab from "./authorization_subtabs/PermissionsTab";
import EvaluateTab from "./authorization_subtabs/EvaluateTab";
import ExportTab from "./authorization_subtabs/ExportTab";

enum AuthorizationSubTab {
  Settings = "Settings",
  Resources = "Resources",
  Scopes = "Scopes",
  Policies = "Policies",
  Permissions = "Permissions",
  Evaluate = "Evaluate",
  Export = "Export",
}

export default class AuthorizationTab extends CommonPage {
  #settingsSubTab = new SettingsTab();
  #resourcesSubTab = new ResourcesTab();
  #scopesSubTab = new ScopesTab();
  #policiesSubTab = new PoliciesTab();
  #permissionsSubTab = new PermissionsTab();
  #evaluateSubTab = new EvaluateTab();
  #exportSubTab = new ExportTab();

  goToSettingsSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Settings, 1);
    return this.#settingsSubTab;
  }

  goToResourcesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Resources, 1);
    return this.#resourcesSubTab;
  }

  goToScopesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Scopes, 1);
    return this.#scopesSubTab;
  }

  goToPoliciesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Policies, 1);
    return this.#policiesSubTab;
  }

  goToPermissionsSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Permissions, 1);
    return this.#permissionsSubTab;
  }

  goToEvaluateSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Evaluate, 1);
    return this.#evaluateSubTab;
  }

  goToExportSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Export, 1);
    return this.#exportSubTab;
  }
}
