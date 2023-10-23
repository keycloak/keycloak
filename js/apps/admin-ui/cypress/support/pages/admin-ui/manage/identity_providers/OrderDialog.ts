const expect = chai.expect;

export default class OrderDialog {
  #manageDisplayOrder = "manageDisplayOrder";
  #list = "manageOrderDataList";

  openDialog() {
    cy.findByTestId(this.#manageDisplayOrder).click({ force: true });
    return this;
  }

  moveRowTo(from: string, to: string) {
    cy.findByTestId(to).as("target");
    cy.findByTestId(from).drag("@target");
    return this;
  }

  clickSave() {
    cy.get("#modal-confirm").click();
    return this;
  }

  checkOrder(providerNames: string[]) {
    cy.get(`[data-testid=${this.#list}] li`).should((providers) => {
      expect(providers).to.have.length(providerNames.length);
      for (let index = 0; index < providerNames.length; index++) {
        expect(providers.eq(index)).to.contain(providerNames[index]);
      }
    });
  }
}
