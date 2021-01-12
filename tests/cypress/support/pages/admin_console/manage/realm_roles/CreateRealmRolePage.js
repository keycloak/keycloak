export default class CreateRealmRolePage {

    constructor() {
        this.realmRoleNameInput = '#kc-name';
        this.realmRoleNameError = '#kc-name-helper';
        this.realmRoleDescriptionInput = '#kc-role-description';

        this.saveBtn = '[type="submit"]';
        this.cancelBtn = '[type="button"]';
    }

    //#region General Settings
    fillRealmRoleData(name, description = '') {
        cy.get(this.realmRoleNameInput).clear();

        if(name) {
            cy.get(this.realmRoleNameInput).type(name);
        }

        if(description) {
            cy.get(this.realmRoleDescriptionInput).type(description);
        }

        return this;
    }

    checkRealmRoleNameRequiredMessage(exist = true) {
        cy.get(this.realmRoleNameError).should((!exist ? 'not.': '') + 'exist');

        return this;
    }
    //#endregion

    save() {
        cy.get(this.saveBtn).click();

        return this;
    }

    cancel() {
        cy.get(this.cancelBtn).click();

        return this;
    }
}