export default class ClientScopesTab {
  private clientScopesTab = "clientScopesTab";

  goToClientScopesTab() {
    cy.findByTestId(this.clientScopesTab).click();
    return this;
  }

  clickDedicatedScope(clientId: string) {
    cy.findByText(`${clientId}-dedicated`).click();
    return this;
  }
}
