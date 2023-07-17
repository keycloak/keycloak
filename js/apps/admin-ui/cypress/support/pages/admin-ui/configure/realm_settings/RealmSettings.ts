export default class RealmSettings {
  private actionDropdown = "action-dropdown";

  clickActionMenu() {
    cy.findByTestId(this.actionDropdown).click();
    return this;
  }
}
