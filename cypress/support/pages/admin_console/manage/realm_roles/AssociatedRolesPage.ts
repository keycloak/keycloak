export default class AssociatedRolesPage {
  actionDropdown: string;
  addRolesDropdownItem: string;
  addRoleToolbarButton: string;
  checkbox: string;
  addAssociatedRolesModalButton: string;
  compositeRoleBadge: string;
  filterTypeDropdown: string;
  filterTypeDropdownItem: string;
  usersPage: string;

  constructor() {
    this.actionDropdown = "[data-testid=action-dropdown]";
    this.addRolesDropdownItem = "[data-testid=add-roles]";
    this.addRoleToolbarButton = "[data-testid=add-role-button]";
    this.checkbox = "[type=checkbox]";
    this.addAssociatedRolesModalButton =
      "[data-testid=add-associated-roles-button]";
    this.compositeRoleBadge = "[data-testid=composite-role-badge]";
    this.filterTypeDropdown = "[data-testid=filter-type-dropdown]";
    this.filterTypeDropdownItem = "[data-testid=filter-type-dropdown-item]";
    this.usersPage = "[data-testid=users-page]";
  }

  addAssociatedRealmRole() {
    cy.get(this.actionDropdown).last().click();

    const load = "/auth/admin/realms/master/clients";
    cy.intercept(load).as("load");

    cy.get(this.addRolesDropdownItem).click();

    cy.wait(["@load"]);
    cy.get(this.checkbox).eq(2).check();

    cy.get(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.url().should("include", "/AssociatedRoles");

    cy.get(this.compositeRoleBadge).should("contain.text", "Composite");

    cy.wait(["@load"]);

    return this;
  }

  addAssociatedClientRole() {
    cy.get(this.addRoleToolbarButton).click();

    cy.get(this.filterTypeDropdown).click();

    cy.get(this.filterTypeDropdownItem).click();

    cy.get(".pf-c-spinner__tail-ball").should("not.exist");

    cy.get(this.checkbox).eq(12).check({ force: true });

    cy.get(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.get(".pf-c-spinner__tail-ball").should("not.exist");

    cy.contains("Users in role").click().get(this.usersPage).should("exist");
  }
}
