export default class ClientScopesTab {
  private clientScopesTab = "clientScopesTab";

  goToClientScopesTab() {
    cy.findByTestId(this.clientScopesTab).click();
    return this;
  }
}
