export default class AssociatedRolesPage {
  private actionDropdown = "action-dropdown";
  private addRolesDropdownItem = "add-roles";
  private addRoleToolbarButton = "add-role-button";
  private addAssociatedRolesModalButton = "assign";
  private compositeRoleBadge = "composite-role-badge";
  private filterTypeDropdown = "filter-type-dropdown";
  private filterTypeDropdownItem = "roles";
  private usersPage = "users-page";
  private removeRolesButton = "removeRoles";
  private addRoleTable = 'td[data-label="Name"]';

  addAssociatedRealmRole(roleName: string) {
    cy.findByTestId(this.actionDropdown).last().click();

    cy.findByTestId(this.addRolesDropdownItem).click();

    cy.get(this.addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    cy.findByTestId(this.addAssociatedRolesModalButton).click();

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

    cy.get(this.addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.addAssociatedRolesModalButton).click();

    cy.contains("Users in role").click();
    cy.findByTestId(this.usersPage).should("exist");
  }

  addAssociatedClientRole(roleName: string) {
    cy.findByTestId(this.addRoleToolbarButton).click();

    cy.findByTestId(this.filterTypeDropdown).click();

    cy.findByTestId(this.filterTypeDropdownItem).click();

    cy.findByTestId(".pf-c-spinner__tail-ball").should("not.exist");

    cy.get(this.addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.addAssociatedRolesModalButton).click();

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
