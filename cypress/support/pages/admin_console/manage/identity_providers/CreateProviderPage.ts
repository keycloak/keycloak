export default class CreateProviderPage {
  private github = "github";
  private addProviderDropdown = "addProviderDropdown";
  private clientIdField = "clientId";
  private clientIdError = "#kc-client-secret-helper";
  private clientSecretField = "clientSecret";
  private discoveryEndpoint = "discoveryEndpoint";
  private authorizationUrl = "authorizationUrl";
  private addButton = "createProvider";

  checkVisible(name: string) {
    cy.getId(`${name}-card`).should("exist");
    return this;
  }

  clickCard(name: string) {
    cy.getId(`${name}-card`).click();
    return this;
  }

  clickGitHubCard() {
    this.clickCard(this.github);
    return this;
  }

  checkGitHubCardVisible() {
    this.checkVisible(this.github);
    return this;
  }

  checkClientIdRequiredMessage(exist = true) {
    cy.get(this.clientIdError).should((!exist ? "not." : "") + "exist");

    return this;
  }

  checkAddButtonDisabled(disabled = true) {
    cy.getId(this.addButton).should(!disabled ? "not." : "" + "be.disabled");
    return this;
  }

  clickAdd() {
    cy.getId(this.addButton).click();
    return this;
  }

  clickCreateDropdown() {
    cy.getId(this.addProviderDropdown, { timeout: 10000 }).click();
    return this;
  }

  clickItem(item: string) {
    cy.getId(item).click();
    return this;
  }

  fill(id: string, secret = "") {
    cy.getId(this.clientIdField).clear();

    if (id) {
      cy.getId(this.clientIdField).type(id);
    }

    if (secret) {
      cy.getId(this.clientSecretField).type(secret);
    }

    return this;
  }

  fillDiscoveryUrl(value: string) {
    cy.getId(this.discoveryEndpoint).type(value).blur();
    return this;
  }

  shouldBeSuccessful() {
    cy.getId(this.discoveryEndpoint).should("have.class", "pf-m-success");
    return this;
  }

  shouldHaveAuthorizationUrl(value: string) {
    cy.getId(this.authorizationUrl).should("have.value", value);
    return this;
  }
}
