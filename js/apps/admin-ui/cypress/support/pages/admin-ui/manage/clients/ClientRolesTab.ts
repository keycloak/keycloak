import CommonPage from "../../../CommonPage";

enum ClientRolesTabItems {
  Details = "Details",
  Attributes = "Attributes",
  UsersInRole = "Users in role",
  Permissions = "Permissions",
}

export default class ClientRolesTab extends CommonPage {
  #createRoleBtn = "create-role";
  #createRoleEmptyStateBtn = "no-roles-for-this-client-empty-action";
  #hideInheritedRolesChkBox = "#hideInheritedRoles";
  #rolesTab = "rolesTab";
  #associatedRolesTab = "associatedRolesTab";
  #defaultRolesTab = "default-roles-tab";
  #defaultGroupsTab = "default-groups-tab";

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

  goToPermissionsTab() {
    this.tabUtils().clickTab(ClientRolesTabItems.Permissions);
    return this;
  }

  goToRolesTab() {
    cy.findByTestId(this.#rolesTab).click();
    return this;
  }

  goToAssociatedRolesTab() {
    cy.findByTestId(this.#associatedRolesTab).click();
    return this;
  }

  goToCreateRoleFromToolbar() {
    cy.findByTestId(this.#createRoleBtn).click();
    return this;
  }

  goToCreateRoleFromEmptyState() {
    cy.findByTestId(this.#createRoleEmptyStateBtn).click();
    return this;
  }

  fillClientRoleData() {
    cy.findByTestId(this.#createRoleBtn).click();
    return this;
  }

  hideInheritedRoles() {
    cy.get(this.#hideInheritedRolesChkBox).check();
    return this;
  }

  goToDefaultRolesTab() {
    cy.findByTestId(this.#defaultRolesTab).click();
    return this;
  }

  goToDefaultGroupsTab() {
    cy.findByTestId(this.#defaultGroupsTab).click();
    return this;
  }
}
