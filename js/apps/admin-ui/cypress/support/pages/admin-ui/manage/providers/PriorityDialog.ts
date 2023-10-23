const expect = chai.expect;

export default class PriorityDialog {
  #managePriorityOrder = "viewHeader-lower-btn";
  #list = "manageOrderDataList";

  openDialog() {
    cy.findByTestId(this.#managePriorityOrder).click({ force: true });
    return this;
  }

  moveRowTo(from: string, to: string) {
    cy.findByTestId(to).as("target");
    cy.findByTestId(from).drag("@target");
    return this;
  }

  clickSave() {
    cy.get("#modal-confirm").click({ force: true });
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
