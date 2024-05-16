import PageObject from "../../../../components/PageObject";

export class AdvancedSamlTab extends PageObject {
  #termsOfServiceUrlId = "attributes.tosUri";

  saveFineGrain() {
    cy.findAllByTestId("fineGrainSave").click();
  }

  revertFineGrain() {
    cy.findByTestId("fineGrainRevert").click();
  }

  termsOfServiceUrl(termsOfServiceUrl: string) {
    cy.findAllByTestId(this.#termsOfServiceUrlId).clear();
    cy.findAllByTestId(this.#termsOfServiceUrlId).type(termsOfServiceUrl);
    return this;
  }

  checkTermsOfServiceUrl(termsOfServiceUrl: string) {
    cy.findAllByTestId(this.#termsOfServiceUrlId).should(
      "have.value",
      termsOfServiceUrl,
    );
    return this;
  }
}
