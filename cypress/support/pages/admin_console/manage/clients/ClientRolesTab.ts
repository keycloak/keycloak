export default class ClientRolesTab {
  private createRoleBtn = "create-role";
  private createRoleEmptyStateBtn = "no-roles-for-this-client-empty-action";
  private actionsDropdown = `[aria-label="Actions"]`;
  private hideInheritedRolesChkBox = "#kc-hide-inherited-roles-checkbox";

  private rolesTab = "rolesTab";
  private associatedRolesTab = ".kc-associated-roles-tab > button";
  private attributesTab = ".kc-attributes-tab > button";
  private attributeKeyInput = "attribute-key-input";
  private attributeValueInput = "attribute-value-input";
  private addAttributeButton = "attribute-add-row";
  private removeFirstAttributeButton = "#minus-button-0";
  private saveAttributesButton = "save-attributes";

  goToRolesTab() {
    cy.findByTestId(this.rolesTab).click();
    return this;
  }

  goToAssociatedRolesTab() {
    cy.get(this.associatedRolesTab).click();
    return this;
  }

  goToAttributesTab() {
    cy.get(this.attributesTab).click();
    return this;
  }

  goToCreateRoleFromToolbar() {
    cy.findByTestId(this.createRoleBtn).click();
    return this;
  }

  goToCreateRoleFromEmptyState() {
    cy.findByTestId(this.createRoleEmptyStateBtn).click();
    return this;
  }

  fillClientRoleData() {
    cy.findByTestId(this.createRoleBtn).click();
    return this;
  }

  saveAttribute() {
    cy.findByTestId(this.saveAttributesButton).click();
    return this;
  }

  addAttribute() {
    cy.findByTestId(this.attributeKeyInput).type("crud_attribute_key");

    cy.findByTestId(this.attributeValueInput).type("crud_attribute_value");

    cy.findByTestId(this.addAttributeButton).click();

    this.saveAttribute();

    cy.get("table")
      .should("have.class", "kc-attributes__table")
      .get("tbody")
      .children()
      .should("have.length", 2);

    return this;
  }

  deleteAttribute() {
    cy.get(this.removeFirstAttributeButton).click();
    this.saveAttribute();
    cy.findByTestId(this.attributeKeyInput).should("have.value", "");
    cy.findByTestId(this.attributeValueInput).should("have.value", "");
  }

  hideInheritedRoles() {
    cy.get(this.hideInheritedRolesChkBox).check();
    return this;
  }
}
