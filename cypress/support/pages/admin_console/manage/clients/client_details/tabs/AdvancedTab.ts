import CommonPage from "../../../../../CommonPage";

export default class AdvancedTab extends CommonPage {
  private setToNowBtn = "#setToNow";
  private clearBtn = "#clear";
  private pushBtn = "#push";
  private notBeforeInput = "#kc-not-before";

  private clusterNodesExpandBtn =
    ".pf-c-expandable-section .pf-c-expandable-section__toggle";
  private testClusterAvailability = "#testClusterAvailability";
  private emptyClusterElement = "empty-state";
  private registerNodeManuallyBtn = "no-nodes-registered-empty-action";
  private deleteClusterNodeDrpDwn =
    '[aria-label="registeredClusterNodes"] [aria-label="Actions"]';
  private deleteClusterNodeBtn =
    '[aria-label="registeredClusterNodes"] [role="menu"] button';
  private nodeHostInput = "#nodeHost";
  private addNodeConfirmBtn = "#add-node-confirm";

  private accessTokenSignatureAlgorithmInput = "#accessTokenSignatureAlgorithm";
  private fineGrainSaveBtn = "#fineGrainSave";
  private fineGrainRevertBtn = "#fineGrainRevert";

  setRevocationToNow() {
    cy.get(this.setToNowBtn).click();
    return this;
  }

  clearRevocation() {
    cy.get(this.clearBtn).click();
    return this;
  }

  pushRevocation() {
    cy.get(this.pushBtn).click();
    return this;
  }

  checkRevacationIsNone() {
    cy.get(this.notBeforeInput).should("have.value", "None");

    return this;
  }

  checkRevocationIsSetToNow() {
    cy.get(this.notBeforeInput).should(
      "have.value",
      new Date().toLocaleString("en-US", {
        dateStyle: "long",
        timeStyle: "short",
      })
    );

    return this;
  }

  expandClusterNode() {
    cy.get(this.clusterNodesExpandBtn).click();
    return this;
  }

  checkTestClusterAvailability(active: boolean) {
    cy.get(this.testClusterAvailability).should(
      (active ? "not." : "") + "have.class",
      "pf-m-disabled"
    );
    return this;
  }

  checkEmptyClusterNode() {
    cy.findByTestId(this.emptyClusterElement).should("exist");
    return this;
  }

  registerNodeManually() {
    cy.findByTestId(this.registerNodeManuallyBtn).click();
    return this;
  }

  deleteClusterNode() {
    cy.get(this.deleteClusterNodeDrpDwn).click();
    cy.get(this.deleteClusterNodeBtn).click();
    return this;
  }

  fillHost(host: string) {
    cy.get(this.nodeHostInput).type(host);
    return this;
  }

  saveHost() {
    cy.get(this.addNodeConfirmBtn).click();
    return this;
  }

  selectAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.accessTokenSignatureAlgorithmInput).click();
    cy.get(this.accessTokenSignatureAlgorithmInput + " + ul")
      .contains(algorithm)
      .click();

    return this;
  }

  checkAccessTokenSignatureAlgorithm(algorithm: string) {
    cy.get(this.accessTokenSignatureAlgorithmInput).should(
      "have.text",
      algorithm
    );
    return this;
  }

  saveFineGrain() {
    cy.get(this.fineGrainSaveBtn).click();
    return this;
  }

  revertFineGrain() {
    cy.get(this.fineGrainRevertBtn).click();
    return this;
  }
}
