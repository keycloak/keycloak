import CommonPage from "../../../../../CommonPage";
import SetupTab from "./clientscopes_subtabs/SetupTab";
import EvaluateTab from "./clientscopes_subtabs/EvaluateTab";
import DedicatedScopesPage from "../DedicatedScopesPage";

export enum ClientScopesSubTab {
  Setup = "Setup",
  Evaluate = "Evaluate",
}

export default class ClientScopesTab extends CommonPage {
  #setupSubTab = new SetupTab();
  #evaluateSubTab = new EvaluateTab();
  #dedicatedScopesPage = new DedicatedScopesPage();

  goToSetupSubTab() {
    this.tabUtils().clickTab(ClientScopesSubTab.Setup);
    return this.#setupSubTab;
  }

  goToEvaluateSubTab() {
    this.tabUtils().clickTab(ClientScopesSubTab.Evaluate);
    return this.#evaluateSubTab;
  }

  clickDedicatedScope(clientId: string) {
    cy.intercept("/admin/realms/*/clients/*").as("get");
    cy.findByText(`${clientId}-dedicated`).click();
    cy.wait("@get");
    return this.#dedicatedScopesPage;
  }
}
