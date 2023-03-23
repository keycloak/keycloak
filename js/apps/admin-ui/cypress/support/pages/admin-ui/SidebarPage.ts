import CommonElements from "../CommonElements";

export default class SidebarPage extends CommonElements {
  private realmsDrpDwn = "realmSelectorToggle";
  private createRealmBtn = "add-realm";

  private clientsBtn = "#nav-item-clients";
  private clientScopesBtn = "#nav-item-client-scopes";
  private realmRolesBtn = "#nav-item-roles";
  private usersBtn = "#nav-item-users";
  private groupsBtn = "#nav-item-groups";
  private sessionsBtn = "#nav-item-sessions";
  private eventsBtn = "#nav-item-events";

  private realmSettingsBtn = "#nav-item-realm-settings";
  private authenticationBtn = "#nav-item-authentication";
  private identityProvidersBtn = "#nav-item-identity-providers";
  private userFederationBtn = "#nav-item-user-federation";

  showCurrentRealms(length: number) {
    cy.findByTestId(this.realmsDrpDwn).click();
    cy.get('[data-testid="realmSelector"] li').should(
      "have.length",
      length + 1 // account for button
    );
    cy.findByTestId(this.realmsDrpDwn).click({ force: true });
  }

  getCurrentRealm() {
    return cy.findByTestId(this.realmsDrpDwn).scrollIntoView().invoke("text");
  }

  goToRealm(realmName: string) {
    this.waitForPageLoad();
    cy.findByTestId(this.realmsDrpDwn)
      .click()
      .parent()
      .contains(realmName)
      .click();
    this.waitForPageLoad();

    return this;
  }

  goToCreateRealm() {
    this.waitForPageLoad();
    cy.findByTestId(this.realmsDrpDwn).click();
    cy.findByTestId(this.createRealmBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToClients() {
    this.waitForPageLoad();
    cy.get(this.clientsBtn).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToClientScopes() {
    this.waitForPageLoad();
    cy.get(this.clientScopesBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToRealmRoles() {
    cy.get(this.realmRolesBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToUsers() {
    this.waitForPageLoad();
    cy.get(this.usersBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToGroups() {
    this.waitForPageLoad();
    cy.get(this.groupsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToSessions() {
    this.waitForPageLoad();
    cy.get(this.sessionsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToEvents() {
    this.waitForPageLoad();
    cy.get(this.eventsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToRealmSettings() {
    this.waitForPageLoad();
    cy.get(this.realmSettingsBtn).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToAuthentication() {
    this.waitForPageLoad();
    cy.get(this.authenticationBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToIdentityProviders() {
    this.waitForPageLoad();
    cy.get(this.identityProvidersBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToUserFederation() {
    this.waitForPageLoad();
    cy.get(this.userFederationBtn).click();
    this.waitForPageLoad();

    return this;
  }

  waitForPageLoad() {
    cy.get('[role="progressbar"]').should("not.exist");
    return this;
  }
}
