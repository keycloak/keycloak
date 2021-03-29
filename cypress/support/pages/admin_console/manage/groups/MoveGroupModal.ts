export default class MoveGroupModal {
  private moveButton = "moveGroup";
  private title = ".pf-c-modal-box__title";

  clickRow(groupName: string) {
    cy.getId(groupName).click();
    return this;
  }

  checkTitle(title: string) {
    cy.get(this.title).should("have.text", title);
    return this;
  }

  clickMove() {
    cy.getId(this.moveButton).click();
    return this;
  }
}
