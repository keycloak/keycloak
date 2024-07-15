export default class AssociatedRolesPage {
  #addRoleToolbarButton = "assignRole";
  #addAssociatedRolesModalButton = "assign";
  #compositeRoleBadge = "composite-role-badge";
  #filterTypeDropdown = "filter-type-dropdown";
  #filterTypeDropdownItem = "clients";
  #usersPage = "users-page";
  #removeRolesButton = "unAssignRole";
  #addRoleTable = '[aria-label="Roles"] td';
  #associatedRolesTab = "associatedRolesTab";
  #assignRole = "no-roles-in-this-realm-empty-action";

  addAssociatedRealmRole(roleName: string) {
    cy.findByTestId(this.#associatedRolesTab).should("exist").click();

    cy.findByTestId(this.#assignRole).click();

    cy.findByTestId(this.#filterTypeDropdown).click();

    cy.findByTestId(this.#filterTypeDropdownItem).click();

    cy.get(this.#addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });
    cy.findByTestId(this.#addAssociatedRolesModalButton).click();

    cy.url().should("include", "/associated-roles");

    cy.findByTestId(this.#compositeRoleBadge).should(
      "contain.text",
      "Composite",
    );

    return this;
  }

  addAssociatedRoleFromSearchBar(roleName: string, isClientRole?: boolean) {
    cy.findByTestId(this.#addRoleToolbarButton).click({ force: true });

    if (!isClientRole) {
      cy.findByTestId(this.#filterTypeDropdown).click();
      cy.findByTestId(this.#filterTypeDropdownItem).click();
    }

    cy.findByTestId(".pf-v5-c-spinner__tail-ball").should("not.exist");

    cy.get(this.#addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.#addAssociatedRolesModalButton).click();

    cy.contains("Users in role").click();
    cy.findByTestId(this.#usersPage).should("exist");
  }

  addAssociatedClientRole(roleName: string) {
    cy.findByTestId(this.#addRoleToolbarButton).click();

    cy.findByTestId(".pf-v5-c-spinner__tail-ball").should("not.exist");

    cy.get(this.#addRoleTable)
      .contains(roleName)
      .parent()
      .within(() => {
        cy.get("input").click();
      });

    cy.findByTestId(this.#addAssociatedRolesModalButton).click();

    cy.contains("Users in role").click();
    cy.findByTestId(this.#usersPage).should("exist");
  }

  removeAssociatedRoles() {
    cy.findByTestId(this.#removeRolesButton).click();
    return this;
  }

  isRemoveAssociatedRolesBtnDisabled() {
    cy.findByTestId(this.#removeRolesButton).should(
      "have.class",
      "pf-m-disabled",
    );
    return this;
  }
}
