import CommonPage from "../../../../../../CommonPage";
import CreateResourcePage from "../../CreateResourcePage";

export default class ResourcesTab extends CommonPage {
  #createResourcePage = new CreateResourcePage();
  #createResourceBtn = "createResource";

  createResource() {
    cy.findByTestId(this.#createResourceBtn).click();

    return this.#createResourcePage;
  }
}
