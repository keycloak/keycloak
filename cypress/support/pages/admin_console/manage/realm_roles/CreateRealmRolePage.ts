export default class CreateRealmRolePage {
  realmRoleNameInput: string;
  realmRoleNameError: string;
  realmRoleDescriptionInput: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    this.realmRoleNameInput = "#kc-name";
    this.realmRoleNameError = "#kc-name-helper";
    this.realmRoleDescriptionInput = "#kc-role-description";

    this.saveBtn = 'realm-roles-save-button';
    this.cancelBtn = '[type="button"]';
  }

  //#region General Settings
  fillRealmRoleData(name: string, description = "") {
    cy.get(this.realmRoleNameInput).clear();

    if (name) {
      cy.get(this.realmRoleNameInput).type(name);
    }

    if (description) {
      cy.get(this.realmRoleDescriptionInput).type(description);
    }

    return this;
  }

  checkRealmRoleNameRequiredMessage(exist = true) {
    cy.get(this.realmRoleNameError).should((!exist ? "not." : "") + "exist");

    return this;
  }
  //#endregion

  save() {
    cy.getId(this.saveBtn).click();

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
