export default class GroupModal {
  private openButton = "openCreateGroupModal";
  private nameInput = "groupNameInput";
  private createButton = "createGroup";
  private renameButton = "renameGroup";

  open(name?: string) {
    cy.getId(name || this.openButton).click();
    return this;
  }

  fillGroupForm(name = "") {
    cy.getId(this.nameInput).clear();
    cy.getId(this.nameInput).type(name);
    return this;
  }

  clickCreate() {
    cy.getId(this.createButton).click();
    return this;
  }

  clickRename() {
    cy.getId(this.renameButton).click();
    return this;
  }
}
