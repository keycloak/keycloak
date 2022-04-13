export default class CreateProviderPage {
  private github = "github";
  private clientIdField = "clientId";
  private clientIdError = "#kc-client-secret-helper";
  private clientSecretField = "clientSecret";
  private discoveryEndpoint = "discoveryEndpoint";
  private authorizationUrl = "authorizationUrl";
  private addButton = "createProvider";
  private saveButton = "save";
  private ssoServiceUrl = "sso-service-url";
  private authnContextClassRefs = "classref-field";
  private authnContextDeclRefs = "declref-field";
  private addProvider = "Add provider";
  private addClassRef = "Add AuthnContext ClassRef";
  private addDeclRef = "Add AuthnContext DeclRef";

  checkVisible(name: string) {
    cy.findByTestId(`${name}-card`).should("exist");
    return this;
  }

  clickCard(name: string) {
    cy.findByTestId(`${name}-card`).click();
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
    cy.findByTestId(this.addButton).should(
      !disabled ? "not." : "" + "be.disabled"
    );
    return this;
  }

  clickAdd() {
    cy.findByTestId(this.addButton).click();
    return this;
  }

  clickSave() {
    cy.findByTestId(this.saveButton).click();
    return this;
  }

  clickClassRefsAdd() {
    cy.contains(this.addClassRef).click();
    return this;
  }

  clickDeclRefsAdd() {
    cy.contains(this.addDeclRef).click();
    return this;
  }

  clickCreateDropdown() {
    cy.contains(this.addProvider).click();
    return this;
  }

  clickItem(item: string) {
    cy.findByTestId(item).click();
    return this;
  }

  fill(id: string, secret = "") {
    cy.findByTestId(this.clientIdField).clear();

    if (id) {
      cy.findByTestId(this.clientIdField).type(id);
    }

    if (secret) {
      cy.findByTestId(this.clientSecretField).type(secret);
    }

    return this;
  }

  fillDiscoveryUrl(value: string) {
    cy.findByTestId(this.discoveryEndpoint).type("x");
    cy.findByTestId(this.discoveryEndpoint).clear().type(value).blur();
    return this;
  }

  fillAuthnContextClassRefs(value: string) {
    cy.findByTestId(this.authnContextClassRefs).type("x");
    cy.findByTestId(this.authnContextClassRefs).clear().type(value).blur();
    return this;
  }

  fillAuthnContextDeclRefs(value: string) {
    cy.findByTestId(this.authnContextDeclRefs).type("x");
    cy.findByTestId(this.authnContextDeclRefs).clear().type(value).blur();
    return this;
  }

  fillSsoServiceUrl(value: string) {
    cy.findByTestId(this.ssoServiceUrl).type("x");
    cy.findByTestId(this.ssoServiceUrl).clear().type(value).blur();
    return this;
  }

  shouldBeSuccessful() {
    cy.findByTestId(this.discoveryEndpoint).should(
      "have.class",
      "pf-m-success"
    );
    return this;
  }

  shouldHaveAuthorizationUrl(value: string) {
    cy.findByTestId(this.authorizationUrl).should("have.value", value);
    return this;
  }
}
