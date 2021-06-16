export default class CreateUserPage {
  usernameInput: string;
  usersEmptyState: string;
  emptyStateCreateUserBtn: string;
  searchPgCreateUserBtn: string;
  addUserBtn: string;
  joinGroupsBtn: string;
  joinBtn: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    this.usernameInput = "#kc-username";

    this.usersEmptyState = "empty-state";
    this.emptyStateCreateUserBtn = "no-users-found-empty-action";
    this.searchPgCreateUserBtn = "create-new-user";
    this.addUserBtn = "add-user";
    this.joinGroupsBtn = "join-groups-button";
    this.joinBtn = "users:join-button";
    this.saveBtn = "create-user";
    this.cancelBtn = "cancel-create-user";
  }

  //#region General Settings
  createUser(username: string) {
    cy.get(this.usernameInput).clear();

    if (username) {
      cy.get(this.usernameInput).type(username);
    }

    return this;
  }

  goToCreateUser() {
    cy.wait(100);
    cy.get("body").then((body) => {
      if (body.find("[data-testid=empty-state]").length > 0) {
        cy.getId(this.emptyStateCreateUserBtn).click();
      } else if (body.find("[data-testid=search-users-title]").length > 0) {
        cy.getId(this.searchPgCreateUserBtn).click();
      } else {
        cy.getId(this.addUserBtn).click();
      }
    });

    return this;
  }

  toggleAddGroupModal() {
    cy.getId(this.joinGroupsBtn).click();

    return this;
  }

  joinGroups() {
    cy.getId(this.joinBtn).click();

    return this;
  }

  save() {
    cy.getId(this.saveBtn).click();

    return this;
  }

  cancel() {
    cy.getId(this.cancelBtn).click();

    return this;
  }
}
