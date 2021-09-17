import moment from "moment";

export default class AdvancedTab {
  private setToNow = "#setToNow";
  private clear = "#clear";
  private push = "#push";
  private notBefore = "#kc-not-before";

  private clusterNodesExpand =
    ".pf-c-expandable-section .pf-c-expandable-section__toggle";
  private testClusterAvailability = "#testClusterAvailability";
  private registerNodeManually = "no-nodes-registered-empty-action";
  private nodeHost = "#nodeHost";
  private addNodeConfirm = "#add-node-confirm";

  private accessTokenSignatureAlgorithm = "#accessTokenSignatureAlgorithm";
  private fineGrainSave = "#fineGrainSave";
  private fineGrainRevert = "#fineGrainRevert";

  private advancedTab = "#pf-tab-advanced-advanced";

  goToTab() {
    cy.get(this.advancedTab).click();
    return this;
  }

  clickSetToNow() {
    cy.get(this.setToNow).click();
    return this;
  }

  clickClear() {
    cy.get(this.clear).click();
    return this;
  }

  clickPush() {
    cy.get(this.push).click();
    return this;
  }

  checkNone() {
    cy.get(this.notBefore).should("have.value", "None");

    return this;
  }

  checkSetToNow() {
    cy.get(this.notBefore).should("have.value", moment().format("LLL"));

    return this;
  }

  expandClusterNode() {
    cy.get(this.clusterNodesExpand).click();
    return this;
  }

  checkTestClusterAvailability(active: boolean) {
    cy.get(this.testClusterAvailability).should(
      (active ? "not." : "") + "have.class",
      "pf-m-disabled"
    );
    return this;
  }

  clickRegisterNodeManually() {
    cy.findByTestId(this.registerNodeManually).click();
    return this;
  }

  fillHost(host: string) {
    cy.get(this.nodeHost).type(host);
    return this;
  }

  clickSaveHost() {
    cy.get(this.addNodeConfirm).click();
    return this;
  }

  selectAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.accessTokenSignatureAlgorithm).click();
    cy.get(this.accessTokenSignatureAlgorithm + " + ul")
      .contains(algorithm)
      .click();

    return this;
  }

  checkAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.accessTokenSignatureAlgorithm).should("have.text", algorithm);
    return this;
  }

  clickSaveFineGrain() {
    cy.get(this.fineGrainSave).click();
    return this;
  }

  clickRevertFineGrain() {
    cy.get(this.fineGrainRevert).click();
    return this;
  }
}
