export default class CreateClientPage {

    constructor() {
        this.goToClientsBtn = '[href="#/realms/master/clients"]';
        this.createClientBtn = '#createClient';

        this.clientIdInput = '#clientId';
        this.rootUrlInput = '#rootUrl';

        this.saveBtn = '[kc-save]';
    }

    goToClients() {
        cy.get(this.goToClientsBtn).click();

        return this;
    }

    addNewAdminConsole() {
        cy.get(this.createClientBtn).click();

        cy.get(this.clientIdInput).type('security-admin-console-v2');
        cy.get(this.rootUrlInput).type('http://localhost:8080/');

        cy.get(this.saveBtn).click();

        return this;
    }
}