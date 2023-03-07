import CommonPage from "../../../../../../CommonPage";
import CreateAuthorizationScopePage from "../../CreateAuthorizationScopePage";

export default class ScopesTab extends CommonPage {
  private createAuthorizationScopePage = new CreateAuthorizationScopePage();
  private createScopeBtn = "no-authorization-scopes-empty-action";

  createAuthorizationScope() {
    cy.findByTestId(this.createScopeBtn).click();
    return this.createAuthorizationScopePage;
  }
}
