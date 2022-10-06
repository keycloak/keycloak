import CommonPage from "../../../CommonPage";

enum ClientRolesTabItems {
  Details = "Details",
  Attributes = "Attributes",
  UsersInRole = "Users in role",
}

export default class ClientRolesTab extends CommonPage {
  private createRoleBtn = "create-role";
  private createRoleEmptyStateBtn = "no-roles-for-this-client-empty-action";
  private hideInheritedRolesChkBox = "#hideInheritedRoles";
  private rolesTab = "rolesTab";
  private associatedRolesTab = ".kc-associated-roles-tab > button";

  goToDetailsTab() {
    this.tabUtils().clickTab(ClientRolesTabItems.Details);
    return this;
  }

  goToAttributesTab() {
    cy.intercept("/admin/realms/master/roles-by-id/*").as("load");
    this.tabUtils().clickTab(ClientRolesTabItems.Attributes);
    cy.wait("@load");
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
    cy.intercept("/admin/realms/master/roles-by-id/*").as("load");
    cy.get(this.associatedRolesTab).click();
    cy.wait("@load");
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

  hideInheritedRoles() {
    cy.get(this.hideInheritedRolesChkBox).check();
    return this;
  }
}
