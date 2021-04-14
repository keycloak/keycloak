export default class UserGroupsPage {
  userGroupsTab: string;
  addGroupButton: string;
  joinGroupButton: string;

  constructor() {
    this.userGroupsTab = "user-groups-tab";
    this.addGroupButton = "add-group-button";
    this.joinGroupButton = "joinGroup";
  }

  goToGroupsTab() {
    cy.getId(this.userGroupsTab).click();

    return this;
  }

  toggleAddGroupModal() {
    cy.getId(this.addGroupButton).click();

    return this;
  }

  joinGroup() {
    cy.getId(this.joinGroupButton).click();

    return this;
  }
}
