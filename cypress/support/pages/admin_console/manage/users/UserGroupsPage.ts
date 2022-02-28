export default class UserGroupsPage {
  userGroupsTab: string;
  addGroupButton: string;
  joinGroupButton: string;

  constructor() {
    this.userGroupsTab = "user-groups-tab";
    this.addGroupButton = "add-group-button";
    this.joinGroupButton = "users:join-button";
  }

  goToGroupsTab() {
    cy.findByTestId(this.userGroupsTab).click();

    return this;
  }

  toggleAddGroupModal() {
    cy.findByTestId(this.addGroupButton).click({ force: true });

    return this;
  }

  joinGroups() {
    cy.findByTestId(this.joinGroupButton).click();

    return this;
  }
}
