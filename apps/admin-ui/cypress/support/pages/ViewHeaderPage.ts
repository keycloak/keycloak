export default class ListingPage {
  private actionMenu = "action-dropdown";

  clickAction(action: string) {
    cy.findByTestId(this.actionMenu).click().findByTestId(action).click();
    return this;
  }
}
