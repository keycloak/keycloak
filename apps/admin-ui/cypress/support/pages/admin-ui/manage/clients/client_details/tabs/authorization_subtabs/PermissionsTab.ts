import CommonPage from "../../../../../../CommonPage";
import CreatePermissionPage from "../../CreatePermissionPage";
type PermissionType = "resource" | "scope";

export default class PermissionsTab extends CommonPage {
  private createPermissionPage = new CreatePermissionPage();
  private createPermissionDrpDwn = "permissionCreateDropdown";
  private permissionResourceDrpDwn = "#resources";

  createPermission(type: PermissionType) {
    cy.findByTestId(this.createPermissionDrpDwn).click();
    cy.findByTestId(`create-${type}`).click();
    return this.createPermissionPage;
  }

  selectResource(name: string) {
    cy.get(this.permissionResourceDrpDwn)
      .click()
      .parent()
      .parent()
      .findByText(name)
      .click();
    return this;
  }
}
