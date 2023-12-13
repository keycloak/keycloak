import CommonPage from "../../../../../../CommonPage";
import CreateAuthorizationScopePage from "../../CreateAuthorizationScopePage";

export default class ScopesTab extends CommonPage {
  #createAuthorizationScopePage = new CreateAuthorizationScopePage();
  #createScopeBtn = "no-authorization-scopes-empty-action";

  createAuthorizationScope() {
    cy.findByTestId(this.#createScopeBtn).click();
    return this.#createAuthorizationScopePage;
  }
}
