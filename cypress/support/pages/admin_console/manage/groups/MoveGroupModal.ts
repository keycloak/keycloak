export default class MoveGroupModal {
  private moveButton = "groups:moveHere-button";
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
    cy.intercept("/auth/admin/realms/master/groups/*/members").as("groupMove");
    cy.getId(this.moveButton).click();
    cy.wait(["@groupMove"]);
    return this;
  }
}
