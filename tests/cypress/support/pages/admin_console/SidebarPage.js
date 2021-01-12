export default class SidebarPage {

    constructor() {
        this.realmsDrpDwn = '#realm-select-toggle';
        this.realmsList = '#realm-select ul';
        this.createRealmBtn = '#realm-select li:last-child a';

        this.clientsBtn = '#nav-item-clients';
        this.clientScopesBtn = '#nav-item-client-scopes';
        this.realmRolesBtn = '#nav-item-roles';
        this.usersBtn = '#nav-item-users';
        this.groupsBtn = '#nav-item-groups';
        this.sessionsBtn = '#nav-item-sessions';
        this.eventsBtn = '#nav-item-events';

        this.realmSettingsBtn = '#nav-item-realm-settings';
        this.authenticationBtn = '#nav-item-authentication';
        this.identityProvidersBtn = '#nav-item-identity-providers';
        this.userFederationBtn = '#nav-item-user-federation';
    }

    getCurrentRealm() {
        return cy.get(this.realmsDrpDwn).invoke('text');
    }

    goToRealm(realmName) {
        cy.get(this.realmsDrpDwn).click();
        cy.get(this.realmsList).contains(realmName).click();

        return this;
    }

    goToCreateRealm() {
        cy.get(this.realmsDrpDwn).click();
        cy.get(this.createRealmBtn).click();

        return this;
    }

    goToClients() {
        cy.get(this.clientsBtn).click();

        return this;
    }

    goToClientScopes() {
        cy.get(this.clientScopesBtn).click();

        return this;
    }

    goToRealmRoles() {
        cy.get(this.realmRolesBtn).click();

        return this;
    }

    goToUsers() {
        cy.get(this.usersBtn).click();

        return this;
    }

    goToGroups() {
        cy.get(this.groupsBtn).click();

        return this;
    }

    goToSessions() {
        cy.get(this.sessionsBtn).click();

        return this;
    }

    goToEvents() {
        cy.get(this.eventsBtn).click();

        return this;
    }

    goToRealmSettings() {
        cy.get(this.realmSettingsBtn).click();

        return this;
    }

    goToAuthentication() {
        cy.get(this.authenticationBtn).click();

        return this;
    }

    goToIdentityProviders() {
        cy.get(this.identityProvidersBtn).click();

        return this;
    }

    goToUserFederation() {
        cy.get(this.userFederationBtn).click();

        return this;
    }
}