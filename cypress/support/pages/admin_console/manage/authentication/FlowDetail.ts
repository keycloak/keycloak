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
}
