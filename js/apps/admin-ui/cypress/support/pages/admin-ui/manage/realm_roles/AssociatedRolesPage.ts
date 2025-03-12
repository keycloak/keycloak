import SidebarPage from "../../SidebarPage";

export default class AssociatedRolesPage {
  #addRoleToolbarButton = "add-role-mapping-button";
  #addAssociatedRolesModalButton = "assign";
  #compositeRoleBadge = "composite-role-badge";
  #clientsRole = "client-role";
  #realmRole = "roles-role";
  #usersPage = "users-page";
  #removeRolesButton = "unAssignRole";
  #addRoleTable = '[aria-label="Roles"] td';
  #associatedRolesTab = "associatedRolesTab";

  addAssociatedRealmRole(roleName: string) {
    cy.findByTestId(this.#associatedRolesTab).should("exist").click();
    new SidebarPage().waitForPageLoad();

    cy.findByTestId(this.#addRoleToolbarButton).click();
    cy.findByTestId(this.#realmRole).click();

    cy.get(this.#addRoleTable)
      .contains(roleName)
      .parents("tr")
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

    if (isClientRole) {
      cy.findByTestId(this.#clientsRole).click();
    } else {
      cy.findByTestId(this.#realmRole).click();
    }

    cy.findByTestId(".pf-v5-c-spinner__tail-ball").should("not.exist");

    cy.get(this.#addRoleTable)
      .contains(roleName)
      .parents("tr")
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
      .parents("tr")
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
