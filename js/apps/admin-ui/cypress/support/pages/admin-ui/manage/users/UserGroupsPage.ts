import ModalUtils from "../../../../../support/util/ModalUtils";

const modalUtils = new ModalUtils();

export default class UserGroupsPage {
  #userGroupsTab = "user-groups-tab";
  #noGroupsAddGroupButton = "no-groups-empty-action";
  #joinGroupButton = "join-button";
  #leaveGroupButton = "leave-group-button";

  goToGroupsTab() {
    cy.findByTestId(this.#userGroupsTab).click();
    return this;
  }

  toggleAddGroupModal() {
    cy.findByTestId(this.#noGroupsAddGroupButton).click({ force: true });
    return this;
  }

  joinGroups() {
    cy.findByTestId(this.#joinGroupButton).click();
  }

  leaveGroup() {
    cy.findByTestId(this.#leaveGroupButton).click();
    modalUtils.confirmModal();
    return this;
  }

  leaveGroupButtonDisabled() {
    cy.findByTestId(this.#leaveGroupButton).should("be.disabled");
    return this;
  }

  leaveGroupButtonEnabled() {
    cy.findByTestId(this.#leaveGroupButton).should("not.be.disabled");
    return this;
  }
}
