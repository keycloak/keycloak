export default class GroupModal {
  private nameInput = "groupNameInput";
  private createButton = "createGroup";
  private renameButton = "renameGroup";

  open(name?: string) {
    if (name) {
      cy.findByTestId(name).click();
    } else {
      cy.get("button").contains("Create").click();
    }
    return this;
  }

  fillGroupForm(name = "") {
    cy.findByTestId(this.nameInput).clear().type(name);
    return this;
  }

  clickCreate() {
    cy.findByTestId(this.createButton).click();

    return this;
  }

  clickRename() {
    cy.findByTestId(this.renameButton).click();
    return this;
  }
}
