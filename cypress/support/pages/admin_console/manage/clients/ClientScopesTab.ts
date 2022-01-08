export default class ClientScopesTab {
  private clientScopesTab = "#pf-tab-clientScopes-clientScopes";

  goToClientScopesTab() {
    cy.get(this.clientScopesTab).click();
    return this;
  }
}
