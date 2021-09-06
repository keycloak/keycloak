type RequirementType = "Required" | "Alternative" | "Disabled" | "Conditional";

export default class FlowDetails {
  executionExists(name: string) {
    this.getExecution(name).should("exist");
    return this;
  }

  private getExecution(name: string) {
    return cy.getId(name);
  }

  moveRowTo(from: string, to: string) {
    cy.getId(from).trigger("dragstart").trigger("dragleave");

    cy.getId(to)
      .trigger("dragenter")
      .trigger("dragover")
      .trigger("drop")
      .trigger("dragend");

    return this;
  }

  changeRequirement(execution: string, requirement: RequirementType) {
    this.getExecution(execution)
      .parentsUntil(".keycloak__authentication__flow-row")
      .find(".keycloak__authentication__requirement-dropdown")
      .click()
      .contains(requirement)
      .click();
    return this;
  }

  goToDiagram() {
    cy.get("#diagramView").click();
    return this;
  }

  private clickEditDropdownForFlow(subFlowName: string, option: string) {
    cy.getId(`${subFlowName}-edit-dropdown`).click().contains(option).click();
  }

  addExecution(subFlowName: string, executionTestId: string) {
    this.clickEditDropdownForFlow(subFlowName, "Add step");

    cy.get(".pf-c-pagination").should("exist");
    cy.getId(executionTestId).click();
    cy.getId("modal-add").click();

    return this;
  }

  addCondition(subFlowName: string, executionTestId: string) {
    this.clickEditDropdownForFlow(subFlowName, "Add condition");

    cy.get(".pf-c-pagination").should("not.exist");
    cy.getId(executionTestId).click();
    cy.getId("modal-add").click();

    return this;
  }
}
