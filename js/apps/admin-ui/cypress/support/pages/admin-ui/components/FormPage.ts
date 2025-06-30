import CommonElements from "../../CommonElements";

export default class FormPage extends CommonElements {
  constructor() {
    super(".pf-v5-c-form:visible");
  }

  save() {
    cy.get(this.primaryBtn).contains("Save").click();
    return this;
  }

  add() {
    cy.get(this.primaryBtn).contains("Add").click();
    return this;
  }

  cancel() {
    cy.get(this.secondaryBtnLink).contains("Cancel").click();
    return this;
  }

  revert() {
    cy.get(this.secondaryBtnLink).contains("Revert").click();
    return this;
  }

  checkSaveButtonIsDisabled(disabled: boolean) {
    this.checkElementIsDisabled(
      cy.get(this.primaryBtn).contains("Save"),
      disabled,
    );
    return this;
  }
}
