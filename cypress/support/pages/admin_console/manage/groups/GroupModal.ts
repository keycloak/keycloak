export default class GroupModal {
  private nameInput = "groupNameInput";
  private createButton = "createGroup";
  private renameButton = "renameGroup";

  open(name?: string) {
    if (name) {
      cy.getId(name).click();
    } else {
      cy.get("button").contains("Create").click();
    }
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
