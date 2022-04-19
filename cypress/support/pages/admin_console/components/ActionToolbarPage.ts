import CommonElements from "../../CommonElements";

export default class ActionToolbarPage extends CommonElements {
  constructor() {
    super(".pf-l-level.pf-m-gutter");
  }

  get bearerOnlyExplainerLabelElement() {
    return cy
      .get(this.parentSelector)
      .findByTestId("bearer-only-explainer-label");
  }

  get bearerOnlyExplainerTooltipElement() {
    return cy.findByTestId("bearer-only-explainer-tooltip");
  }

  clickActionToggleButton() {
    cy.get(this.parentSelector).findByTestId("action-dropdown").click();
    return this;
  }

  checkActionItemExists(itemName: string, exists: boolean) {
    const condition = exists ? "exist" : "not.exist";
    cy.get(this.dropdownMenuItem).contains(itemName).should(condition);
    return this;
  }
}
