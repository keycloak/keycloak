export default class CreateRealmPage {

    constructor() {
        this.browseBtn = '#kc-realm-filename-browse-button';
        this.clearBtn = '.pf-c-file-upload__file-select button:last-child';
        this.realmFileNameInput = '#kc-realm-filename';
        this.realmNameInput = '#kc-realm-name';
        this.enabledSwitch = '[for="kc-realm-enabled-switch"] span.pf-c-switch__toggle';

        this.createBtn = '.pf-c-form__group:last-child button[type="submit"]';
        this.cancelBtn = '.pf-c-form__group:last-child button[type="button"]';
    }

    fillRealmName(realmName) {
        cy.get(this.realmNameInput).type(realmName);

        return this;
    }

    createRealm() {
        cy.get(this.createBtn).click();

        return this;
    }
}