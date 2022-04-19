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
  private settingsSubTab = new SettingsTab();
  private resourcesSubTab = new ResourcesTab();
  private scopesSubTab = new ScopesTab();
  private policiesSubTab = new PoliciesTab();
  private permissionsSubTab = new PermissionsTab();
  private evaluateSubTab = new EvaluateTab();
  private exportSubTab = new ExportTab();

  goToSettingsSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Settings);
    return this.settingsSubTab;
  }

  goToResourcesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Resources);
    return this.resourcesSubTab;
  }

  goToScopesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Scopes);
    return this.scopesSubTab;
  }

  goToPoliciesSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Policies);
    return this.policiesSubTab;
  }

  goToPermissionsSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Permissions);
    return this.permissionsSubTab;
  }

  goToEvaluateSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Evaluate);
    return this.evaluateSubTab;
  }

  goToExportSubTab() {
    this.tabUtils().clickTab(AuthorizationSubTab.Export);
    return this.exportSubTab;
  }
}
