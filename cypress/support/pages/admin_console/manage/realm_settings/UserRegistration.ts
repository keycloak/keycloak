export default class UserRegistration {
  private userRegistrationTab = "rs-userRegistration-tab";
  private defaultGroupTab = "#pf-tab-20-groups";
  private addRoleButton = "add-role-button";
  private addDefaultGroup = "no-default-groups-empty-action";
  private namesColumn = 'td[data-label="Role name"]:visible';
  private addBtn = "add-associated-roles-button";

  goToTab() {
    cy.findByTestId(this.userRegistrationTab).click({ force: true });
    return this;
  }

  goToDefaultGroupTab() {
    cy.get(this.defaultGroupTab).click();
    return this;
  }

  addRoleButtonClick() {
    cy.findByTestId(this.addRoleButton).click({ force: true });
    return this;
  }

  addDefaultGroupClick() {
    cy.findByTestId(this.addDefaultGroup).click();
    return this;
  }

  selectRow(name: string) {
    cy.get(this.namesColumn)
      .contains(name)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    return this;
  }

  assign() {
    cy.findByTestId(this.addBtn).click();
    return this;
  }
}

export class GroupPickerDialog {
  private addButton = "common:add-button";
  private title = ".pf-c-modal-box__title";

  clickRow(groupName: string) {
    cy.findByTestId(groupName).within(() => cy.get("input").click());
    return this;
  }

  clickRoot() {
    cy.get(".pf-c-breadcrumb__item > button").click();
    return this;
  }

  checkTitle(title: string) {
    cy.get(this.title).should("have.text", title);
    return this;
  }

  clickAdd() {
    cy.findByTestId(this.addButton).click();
    return this;
  }
}
