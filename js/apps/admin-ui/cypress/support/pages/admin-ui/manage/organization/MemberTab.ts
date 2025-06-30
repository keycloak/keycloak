import CommonPage from "../../../CommonPage";

export default class MembersTab extends CommonPage {
  goToTab() {
    cy.findByTestId("membersTab").click();
  }

  clickAddRealmUser() {
    cy.findByTestId("add-realm-user-empty-action").click();
  }

  assertMemberAddedSuccess() {
    this.masthead().checkNotificationMessage(
      "1 user added to the organization",
    );
  }
}
