type RequirementType = "Required" | "Alternative" | "Disabled" | "Conditional";

export default class FlowDetails {
  executionExists(name: string) {
    this.getExecution(name).should("exist");
    return this;
  }

  flowExists(name: string) {
    cy.findAllByText(name).should("exist");
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

  addSubFlow(subFlowName: string, name: string) {
    this.clickEditDropdownForFlow(subFlowName, "Add sub-flow");
    this.fillSubFlowModal(subFlowName, name);

    return this;
  }

  private fillSubFlowModal(subFlowName: string, name: string) {
    cy.get(".pf-c-modal-box__title-text").contains(
      "Add step to " + subFlowName
    );
    cy.findByTestId("name").type(name);
    cy.findByTestId("modal-add").click();
  }

  fillCreateForm(
    name: string,
    description: string,
    type: "Basic flow" | "Client flow"
  ) {
    cy.findByTestId("alias").type(name);
    cy.findByTestId("description").type(description);
    cy.get("#flowType").click().parent().contains(type).click();
    cy.findByTestId("create").click();
    return this;
  }

  addSubFlowToEmpty(subFlowName: string, name: string) {
    cy.findByTestId("addSubFlow").click();
    this.fillSubFlowModal(subFlowName, name);

    return this;
  }
}
