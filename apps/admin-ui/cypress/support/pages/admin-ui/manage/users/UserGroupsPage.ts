import ModalUtils from "apps/admin-ui/cypress/support/util/ModalUtils";

const modalUtils = new ModalUtils();

export default class UserGroupsPage {
  userGroupsTab: string;
  addGroupButton: string;
  joinGroupButton: string;
  leaveGroupButton: string;

  constructor() {
    this.userGroupsTab = "user-groups-tab";
    this.addGroupButton = "add-group-button";
    this.joinGroupButton = "users:join-button";
    this.leaveGroupButton = "leave-group-button";
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

  leaveGroup() {
    cy.findByTestId(this.leaveGroupButton).click();
    modalUtils.confirmModal();
    return this;
  }

  leaveGroupButtonDisabled() {
    cy.findByTestId(this.leaveGroupButton).should("be.disabled");
    return this;
  }

  leaveGroupButtonEnabled() {
    cy.findByTestId(this.leaveGroupButton).should("not.be.disabled");
    return this;
  }
}
