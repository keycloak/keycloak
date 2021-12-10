export default class ClientScopesTab {
  private clientScopesTab = "#pf-tab-clientScopes-clientScopes";

  goToTab() {
    cy.get(this.clientScopesTab).click();
    return this;
  }
}
