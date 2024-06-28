import CommonPage from "../../../../../../CommonPage";
import CreatePermissionPage from "../../CreatePermissionPage";
type PermissionType = "resource" | "scope";

export default class PermissionsTab extends CommonPage {
  #createPermissionPage = new CreatePermissionPage();
  #createPermissionDrpDwn = "permissionCreateDropdown";
  #permissionResourceDrpDwn = "#resources";

  createPermission(type: PermissionType) {
    cy.findByTestId(this.#createPermissionDrpDwn).click();
    cy.findByTestId(`create-${type}`).click();
    return this.#createPermissionPage;
  }

  selectResource(name: string) {
    cy.get(this.#permissionResourceDrpDwn)
      .click()
      .parent()
      .parent()
      .findByText(name)
      .parent()
      .click();
    return this;
  }
}
