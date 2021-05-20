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
    cy.getId(this.nameInput).clear().type(name);
    return this;
  }

  clickCreate() {
    cy.intercept("/auth/admin/realms/master/groups/*/members").as(
      "groupCreate"
    );
    cy.getId(this.createButton).click();
    cy.wait(["@groupCreate"]);

    return this;
  }

  clickRename() {
    cy.getId(this.renameButton).click();
    return this;
  }
}
