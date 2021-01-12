export default class CreateClientScopePage {

    constructor() {
        this.settingsTab = '.pf-c-tabs__item:nth-child(1)';
        this.mappersTab = '.pf-c-tabs__item:nth-child(2)';

        this.clientScopeNameInput = '#kc-name';
        this.clientScopeNameError = '#kc-name-helper';
        this.clientScopeDescriptionInput = '#kc-description';
        this.clientScopeTypeDrpDwn = '#kc-protocol';
        this.clientScopeTypeList = '#kc-protocol + ul';
        this.displayOnConsentSwitch = '[id="kc-display.on.consent.screen"] + .pf-c-switch__toggle';
        this.consentScreenTextInput = '#kc-consent-screen-text';
        this.includeInTokenSwitch = '[id="includeInTokenScope"] + .pf-c-switch__toggle';
        this.displayOrderInput = '#kc-gui-order';

        this.saveBtn = '[type="submit"]';
        this.cancelBtn = '[type="button"]';
    }

    //#region General Settings
    fillClientScopeData(name, description = '', consentScreenText = '', displayOrder = '') {
        cy.get(this.clientScopeNameInput).clear();

        if(name) {
            cy.get(this.clientScopeNameInput).type(name);
        }

        if(description) {
            cy.get(this.clientScopeDescriptionInput).type(description);
        }

        if(consentScreenText) {
            cy.get(this.consentScreenTextInput).type(consentScreenText);
        }

        if(displayOrder) {
            cy.get(this.displayOrderInput).type(displayOrder);
        }

        return this;
    }

    selectClientScopeType(clientScopeType) {
        cy.get(this.clientScopeTypeDrpDwn).click();
        cy.get(this.clientScopeTypeList).contains(clientScopeType).click();

        return this;
    }

    checkClientNameRequiredMessage(exist = true) {
        cy.get(this.clientScopeNameError).should((!exist ? 'not.': '') + 'exist');

        return this;
    }
    
    switchDisplayOnConsentScreen() {
        cy.get(this.displayOnConsentSwitch).click();

        return this;
    }

    switchIncludeInTokenScope() {
        cy.get(this.includeInTokenSwitch).click();

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