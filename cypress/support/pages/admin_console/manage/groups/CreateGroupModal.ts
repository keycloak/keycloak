export default class CreateGroupModal {
  private openButton = "openCreateGroupModal";
  private nameInput = "groupNameInput";
  private createButton = "createGroup";

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
}
