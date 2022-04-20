import CommonPage from "../../../CommonPage";

enum ClientRolesTabItems {
  Details = "Details",
  Attributes = "Attributes",
  UsersInRole = "Users in role",
}

export default class ClientRolesTab extends CommonPage {
  private createRoleBtn = "create-role";
  private createRoleEmptyStateBtn = "no-roles-for-this-client-empty-action";
  private hideInheritedRolesChkBox = "#kc-hide-inherited-roles-checkbox";
  private rolesTab = "rolesTab";
  private associatedRolesTab = ".kc-associated-roles-tab > button";

  goToDetailsTab() {
    this.tabUtils().clickTab(ClientRolesTabItems.Details);
    return this;
  }

  goToAttributesTab() {
    this.tabUtils().clickTab(ClientRolesTabItems.Attributes);
    return this;
  }

  goToUsersInRoleTab() {
    this.tabUtils().clickTab(ClientRolesTabItems.UsersInRole);
    return this;
  }

  goToRolesTab() {
    cy.findByTestId(this.rolesTab).click();
    return this;
  }

  goToAssociatedRolesTab() {
    cy.get(this.associatedRolesTab).click();
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

  clickAddAnAttributeButton() {
    cy.findByTestId("attributes-add-row").click();
    return this;
  }

  clickDeleteAttributeButton(row: number) {
    cy.findByTestId(`attributes[${row - 1}].remove`).click();
    return this;
  }

  addAttribute(rowIndex: number, key: string, value: string) {
    cy.findAllByTestId(`attributes[${rowIndex - 1}].key`).type(key);
    cy.findAllByTestId(`attributes[${rowIndex - 1}].value`).type(value);
    this.clickAddAnAttributeButton();
    this.formUtils().save();
    return this;
  }

  deleteAttribute(rowIndex: number) {
    this.clickDeleteAttributeButton(rowIndex);
    this.formUtils().save();

    cy.findAllByTestId(`attributes[${rowIndex - 1}].key`).should(
      "have.value",
      ""
    );
    cy.findAllByTestId(`attributes[${rowIndex - 1}].value`).should(
      "have.value",
      ""
    );
    return this;
  }

  checkRowItemsEqualTo(amount: number) {
    cy.findAllByTestId("row").its("length").should("be.eq", amount);
    return this;
  }

  hideInheritedRoles() {
    cy.get(this.hideInheritedRolesChkBox).check();
    return this;
  }
}
