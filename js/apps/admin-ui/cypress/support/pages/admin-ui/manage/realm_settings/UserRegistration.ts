export default class UserRegistration {
  #userRegistrationTab = "rs-userRegistration-tab";
  #defaultGroupTab = "#pf-tab-20-groups";
  #addRoleBtn = "assignRole";
  #addDefaultGroupBtn = "no-default-groups-empty-action";
  #namesColumn = "tbody td:visible";
  #addBtn = "assign";
  #filterTypeDropdown = "filter-type-dropdown";

  goToTab() {
    cy.findByTestId(this.#userRegistrationTab).click({ force: true });
    return this;
  }

  goToDefaultGroupTab() {
    cy.get(this.#defaultGroupTab).click();
    return this;
  }

  addRole() {
    cy.findByTestId(this.#addRoleBtn).click({ force: true });
    return this;
  }

  addDefaultGroup() {
    cy.findByTestId(this.#addDefaultGroupBtn).click();
    return this;
  }

  changeRoleTypeFilter(filter: string) {
    // Invert the filter because the testid is the current selection
    const option = filter == "roles" ? "clients" : "roles";

    cy.findByTestId(this.#filterTypeDropdown).click();
    cy.findByTestId(option).click();

    return this;
  }

  selectRow(name: string) {
    cy.get(this.#namesColumn)
      .contains(name)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    return this;
  }

  assign() {
    cy.findByTestId(this.#addBtn).click();
    return this;
  }
}

export class GroupPickerDialog {
  #addButton = "add-button";
  #title = ".pf-v5-c-modal-box__title";

  clickRow(groupName: string) {
    cy.findByTestId(groupName).within(() => cy.get("input").click());
    return this;
  }

  clickRoot() {
    cy.get(".pf-v5-c-breadcrumb__item > button").click();
    return this;
  }

  checkTitle(title: string) {
    cy.get(this.#title).should("have.text", title);
    return this;
  }

  clickAdd() {
    cy.findByTestId(this.#addButton).click();
    return this;
  }
}
