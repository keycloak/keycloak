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
  private attributeKeyInput = "attribute-key-input";
  private attributeValueInput = "attribute-value-input";
  private removeFirstAttributeButton = "#minus-button-0";

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
    this.tableUtils().clickRowItemByItemName("Add an attribute", 1, "button");
    return this;
  }

  clickDeleteAttributeButton(row: number) {
    this.tableUtils().clickRowItemByIndex(row, 3, "button");
    return this;
  }

  addAttribute(rowIndex: number, key: string, value: string) {
    this.tableUtils()
      .typeValueToRowItem(rowIndex, 1, key)
      .typeValueToRowItem(rowIndex, 2, value);
    this.clickAddAnAttributeButton();
    this.formUtils().save();
    return this;
  }

  deleteAttribute(rowIndex: number) {
    this.clickDeleteAttributeButton(rowIndex);
    this.formUtils().save();
    this.tableUtils()
      .checkRowItemValueByIndex(rowIndex, 1, "", "input")
      .checkRowItemValueByIndex(rowIndex, 2, "", "input");
    return this;
  }

  hideInheritedRoles() {
    cy.get(this.hideInheritedRolesChkBox).check();
    return this;
  }
}
