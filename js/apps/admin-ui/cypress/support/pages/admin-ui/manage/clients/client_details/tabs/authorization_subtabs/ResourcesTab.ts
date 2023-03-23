import CommonPage from "../../../../../../CommonPage";
import CreateResourcePage from "../../CreateResourcePage";

export default class ResourcesTab extends CommonPage {
  private createResourcePage = new CreateResourcePage();
  private createResourceBtn = "createResource";

  createResource() {
    cy.findByTestId(this.createResourceBtn).click();

    return this.createResourcePage;
  }
}
