export default class ListingPage {
  private actionMenu = "action-dropdown";

  clickAction(action: string) {
    cy.getId(this.actionMenu).click().getId(action).click();
    return this;
  }
}
