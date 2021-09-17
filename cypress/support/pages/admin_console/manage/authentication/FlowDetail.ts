type RequirementType = "Required" | "Alternative" | "Disabled" | "Conditional";

export default class FlowDetails {
  executionExists(name: string) {
    this.getExecution(name).should("exist");
    return this;
  }

  private getExecution(name: string) {
    return cy.findByTestId(name);
  }

  moveRowTo(from: string, to: string) {
    cy.findByTestId(from).trigger("dragstart").trigger("dragleave");

    cy.findByTestId(to)
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
    cy.findByTestId(`${subFlowName}-edit-dropdown`)
      .click()
      .contains(option)
      .click();
  }

  addExecution(subFlowName: string, executionTestId: string) {
    this.clickEditDropdownForFlow(subFlowName, "Add step");

    cy.get(".pf-c-pagination").should("exist");
    cy.findByTestId(executionTestId).click();
    cy.findByTestId("modal-add").click();

    return this;
  }

  addCondition(subFlowName: string, executionTestId: string) {
    this.clickEditDropdownForFlow(subFlowName, "Add condition");

    cy.get(".pf-c-pagination").should("not.exist");
    cy.findByTestId(executionTestId).click();
    cy.findByTestId("modal-add").click();

    return this;
  }
}
