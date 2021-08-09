export default class AssociatedRolesPage {
  private actionDropdown = "action-dropdown";
  private addRolesDropdownItem = "add-roles";
  private addRoleToolbarButton = "add-role-button";
  private checkbox = "[type=checkbox]";
  private addAssociatedRolesModalButton = "add-associated-roles-button";
  private compositeRoleBadge = "composite-role-badge";
  private filterTypeDropdown = "filter-type-dropdown";
  private filterTypeDropdownItem = "filter-type-dropdown-item";
  private usersPage = "users-page";

  addAssociatedRealmRole() {
    cy.getId(this.actionDropdown).last().click();

    const load = "/auth/admin/realms/master/clients";
    cy.intercept(load).as("load");

    cy.getId(this.addRolesDropdownItem).click();

    cy.wait(["@load"]);
    cy.get(this.checkbox).eq(2).check();

    cy.getId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.url().should("include", "/AssociatedRoles");

    cy.getId(this.compositeRoleBadge).should("contain.text", "Composite");

    cy.wait(["@load"]);

    return this;
  }

  addAssociatedClientRole() {
    cy.getId(this.addRoleToolbarButton).click();

    cy.getId(this.filterTypeDropdown).click();

    cy.getId(this.filterTypeDropdownItem).click();

    cy.getId(".pf-c-spinner__tail-ball").should("not.exist");

    cy.get('[data-testid="addAssociatedRole"] td[data-label="Role name"]')
      .contains("manage-account")
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.getId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.contains("Users in role").click().getId(this.usersPage).should("exist");
  }
}
