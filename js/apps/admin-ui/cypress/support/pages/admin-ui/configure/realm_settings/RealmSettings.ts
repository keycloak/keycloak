export default class RealmSettings {
  #actionDropdown = "action-dropdown";

  clickActionMenu() {
    cy.findByTestId(this.#actionDropdown).click();
    return this;
  }
}
