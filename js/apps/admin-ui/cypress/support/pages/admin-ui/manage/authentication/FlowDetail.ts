type RequirementType = "Required" | "Alternative" | "Disabled" | "Conditional";

export default class FlowDetails {
  executionExists(name: string, exist = true) {
    this.getExecution(name).should((!exist ? "not." : "") + "exist");
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
    cy.findAllByTestId(from).drag(to);

    return this;
  }

  expectPriorityChange(execution: string, callback: () => void) {
    cy.findAllByTestId(execution).then((rowDetails) => {
      const executionId = rowDetails.children().attr("data-id");
      cy.intercept(
        "POST",
        `/admin/realms/test*/authentication/executions/${executionId}/lower-priority`
      ).as("priority");
      callback();
      cy.wait("@priority");
    });
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

  clickRowDelete(name: string) {
    cy.findByTestId(`${name}-delete`).click();

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
    cy.findByTestId("name").type(name);
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
