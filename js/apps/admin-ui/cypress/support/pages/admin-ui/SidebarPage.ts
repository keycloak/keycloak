import CommonElements from "../CommonElements";

export default class SidebarPage extends CommonElements {
  #realmsDrpDwn = "realmSelector";
  #createRealmBtn = "add-realm";

  #clientsBtn = "#nav-item-clients";
  #clientScopesBtn = "#nav-item-client-scopes";
  #realmRolesBtn = "#nav-item-roles";
  #usersBtn = "#nav-item-users";
  #groupsBtn = "#nav-item-groups";
  #sessionsBtn = "#nav-item-sessions";
  #eventsBtn = "#nav-item-events";

  #realmSettingsBtn = "#nav-item-realm-settings";
  #authenticationBtn = "#nav-item-authentication";
  #identityProvidersBtn = "#nav-item-identity-providers";
  #userFederationBtn = "#nav-item-user-federation";

  realmsElements = '[id="realm-select"] li';

  showCurrentRealms(length: number) {
    cy.findByTestId(this.#realmsDrpDwn).click();
    cy.get(this.realmsElements).contains("Loading realms…").should("not.exist");
    cy.get(this.realmsElements).should(
      "have.length",
      length + 1, // account for button
    );
    cy.findByTestId(this.#realmsDrpDwn).click({ force: true });

    return this;
  }

  realmExists(realmName: string, exists = true) {
    cy.findByTestId(this.#realmsDrpDwn).click();
    cy.get(this.realmsElements).contains("Loading realms…").should("not.exist");
    cy.get(this.realmsElements)
      .contains(realmName)
      .should((exists ? "" : "not.") + "exist");
    cy.findByTestId(this.#realmsDrpDwn).click();

    return this;
  }

  getCurrentRealm() {
    return cy.findByTestId(this.#realmsDrpDwn).scrollIntoView().invoke("text");
  }

  goToRealm(realmName: string) {
    this.waitForPageLoad();
    cy.findByTestId(this.#realmsDrpDwn).click();
    cy.get(this.realmsElements).contains("Loading realms…").should("not.exist");
    cy.get(this.realmsElements).contains(realmName).click();
    this.waitForPageLoad();

    return this;
  }

  goToCreateRealm() {
    this.waitForPageLoad();
    cy.findByTestId(this.#realmsDrpDwn).click();
    cy.findByTestId(this.#createRealmBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToClients() {
    this.waitForPageLoad();
    cy.get(this.#clientsBtn).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToClientScopes() {
    this.waitForPageLoad();
    cy.get(this.#clientScopesBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToRealmRoles() {
    cy.get(this.#realmRolesBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToUsers() {
    this.waitForPageLoad();
    cy.get(this.#usersBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToGroups() {
    this.waitForPageLoad();
    cy.get(this.#groupsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToSessions() {
    this.waitForPageLoad();
    cy.get(this.#sessionsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToEvents() {
    this.waitForPageLoad();
    cy.get(this.#eventsBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToRealmSettings() {
    this.waitForPageLoad();
    cy.get(this.#realmSettingsBtn).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToAuthentication() {
    this.waitForPageLoad();
    cy.get(this.#authenticationBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToIdentityProviders() {
    this.waitForPageLoad();
    cy.get(this.#identityProvidersBtn).click();
    this.waitForPageLoad();

    return this;
  }

  goToUserFederation() {
    this.waitForPageLoad();
    cy.get(this.#userFederationBtn).click();
    this.waitForPageLoad();

    return this;
  }

  waitForPageLoad() {
    cy.get('[role="progressbar"]').should("not.exist");
    return this;
  }

  checkRealmSettingsLinkContainsText(expectedText: string) {
    cy.get(this.#realmSettingsBtn).should("contain", expectedText);
  }
}
