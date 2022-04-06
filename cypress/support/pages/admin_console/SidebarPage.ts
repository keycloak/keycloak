export default class SidebarPage {
  private realmsDrpDwn = "#realm-select button.pf-c-dropdown__toggle";
  private realmsList = '#realm-select ul[role="menu"]';
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
    cy.get(this.realmsDrpDwn).scrollIntoView().click({ force: true });
    cy.get(this.realmsList)
      .children("li")
      .should("have.length", length + 1); // account for button
    cy.get(this.realmsDrpDwn).click({ force: true });
  }

  getCurrentRealm() {
    return cy.get(this.realmsDrpDwn).scrollIntoView().invoke("text");
  }

  goToRealm(realmName: string) {
    this.waitForPageLoad();
    cy.get(this.realmsDrpDwn).scrollIntoView().click({ force: true });
    cy.get(this.realmsList).contains(realmName).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToCreateRealm() {
    this.waitForPageLoad();
    cy.get(this.realmsDrpDwn).scrollIntoView().click({ force: true });
    cy.findByTestId(this.createRealmBtn).click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToClients() {
    this.waitForPageLoad();
    cy.get(this.clientsBtn).scrollIntoView().click({ force: true });
    this.waitForPageLoad();

    return this;
  }

  goToClientScopes() {
    this.waitForPageLoad();
    cy.get(this.clientScopesBtn).scrollIntoView().click();
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
