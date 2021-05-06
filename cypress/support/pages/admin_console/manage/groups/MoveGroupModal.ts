export default class MoveGroupModal {
  private moveButton = "joinGroup";
  private title = ".pf-c-modal-box__title";

  clickRow(groupName: string) {
    cy.getId(groupName).click();
    return this;
  }

  clickRoot() {
    cy.get(".pf-c-breadcrumb__item > button").click();
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
