import CommonPage from "../../../../../CommonPage";
import SetupTab from "./clientscopes_subtabs/SetupTab";
import EvaluateTab from "./clientscopes_subtabs/EvaluateTab";
import DedicatedScopesPage from "../DedicatedScopesPage";

enum ClientScopesSubTab {
  Setup = "Setup",
  Evaluate = "Evaluate",
}

export default class ClientScopesTab extends CommonPage {
  private setupSubTab = new SetupTab();
  private evaluateSubTab = new EvaluateTab();
  private dedicatedScopesPage = new DedicatedScopesPage();

  goToSetupSubTab() {
    this.tabUtils().clickTab(ClientScopesSubTab.Setup);
    return this.setupSubTab;
  }

  goToEvaluateSubTab() {
    this.tabUtils().clickTab(ClientScopesSubTab.Evaluate);
    return this.evaluateSubTab;
  }

  clickDedicatedScope(clientId: string) {
    cy.intercept("/admin/realms/master/clients/*").as("get");
    cy.findByText(`${clientId}-dedicated`).click();
    cy.wait("@get");
    return this.dedicatedScopesPage;
  }
}
