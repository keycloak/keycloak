export default class RealmSettings {
  private actionDropdown = "action-dropdown";

  clickActionMenu() {
    cy.getId(this.actionDropdown).click();
    return this;
  }
}
