import CommonElements from "../../CommonElements";

export default class ActionToolbarPage extends CommonElements {
  constructor() {
    super(".pf-v5-l-level.pf-m-gutter");
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

  #getDropdownItem(itemName: string) {
    return cy.get(this.dropdownMenuItem).contains(itemName);
  }

  checkActionItemExists(itemName: string, exists: boolean) {
    const condition = exists ? "exist" : "not.exist";
    this.#getDropdownItem(itemName).should(condition);
    return this;
  }

  clickDropdownItem(itemName: string) {
    this.#getDropdownItem(itemName).click();
    return this;
  }
}
