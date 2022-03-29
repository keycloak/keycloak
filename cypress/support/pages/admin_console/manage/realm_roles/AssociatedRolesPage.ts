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
  private removeRolesButton = "removeRoles";

  addAssociatedRealmRole(roleName: string) {
    cy.findByTestId(this.actionDropdown).last().click();

    cy.findByTestId(this.addRolesDropdownItem).click();

    cy.get('[data-testid="addAssociatedRole"] td[data-label="Role name"]')
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    cy.findByTestId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.url().should("include", "/associated-roles");

    cy.findByTestId(this.compositeRoleBadge).should(
      "contain.text",
      "Composite"
    );

    return this;
  }

  addAssociatedRoleFromSearchBar(roleName: string, isClientRole?: boolean) {
    cy.findByTestId(this.addRoleToolbarButton).click();

    if (isClientRole) {
      cy.findByTestId(this.filterTypeDropdown).click();
      cy.findByTestId(this.filterTypeDropdownItem).click();
    }

    cy.findByTestId(".pf-c-spinner__tail-ball").should("not.exist");

    cy.get('[data-testid="addAssociatedRole"] td[data-label="Role name"]')
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.contains("Users in role").click();
    cy.findByTestId(this.usersPage).should("exist");
  }

  addAssociatedClientRole(roleName: string) {
    cy.findByTestId(this.addRoleToolbarButton).click();

    cy.findByTestId(this.filterTypeDropdown).click();

    cy.findByTestId(this.filterTypeDropdownItem).click();

    cy.findByTestId(".pf-c-spinner__tail-ball").should("not.exist");

    cy.get('[data-testid="addAssociatedRole"] td[data-label="Role name"]')
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.addAssociatedRolesModalButton).contains("Add").click();

    cy.contains("Users in role").click();
    cy.findByTestId(this.usersPage).should("exist");
  }

  removeAssociatedRoles() {
    cy.findByTestId(this.removeRolesButton).click();
    return this;
  }

  isRemoveAssociatedRolesBtnDisabled() {
    cy.findByTestId(this.removeRolesButton).should(
      "have.class",
      "pf-m-disabled"
    );
    return this;
  }
}
